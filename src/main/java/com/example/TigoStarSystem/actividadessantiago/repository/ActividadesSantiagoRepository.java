package com.example.TigoStarSystem.actividadessantiago.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registro de actividades de Santiago en BDControlOrdenes (172.16.0.13).
 * Las tablas se crean solas la primera vez que se usan (compatible con SQL Server 2008).
 */
@Repository
public class ActividadesSantiagoRepository {
    private final JdbcTemplate jdbc;
    private final AtomicBoolean tablasListas = new AtomicBoolean(false);

    public ActividadesSantiagoRepository(@Qualifier("supervisionV2JdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private void asegurarTablas() {
        if (tablasListas.get()) {
            return;
        }
        jdbc.execute(
                "IF OBJECT_ID('dbo.tbl_SupSRegistro', 'U') IS NULL "
                        + "CREATE TABLE dbo.tbl_SupSRegistro ("
                        + " Id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,"
                        + " Fecha DATE NOT NULL,"
                        + " ActividadId VARCHAR(20) NOT NULL,"
                        + " Nivel TINYINT NULL,"
                        + " Minutos INT NULL,"
                        + " Observacion NVARCHAR(MAX) NULL,"
                        + " UsuarioId INT NULL,"
                        + " FechaMod DATETIME NOT NULL DEFAULT GETDATE())"
        );
        jdbc.execute(
                "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_tbl_SupSRegistro_Fecha_Act') "
                        + "CREATE UNIQUE INDEX UX_tbl_SupSRegistro_Fecha_Act ON dbo.tbl_SupSRegistro(Fecha, ActividadId)"
        );
        jdbc.execute(
                "IF OBJECT_ID('dbo.tbl_SupSDiaNota', 'U') IS NULL "
                        + "CREATE TABLE dbo.tbl_SupSDiaNota ("
                        + " Fecha DATE NOT NULL PRIMARY KEY,"
                        + " Animo NVARCHAR(50) NULL,"
                        + " Observacion NVARCHAR(MAX) NULL,"
                        + " UsuarioId INT NULL,"
                        + " FechaMod DATETIME NOT NULL DEFAULT GETDATE())"
        );
        tablasListas.set(true);
    }

    public List<Map<String, Object>> listarRegistros() {
        asegurarTablas();
        return jdbc.queryForList(
                "SELECT CONVERT(char(10), Fecha, 23) AS fecha, ActividadId AS id, Nivel AS nivel, Minutos AS minutos, Observacion AS observacion "
                        + "FROM dbo.tbl_SupSRegistro ORDER BY Fecha, ActividadId");
    }

    public List<Map<String, Object>> listarDias() {
        asegurarTablas();
        return jdbc.queryForList(
                "SELECT CONVERT(char(10), Fecha, 23) AS fecha, Animo AS animo, Observacion AS observacion "
                        + "FROM dbo.tbl_SupSDiaNota ORDER BY Fecha");
    }

    public void guardarRegistro(String fecha, String actividadId, Integer nivel, Integer minutos, String observacion, Integer usuarioId) {
        asegurarTablas();
        int n = jdbc.update(
                "UPDATE dbo.tbl_SupSRegistro SET Nivel = ?, Minutos = ?, Observacion = ?, UsuarioId = ?, FechaMod = GETDATE() "
                        + "WHERE Fecha = CONVERT(date, ?, 23) AND ActividadId = ?",
                new Object[]{nivel, minutos, observacion, usuarioId, fecha, actividadId},
                new int[]{Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR});
        if (n == 0) {
            jdbc.update(
                    "INSERT INTO dbo.tbl_SupSRegistro (Fecha, ActividadId, Nivel, Minutos, Observacion, UsuarioId, FechaMod) "
                            + "VALUES (CONVERT(date, ?, 23), ?, ?, ?, ?, ?, GETDATE())",
                    new Object[]{fecha, actividadId, nivel, minutos, observacion, usuarioId},
                    new int[]{Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.INTEGER});
        }
    }

    public void guardarDia(String fecha, String animo, String observacion, Integer usuarioId) {
        asegurarTablas();
        int n = jdbc.update(
                "UPDATE dbo.tbl_SupSDiaNota SET Animo = ?, Observacion = ?, UsuarioId = ?, FechaMod = GETDATE() "
                        + "WHERE Fecha = CONVERT(date, ?, 23)",
                new Object[]{animo, observacion, usuarioId, fecha},
                new int[]{Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR});
        if (n == 0) {
            jdbc.update(
                    "INSERT INTO dbo.tbl_SupSDiaNota (Fecha, Animo, Observacion, UsuarioId, FechaMod) "
                            + "VALUES (CONVERT(date, ?, 23), ?, ?, ?, GETDATE())",
                    new Object[]{fecha, animo, observacion, usuarioId},
                    new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER});
        }
    }
}
