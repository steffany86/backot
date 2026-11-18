package com.example.TigoStarSystem.configuraciones.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Configuraciones generales del sistema (clave/valor) en BDControlOrdenes (172.16.0.13).
 * La tabla se crea sola la primera vez que se usa (compatible con SQL Server 2008).
 */
@Repository
public class ConfiguracionRepository {
    private final JdbcTemplate jdbc;
    private final AtomicBoolean tablaLista = new AtomicBoolean(false);

    public ConfiguracionRepository(@Qualifier("supervisionV2JdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private void asegurarTabla() {
        if (tablaLista.get()) {
            return;
        }
        jdbc.execute(
                "IF OBJECT_ID('dbo.tbl_ConfiguracionSistema', 'U') IS NULL "
                        + "CREATE TABLE dbo.tbl_ConfiguracionSistema ("
                        + " Clave NVARCHAR(100) NOT NULL PRIMARY KEY,"
                        + " Valor NVARCHAR(500) NULL,"
                        + " FechaModificacion DATETIME NOT NULL DEFAULT GETDATE(),"
                        + " UsuarioModificacion NVARCHAR(100) NULL)"
        );
        tablaLista.set(true);
    }

    public Map<String, String> listarValores() {
        asegurarTabla();
        Map<String, String> out = new HashMap<String, String>();
        for (Map<String, Object> row : jdbc.queryForList("SELECT Clave, Valor FROM dbo.tbl_ConfiguracionSistema")) {
            Object valor = row.get("Valor");
            out.put(String.valueOf(row.get("Clave")), valor == null ? null : String.valueOf(valor));
        }
        return out;
    }

    public void guardar(String clave, String valor, String usuario) {
        asegurarTabla();
        int updated = jdbc.update(
                "UPDATE dbo.tbl_ConfiguracionSistema SET Valor = ?, FechaModificacion = GETDATE(), UsuarioModificacion = ? WHERE Clave = ?",
                valor, usuario, clave);
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO dbo.tbl_ConfiguracionSistema (Clave, Valor, UsuarioModificacion) VALUES (?, ?, ?)",
                    clave, valor, usuario);
        }
    }
}
