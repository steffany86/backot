package com.example.TigoStarSystem.inventariodpto.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Inventario del Dpto 91 en BDControlOrdenes (172.16.0.13).
 * Las tablas se crean solas la primera vez que se usan (compatible con SQL Server 2008).
 */
@Repository
public class InventarioDptoRepository {
    private final JdbcTemplate jdbc;
    private final AtomicBoolean tablasListas = new AtomicBoolean(false);

    public InventarioDptoRepository(@Qualifier("supervisionV2JdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private void asegurarTablas() {
        if (tablasListas.get()) {
            return;
        }
        jdbc.execute(
                "IF OBJECT_ID('dbo.tbl_InvDptoItem', 'U') IS NULL "
                        + "CREATE TABLE dbo.tbl_InvDptoItem ("
                        + " Item INT NOT NULL PRIMARY KEY,"
                        + " Estado NVARCHAR(30) NOT NULL DEFAULT 'Bueno',"
                        + " Observacion NVARCHAR(1000) NULL,"
                        + " FechaCambio DATE NULL,"
                        + " MotivoCambio NVARCHAR(500) NULL,"
                        + " UsuarioId INT NULL,"
                        + " FechaMod DATETIME NOT NULL DEFAULT GETDATE())"
        );
        jdbc.execute(
                "IF OBJECT_ID('dbo.tbl_InvDptoFoto', 'U') IS NULL "
                        + "CREATE TABLE dbo.tbl_InvDptoFoto ("
                        + " Id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,"
                        + " Item INT NOT NULL,"
                        + " Imagen VARBINARY(MAX) NULL,"
                        + " DriveId VARCHAR(100) NULL,"
                        + " UsuarioId INT NULL,"
                        + " FechaRegistro DATETIME NOT NULL DEFAULT GETDATE())"
        );
        // Tablas creadas antes de usar Google Drive: agrega DriveId y permite Imagen nula (la foto vive en Drive).
        jdbc.execute("IF COL_LENGTH('dbo.tbl_InvDptoFoto', 'DriveId') IS NULL ALTER TABLE dbo.tbl_InvDptoFoto ADD DriveId VARCHAR(100) NULL");
        jdbc.execute("ALTER TABLE dbo.tbl_InvDptoFoto ALTER COLUMN Imagen VARBINARY(MAX) NULL");
        jdbc.execute(
                "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_tbl_InvDptoFoto_Item') "
                        + "CREATE INDEX IX_tbl_InvDptoFoto_Item ON dbo.tbl_InvDptoFoto(Item)"
        );
        tablasListas.set(true);
    }

    public List<Map<String, Object>> listarItems() {
        asegurarTablas();
        return jdbc.queryForList(
                "SELECT Item AS item, Estado AS estado, Observacion AS observacion, "
                        + "CONVERT(char(10), FechaCambio, 23) AS fechaCambio, MotivoCambio AS motivoCambio "
                        + "FROM dbo.tbl_InvDptoItem");
    }

    /** Solo ids (sin los bytes) para no cargar todas las imagenes en el listado. */
    public List<Map<String, Object>> listarFotos() {
        asegurarTablas();
        return jdbc.queryForList("SELECT Id AS id, Item AS item FROM dbo.tbl_InvDptoFoto ORDER BY Id");
    }

    public void guardarItem(Integer item, String estado, String observacion, String fechaCambio, String motivoCambio, Integer usuarioId) {
        asegurarTablas();
        int n = jdbc.update(
                "UPDATE dbo.tbl_InvDptoItem SET Estado = ?, Observacion = ?, FechaCambio = CONVERT(date, ?, 23), "
                        + "MotivoCambio = ?, UsuarioId = ?, FechaMod = GETDATE() WHERE Item = ?",
                new Object[]{estado, observacion, fechaCambio, motivoCambio, usuarioId, item},
                new int[]{Types.NVARCHAR, Types.NVARCHAR, Types.VARCHAR, Types.NVARCHAR, Types.INTEGER, Types.INTEGER});
        if (n == 0) {
            jdbc.update(
                    "INSERT INTO dbo.tbl_InvDptoItem (Item, Estado, Observacion, FechaCambio, MotivoCambio, UsuarioId, FechaMod) "
                            + "VALUES (?, ?, ?, CONVERT(date, ?, 23), ?, ?, GETDATE())",
                    new Object[]{item, estado, observacion, fechaCambio, motivoCambio, usuarioId},
                    new int[]{Types.INTEGER, Types.NVARCHAR, Types.NVARCHAR, Types.VARCHAR, Types.NVARCHAR, Types.INTEGER});
        }
    }

    /** Inserta la foto y devuelve su Id (INSERT + SELECT en la misma conexion). */
    public int insertarFoto(Integer item, byte[] imagen, String driveId, Integer usuarioId) {
        asegurarTablas();
        Integer id = jdbc.execute((java.sql.Connection con) -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO dbo.tbl_InvDptoFoto (Item, Imagen, DriveId, UsuarioId) VALUES (?, ?, ?, ?); "
                            + "SELECT CAST(SCOPE_IDENTITY() AS INT)")) {
                ps.setInt(1, item);
                if (imagen == null) {
                    ps.setNull(2, Types.VARBINARY);
                } else {
                    ps.setBytes(2, imagen);
                }
                if (driveId == null) {
                    ps.setNull(3, Types.VARCHAR);
                } else {
                    ps.setString(3, driveId);
                }
                if (usuarioId == null) {
                    ps.setNull(4, Types.INTEGER);
                } else {
                    ps.setInt(4, usuarioId);
                }
                boolean hayResultado = ps.execute();
                while (!hayResultado && ps.getUpdateCount() != -1) {
                    hayResultado = ps.getMoreResults();
                }
                try (java.sql.ResultSet rs = ps.getResultSet()) {
                    return rs != null && rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
        return id == null ? 0 : id;
    }

    /** Devuelve las claves "Imagen" (bytes, si esta en la base) y "DriveId" (si esta en Drive); null si no existe. */
    public Map<String, Object> obtenerFoto(Integer id) {
        asegurarTablas();
        List<Map<String, Object>> filas = jdbc.queryForList("SELECT Imagen, DriveId FROM dbo.tbl_InvDptoFoto WHERE Id = ?", id);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /** Borra el registro y devuelve su DriveId (o null) para que el servicio borre tambien el archivo de Drive. */
    public String eliminarFoto(Integer id) {
        asegurarTablas();
        String driveId = null;
        try {
            driveId = jdbc.queryForObject("SELECT DriveId FROM dbo.tbl_InvDptoFoto WHERE Id = ?", new Object[]{id}, String.class);
        } catch (EmptyResultDataAccessException ex) {
            // no existe
        }
        jdbc.update("DELETE FROM dbo.tbl_InvDptoFoto WHERE Id = ?", id);
        return driveId;
    }
}
