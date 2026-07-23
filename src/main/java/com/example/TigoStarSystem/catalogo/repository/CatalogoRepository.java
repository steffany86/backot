package com.example.TigoStarSystem.catalogo.repository;

import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Repository
public class CatalogoRepository {
    private static final Logger logger = LoggerFactory.getLogger(CatalogoRepository.class);
    private final JdbcTemplate jdbcTemplate;
    private final CatalogoDbSupport dbSupport;

    public CatalogoRepository(
            JdbcTemplate jdbcTemplate,
            SucursalRepository sucursalRepository,
            @Value("${spring.datasource.driver-class-name}") String dbDriver,
            @Value("${spring.datasource.url}") String mainDatasourceUrl,
            @Value("${app.sucre.datasource.url:}") String sucreDatasourceUrl,
            @Value("${auth.login.sucre.database:SucrePrueba}") String sucreDatabase,
            @Value("${app.sucre.datasource.username:${spring.datasource.username}}") String sucreUsername,
            @Value("${app.sucre.datasource.password:${spring.datasource.password}}") String sucrePassword,
            @Value("${app.datasource.params:encrypt=false;trustServerCertificate=true}") String dbParams) {
        this.jdbcTemplate = jdbcTemplate;
        this.dbSupport = new CatalogoDbSupport(
                sucursalRepository,
                dbDriver,
                mainDatasourceUrl,
                sucreDatasourceUrl,
                sucreDatabase,
                sucreUsername,
                sucrePassword,
                dbParams
        );
    }

    public List<Map<String, Object>> listarTecnicos() {
        return listarTecnicos(null);
    }

    public List<Map<String, Object>> listarTecnicos(Integer idSucursal) {
        return template(idSucursal).queryForList("EXEC spx_ObtenerTecnicosEnRuta");
    }

    public List<Map<String, Object>> listarRutasPorTecnico(Integer idTecnico) {
        return listarRutasPorTecnico(idTecnico, null);
    }

    public List<Map<String, Object>> listarRutasPorTecnico(Integer idTecnico, Integer idSucursal) {
        JdbcTemplate target = template(idSucursal);
        Integer tecnico = idTecnico != null && idTecnico > 0 ? idTecnico : null;

        if (tecnico != null) {
            // Flujo requerido:
            // 1) obtener id_vendedor en tbl_usuariotecnico por id_usuario
            // 2) buscar rutas activas en tbl_ruta por id_vendedor (e_eliminado = 0)
            List<Integer> vendedores = listarVendedoresPorUsuario(target, tecnico);
            if (!vendedores.isEmpty()) {
                List<Map<String, Object>> rutasPorVendedor = new ArrayList<>();
                for (Integer idVendedor : vendedores) {
                    rutasPorVendedor.addAll(listarRutasActivasPorVendedor(target, idVendedor));
                }
                List<Map<String, Object>> filtradas = filtrarRutasNoEliminadas(rutasPorVendedor);
                if (!filtradas.isEmpty()) {
                    return filtradas;
                }
            }

            // Compatibilidad: en algunas instalaciones el id recibido puede corresponder directamente al vendedor.
            List<Map<String, Object>> vendedorRows = listarRutasActivasPorVendedor(target, tecnico);
            if (!vendedorRows.isEmpty()) {
                return vendedorRows;
            }
            try {
                return filtrarRutasNoEliminadas(target.queryForList("EXEC spx_ObtenerRutaXIdTecnico ?", tecnico));
            } catch (DataAccessException ex) {
                return new ArrayList<>();
            }
        }

        List<Map<String, Object>> activeRows = listarRutasActivas(target);
        if (!activeRows.isEmpty()) {
            return activeRows;
        }

        try {
            return filtrarRutasNoEliminadas(target.queryForList("EXEC spx_ObtenerRutaXIdTecnico ?", (Object) null));
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> listarRutasActivasPorUsuario(JdbcTemplate target, Integer idUsuario) {
        String[] statements = new String[] {
                "SELECT DISTINCT " +
                        "r.id_ruta AS idRuta, " +
                        "ISNULL(r.Nombre, CONVERT(NVARCHAR(200), r.id_ruta)) AS ruta, " +
                        "r.id_vendedor AS id_vendedor, " +
                        "ISNULL(r.e_eliminado, 0) AS e_eliminado " +
                        "FROM dbo.tbl_ruta r " +
                        "INNER JOIN dbo.tbl_usuariotecnico ut ON ut.id_vendedor = r.id_vendedor " +
                        "WHERE (ut.id_usuario = ? OR ut.idusuario = ?) " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ISNULL(r.e_eliminado, 0) = 0 " +
                        "ORDER BY 2",
                "SELECT DISTINCT " +
                        "r.id_ruta AS idRuta, " +
                        "ISNULL(r.Nombre, CONVERT(NVARCHAR(200), r.id_ruta)) AS ruta, " +
                        "r.id_tecnico AS id_tecnico, " +
                        "ISNULL(r.e_eliminado, 0) AS e_eliminado " +
                        "FROM dbo.tbl_ruta r " +
                        "INNER JOIN dbo.tbl_usuariotecnico ut ON ut.id_vendedor = r.id_tecnico " +
                        "WHERE (ut.id_usuario = ? OR ut.idusuario = ?) " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ISNULL(r.e_eliminado, 0) = 0 " +
                        "ORDER BY 2",
                "SELECT DISTINCT " +
                        "r.Id_Ruta AS idRuta, " +
                        "ISNULL(r.Nombre, CONVERT(NVARCHAR(200), r.Id_Ruta)) AS ruta, " +
                        "r.Id_Vendedor AS id_vendedor, " +
                        "ISNULL(r.E_Eliminado, 0) AS e_eliminado " +
                        "FROM dbo.tbl_Ruta r " +
                        "INNER JOIN dbo.tbl_UsuarioTecnico ut ON ut.Id_Vendedor = r.Id_Vendedor " +
                        "WHERE (ut.Id_Usuario = ? OR ut.IdUsuario = ?) " +
                        "AND ISNULL(ut.E_Eliminado, 0) = 0 " +
                        "AND ISNULL(r.E_Eliminado, 0) = 0 " +
                        "ORDER BY 2",
                "SELECT DISTINCT " +
                        "r.Id_Ruta AS idRuta, " +
                        "ISNULL(r.Nombre, CONVERT(NVARCHAR(200), r.Id_Ruta)) AS ruta, " +
                        "r.Id_Tecnico AS id_tecnico, " +
                        "ISNULL(r.E_Eliminado, 0) AS e_eliminado " +
                        "FROM dbo.tbl_Ruta r " +
                        "INNER JOIN dbo.tbl_UsuarioTecnico ut ON ut.Id_Vendedor = r.Id_Tecnico " +
                        "WHERE (ut.Id_Usuario = ? OR ut.IdUsuario = ?) " +
                        "AND ISNULL(ut.E_Eliminado, 0) = 0 " +
                        "AND ISNULL(r.E_Eliminado, 0) = 0 " +
                        "ORDER BY 2"
        };

        for (String sql : statements) {
            try {
                List<Map<String, Object>> rows = target.queryForList(sql, idUsuario, idUsuario);
                List<Map<String, Object>> filtered = filtrarRutasNoEliminadas(rows);
                if (!filtered.isEmpty()) {
                    return filtered;
                }
            } catch (DataAccessException ex) {
                // probar siguiente variante
            }
        }
        return new ArrayList<>();
    }

    private List<Integer> listarVendedoresPorUsuario(JdbcTemplate target, Integer idUsuario) {
        String[] statements = new String[] {
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.id_usuario = ? " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.idusuario = ? " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.id_tecnico = ? " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.idtecnico = ? " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.id_usuario = ? " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ut.idvendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.idusuario = ? " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ut.idvendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.id_usuario = ? " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.idusuario = ? " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.id_tecnico = ? " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.idtecnico = ? " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.id_usuario = ? " +
                        "AND ut.idvendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.idusuario = ? " +
                        "AND ut.idvendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.Id_Vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.Id_Usuario = ? " +
                        "AND ISNULL(ut.E_Eliminado, 0) = 0 " +
                        "AND ut.Id_Vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.Id_Vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.IdUsuario = ? " +
                        "AND ISNULL(ut.E_Eliminado, 0) = 0 " +
                        "AND ut.Id_Vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.Id_Vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.Id_Tecnico = ? " +
                        "AND ISNULL(ut.E_Eliminado, 0) = 0 " +
                        "AND ut.Id_Vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.Id_Vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.IdTecnico = ? " +
                        "AND ISNULL(ut.E_Eliminado, 0) = 0 " +
                        "AND ut.Id_Vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.Id_Vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.Id_Usuario = ? " +
                        "AND ut.Id_Vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.Id_Vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.IdUsuario = ? " +
                        "AND ut.Id_Vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.Id_Vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.Id_Tecnico = ? " +
                        "AND ut.Id_Vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.Id_Vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.IdTecnico = ? " +
                        "AND ut.Id_Vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.id_usuario = ? " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.idusuario = ? " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.id_tecnico = ? " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.idtecnico = ? " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.id_usuario = ? " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ut.idvendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.idusuario = ? " +
                        "AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "AND ut.idvendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.id_usuario = ? " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.idusuario = ? " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.id_tecnico = ? " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.idtecnico = ? " +
                        "AND ut.id_vendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.id_usuario = ? " +
                        "AND ut.idvendedor IS NOT NULL " +
                        "ORDER BY 1",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.idusuario = ? " +
                        "AND ut.idvendedor IS NOT NULL " +
                        "ORDER BY 1"
        };

        for (String sql : statements) {
            try {
                List<Map<String, Object>> rows = target.queryForList(sql, idUsuario);
                List<Integer> vendedores = extraerIdsVendedor(rows);
                if (!vendedores.isEmpty()) {
                    return vendedores;
                }
            } catch (DataAccessException ex) {
                // probar siguiente variante de esquema
            }
        }
        return new ArrayList<>();
    }

    private List<Integer> extraerIdsVendedor(List<Map<String, Object>> rows) {
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Object raw = row.get("id_vendedor");
            if (raw == null) raw = row.get("Id_Vendedor");
            if (raw == null) raw = row.get("idvendedor");
            Integer id = tryParseInteger(raw);
            if (id != null && id > 0) {
                ids.add(id);
            }
        }
        return new ArrayList<>(ids);
    }

    private Integer tryParseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return null;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<Map<String, Object>> listarRutasActivasPorVendedor(JdbcTemplate target, Integer idVendedorOTecnico) {
        String[] statements = new String[] {
                "SELECT DISTINCT " +
                        "r.id_ruta AS idRuta, " +
                        "ISNULL(r.Nombre, CONVERT(NVARCHAR(200), r.id_ruta)) AS ruta, " +
                        "r.id_vendedor AS id_vendedor, " +
                        "ISNULL(r.e_eliminado, 0) AS e_eliminado " +
                        "FROM dbo.tbl_ruta r " +
                        "WHERE r.id_vendedor = ? " +
                        "AND ISNULL(r.e_eliminado, 0) = 0 " +
                        "ORDER BY 2",
                "SELECT DISTINCT " +
                        "r.id_ruta AS idRuta, " +
                        "ISNULL(r.Nombre, CONVERT(NVARCHAR(200), r.id_ruta)) AS ruta, " +
                        "r.id_tecnico AS id_tecnico, " +
                        "ISNULL(r.e_eliminado, 0) AS e_eliminado " +
                        "FROM dbo.tbl_ruta r " +
                        "WHERE r.id_tecnico = ? " +
                        "AND ISNULL(r.e_eliminado, 0) = 0 " +
                        "ORDER BY 2",
                "SELECT DISTINCT " +
                        "r.Id_Ruta AS idRuta, " +
                        "ISNULL(r.Nombre, CONVERT(NVARCHAR(200), r.Id_Ruta)) AS ruta, " +
                        "r.Id_Vendedor AS id_vendedor, " +
                        "ISNULL(r.E_Eliminado, 0) AS e_eliminado " +
                        "FROM dbo.tbl_Ruta r " +
                        "WHERE r.Id_Vendedor = ? " +
                        "AND ISNULL(r.E_Eliminado, 0) = 0 " +
                        "ORDER BY 2",
                "SELECT DISTINCT " +
                        "r.Id_Ruta AS idRuta, " +
                        "ISNULL(r.Nombre, CONVERT(NVARCHAR(200), r.Id_Ruta)) AS ruta, " +
                        "r.Id_Tecnico AS id_tecnico, " +
                        "ISNULL(r.E_Eliminado, 0) AS e_eliminado " +
                        "FROM dbo.tbl_Ruta r " +
                        "WHERE r.Id_Tecnico = ? " +
                        "AND ISNULL(r.E_Eliminado, 0) = 0 " +
                        "ORDER BY 2"
        };

        for (String sql : statements) {
            try {
                List<Map<String, Object>> rows = target.queryForList(sql, idVendedorOTecnico);
                List<Map<String, Object>> filtered = filtrarRutasNoEliminadas(rows);
                if (!filtered.isEmpty()) {
                    return filtered;
                }
            } catch (DataAccessException ex) {
                // probar siguiente variante
            }
        }
        return new ArrayList<>();
    }

    private List<Map<String, Object>> listarRutasActivas(JdbcTemplate target) {
        String[] statements = new String[] {
                "SELECT DISTINCT " +
                        "CAST(r.id_ruta AS INT) AS idRuta, " +
                        "ISNULL(r.Nombre, CAST(r.id_ruta AS NVARCHAR(200))) AS ruta, " +
                        "CAST(r.id_vendedor AS INT) AS id_vendedor, " +
                        "ISNULL(r.e_eliminado, 0) AS e_eliminado " +
                        "FROM dbo.tbl_ruta r " +
                        "WHERE ISNULL(r.e_eliminado, 0) = 0 " +
                        "ORDER BY 2",
                "SELECT DISTINCT " +
                        "CAST(r.Id_Ruta AS INT) AS idRuta, " +
                        "ISNULL(r.Nombre, CAST(r.Id_Ruta AS NVARCHAR(200))) AS ruta, " +
                        "CAST(r.Id_Vendedor AS INT) AS id_vendedor, " +
                        "ISNULL(r.E_Eliminado, 0) AS e_eliminado " +
                        "FROM dbo.tbl_Ruta r " +
                        "WHERE ISNULL(r.E_Eliminado, 0) = 0 " +
                        "ORDER BY 2"
        };

        for (String sql : statements) {
            try {
                List<Map<String, Object>> rows = target.queryForList(sql);
                List<Map<String, Object>> filtered = filtrarRutasNoEliminadas(rows);
                if (!filtered.isEmpty()) {
                    return filtered;
                }
            } catch (DataAccessException ex) {
                // probar siguiente variante
            }
        }
        return new ArrayList<>();
    }

    private List<Map<String, Object>> filtrarRutasNoEliminadas(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Object eliminado = row.get("e_eliminado");
            if (eliminado == null) eliminado = row.get("E_Eliminado");
            if (eliminado == null) eliminado = row.get("eeliminado");
            if (esEliminado(eliminado)) {
                continue;
            }
            out.add(row);
        }
        return out;
    }

    private boolean esEliminado(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        String normalized = String.valueOf(value).trim().toLowerCase();
        if (normalized.isEmpty()) return false;
        return normalized.equals("1")
                || normalized.equals("true")
                || normalized.equals("si")
                || normalized.equals("yes");
    }

    public List<Map<String, Object>> listarTiposServicio() {
        return listarTiposServicio(null);
    }

    public List<Map<String, Object>> listarTiposServicio(Integer idSucursal) {
        JdbcTemplate target = template(idSucursal);
        try {
            return target.queryForList(
                    "SELECT " +
                            "Id_TipoServicio AS idTipoServicio, " +
                            "Nombre AS tipoServicio, " +
                            "Prefijo AS prefijo, " +
                            "TipoArchivo_PROTW AS tipoArchivoPROTW, " +
                            "TipoArchivo_PROTW AS TipoArchivo_PROTW, " +
                            "ISNULL(CAST(Nomencladores AS BIT), 0) AS nomencladores, " +
                            "ISNULL(CAST(habilitarTieneDetalle AS BIT), 0) AS habilitarTieneDetalle, " +
                            "ISNULL(CAST(checkSeUsoMaterial AS BIT), 0) AS checkSeUsoMaterial " +
                            "FROM dbo.tbl_tiposervicio " +
                            "WHERE ISNULL(E_Eliminado, 0) = 0 " +
                            "ORDER BY Nombre"
            );
        } catch (DataAccessException ex) {
            return target.queryForList("EXEC spx_ObtenerTipoServicio");
        }
    }

    public List<Map<String, Object>> listarNomencladores() {
        return listarNomencladores(null);
    }

    public List<Map<String, Object>> listarNomencladores(Integer idSucursal) {
        JdbcTemplate target = template(idSucursal);
        try {
            return target.queryForList("EXEC spx_ObtenerNomencladores");
        } catch (DataAccessException ex) {
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarEstados() {
        return listarEstados(null);
    }

    public List<Map<String, Object>> listarEstados(Integer idSucursal) {
        return template(idSucursal).queryForList("EXEC sp_ObtenerEstado");
    }

    public List<Map<String, Object>> listarRamales(Integer idSucursal) {
        return template(idSucursal).queryForList("EXEC spx_ObtenerRamal");
    }

    public List<Map<String, Object>> listarTiposTecnologia(Integer idRuta, Integer idSucursal) {
        return template(idSucursal).queryForList("EXEC spx_ObtenerTipoTecnologia ?", idRuta);
    }

    public List<Map<String, Object>> listarTipoMaterial(Integer idTipoServicio) {
        return listarTipoMaterial(idTipoServicio, null);
    }

    public List<Map<String, Object>> listarTipoMaterial(Integer idTipoServicio, Integer idSucursal) {
        return template(idSucursal).queryForList("EXEC sp_ObtenerTipoMaterial ?", idTipoServicio);
    }

    public List<Map<String, Object>> listarProductos() {
        return listarProductos(null);
    }

    public List<Map<String, Object>> listarProductos(Integer idSucursal) {
        return template(idSucursal).queryForList("EXEC TraerTodosLosProductos");
    }

    public List<Map<String, Object>> listarProductosSinFungibleWeb() {
        return listarProductosSinFungibleWeb(null);
    }

    public List<Map<String, Object>> listarProductosSinFungibleWeb(Integer idSucursal) {
        return template(idSucursal).queryForList("EXEC TraerTodosLosProductos_SinFungibleWeb");
    }

    public List<Map<String, Object>> listarReglasAutoMaterial(Integer idSucursal) {
        JdbcTemplate target = template(idSucursal);
        try {
            List<Map<String, Object>> reglas = target.queryForList(
                    "SELECT " +
                            "r.TipoRegla AS tipoRegla, " +
                            "r.TipoRegla AS TipoRegla, " +
                            "r.Id_TipoServicio AS idTipoServicio, " +
                            "r.Id_TipoServicio AS Id_TipoServicio, " +
                            "r.TipoTecnologia AS tipoTecnologia, " +
                            "r.TipoTecnologia AS TipoTecnologia, " +
                            "r.SufijoNomenclador AS sufijoNomenclador, " +
                            "r.SufijoNomenclador AS SufijoNomenclador, " +
                            "r.Id_Producto AS idProducto, " +
                            "r.Id_Producto AS Id_Producto, " +
                            "ISNULL(p.nombre, CAST(r.Id_Producto AS VARCHAR(20))) AS producto, " +
                            "ISNULL(p.nombre, CAST(r.Id_Producto AS VARCHAR(20))) AS Producto, " +
                            "r.Cantidad AS cantidad, " +
                            "r.Cantidad AS Cantidad, " +
                            "r.Id_TipoMaterial AS idTipoMaterial, " +
                            "r.Id_TipoMaterial AS Id_TipoMaterial, " +
                            "r.Activo AS activo, " +
                            "r.Observacion AS observacion " +
                            "FROM dbo.tbl_ReglaAutoMaterial r " +
                            "LEFT JOIN dbo.tbl_producto p ON p.Id_Producto = r.Id_Producto " +
                            "WHERE ISNULL(r.Activo, 1) = 1 " +
                            "AND ISNULL(r.E_Eliminado, 0) = 0 " +
                            "ORDER BY CASE WHEN UPPER(LTRIM(RTRIM(r.TipoRegla))) = 'FIJO' THEN 0 ELSE 1 END, r.Id_ReglaAutoMaterial"
            );
            if (reglas != null && !reglas.isEmpty()) {
                return reglas;
            }
        } catch (DataAccessException ex) {
            logger.debug("tbl_ReglaAutoMaterial no disponible, usando TraerTodosLosProductos_SinFungibleWeb: {}", ex.getMessage());
        }
        return Collections.emptyList();
    }

    public List<Map<String, Object>> listarProductosPorRuta(Integer idRuta) {
        return listarProductosPorRuta(idRuta, null);
    }

    public List<Map<String, Object>> listarProductosPorRuta(Integer idRuta, Integer idSucursal) {
        JdbcTemplate target = template(idSucursal);
        List<Map<String, Object>> rows = target.queryForList("EXEC TraerTodosLosProductos_x_IdRutaWeb ?", idRuta);
        if (rows == null || rows.isEmpty()) {
            Integer rutaActiva = resolverRutaActivaConSaldo(target, idRuta);
            if (rutaActiva != null && !rutaActiva.equals(idRuta)) {
                List<Map<String, Object>> rutaActivaRows = target.queryForList("EXEC TraerTodosLosProductos_x_IdRutaWeb ?", rutaActiva);
                if (rutaActivaRows != null && !rutaActivaRows.isEmpty()) {
                    logger.warn(
                            "TraerTodosLosProductos_x_IdRutaWeb vacio para rutaId={}. Usando ruta activa con saldo={}.",
                            idRuta,
                            rutaActiva
                    );
                    return rutaActivaRows;
                }
            }
        }
        if ((rows == null || rows.isEmpty()) && idSucursal != null) {
            try {
                List<Map<String, Object>> fallback = jdbcTemplate.queryForList("EXEC TraerTodosLosProductos_x_IdRutaWeb ?", idRuta);
                if (fallback != null && !fallback.isEmpty()) {
                    logger.warn(
                            "TraerTodosLosProductos_x_IdRutaWeb vacio en sucursal id={} para rutaId={}. Usando fallback DB principal.",
                            idSucursal,
                            idRuta
                    );
                    return fallback;
                }
            } catch (DataAccessException ex) {
                logger.warn(
                        "Fallback DB principal fallo para TraerTodosLosProductos_x_IdRutaWeb idSucursal={} rutaId={}: {}",
                        idSucursal,
                        idRuta,
                        ex.getMessage()
                );
            }
        }
        return rows;
    }

    public List<Map<String, Object>> listarProductosCargoUsuarioWeb() {
        return listarProductosCargoUsuarioWeb(null);
    }

    public List<Map<String, Object>> listarProductosCargoUsuarioWeb(Integer idSucursal) {
        JdbcTemplate target = template(idSucursal);
        try {
            return target.queryForList("EXEC TraerTodosLosProductos_SinFungibleWeb");
        } catch (DataAccessException ex) {
            // Compatibilidad: si el SP nuevo no existe en alguna sucursal, usar el anterior.
            return target.queryForList("EXEC spx_ObtenerProductosPCargoUsuario");
        }
    }

    public List<Map<String, Object>> buscarSerialCargoUsuario(String serial, String chipId, Integer tipoCodigo) {
        return buscarSerialCargoUsuario(serial, chipId, tipoCodigo, null);
    }

    public List<Map<String, Object>> buscarSerialCargoUsuario(String serial, String chipId, Integer tipoCodigo, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC spx_BuscarSerialCargoUsuario ?, ?, ?",
                serial,
                chipId,
                tipoCodigo
        );
    }

    public List<Map<String, Object>> traerChipIdPorSerie(String serie) {
        return traerChipIdPorSerie(serie, null);
    }

    public List<Map<String, Object>> traerChipIdPorSerie(String serie, Integer idSucursal) {
        return template(idSucursal).queryForList("EXEC spx_TraerChipID2 ?", serie);
    }

    public List<Map<String, Object>> sugerirSeriesPorPrefijo(String prefijo, Integer limite, Integer idProducto, Integer idSucursal) {
        int top = (limite == null || limite <= 0) ? 10 : Math.min(limite, 50);
        String text = prefijo == null ? "" : prefijo.trim();
        if (text.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        Integer productoFiltro = idProducto != null && idProducto > 0 ? idProducto : null;
        String prefixValue = text + "%";
        String containsValue = "%" + text + "%";
        return template(idSucursal).queryForList(
                "SELECT TOP " + top + " " +
                        "LTRIM(RTRIM(ISNULL(p.Serial, ''))) AS serial, " +
                        "LTRIM(RTRIM(ISNULL(p.ChipID, ''))) AS chipId, " +
                        "p.Id_Productos AS idProductos, " +
                        "p.Id_Producto AS idProducto, " +
                        "prod.Nombre AS producto, " +
                        "p.Id_EstadoProducto AS idEstadoProducto, " +
                        "ep.Nombre AS estadoProducto, " +
                        "p.Id_Ruta AS idRuta, " +
                        "r.Nombre AS ruta, " +
                        "CASE " +
                        "  WHEN UPPER(LTRIM(RTRIM(ISNULL(p.Serial, '')))) LIKE UPPER(?) THEN 'serial' " +
                        "  ELSE 'chipId' " +
                        "END AS coincidencia " +
                        "FROM dbo.tbl_productos p " +
                        "LEFT JOIN dbo.tbl_producto prod ON prod.Id_Producto = p.Id_Producto " +
                        "LEFT JOIN dbo.tbl_estadoproducto ep ON ep.Id_EstadoProducto = p.Id_EstadoProducto " +
                        "LEFT JOIN dbo.tbl_ruta r ON r.Id_Ruta = p.Id_Ruta " +
                        "WHERE ISNULL(p.e_eliminado, 0) = 0 " +
                        "AND p.Id_EstadoProducto = 2 " +
                        "AND (? IS NULL OR p.Id_Producto = ?) " +
                        "AND ( " +
                        "  (LTRIM(RTRIM(ISNULL(p.Serial, ''))) <> '' AND UPPER(LTRIM(RTRIM(ISNULL(p.Serial, '')))) LIKE UPPER(?)) " +
                        "  OR (LTRIM(RTRIM(ISNULL(p.ChipID, ''))) <> '' AND UPPER(LTRIM(RTRIM(ISNULL(p.ChipID, '')))) LIKE UPPER(?)) " +
                        ") " +
                        "ORDER BY " +
                        "  CASE WHEN UPPER(LTRIM(RTRIM(ISNULL(p.Serial, '')))) LIKE UPPER(?) THEN 0 ELSE 1 END, " +
                        "  p.Id_Productos DESC",
                prefixValue,
                productoFiltro,
                productoFiltro,
                containsValue,
                containsValue,
                prefixValue
        );
    }

    public List<Map<String, Object>> sugerirSeriesSaldoInstalado(
            String prefijo,
            Integer limite,
            Integer idRuta,
            Integer idProducto,
            Integer idSucursal) {
        int top = (limite == null || limite <= 0) ? 10 : Math.min(limite, 50);
        String text = prefijo == null ? "" : prefijo.trim();
        JdbcTemplate target = template(idSucursal);
        Integer rutaConsulta = resolverRutaActivaConSaldo(target, idRuta);
        if (rutaConsulta == null) {
            rutaConsulta = idRuta;
        }

        List<Map<String, Object>> rows = target.queryForList(
                "EXEC dbo.spx_ObtenerSaldoTarjetasSugeridoswb ?, ?",
                rutaConsulta,
                idProducto
        );

        String normalizedText = normalizeText(text);
        List<Map<String, Object>> prefixMatches = new ArrayList<>();
        List<Map<String, Object>> containsMatches = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> normalized = normalizarSugerenciaSaldo(row);
            String serial = normalizeText(asString(normalized.get("serial")));
            String chipId = normalizeText(asString(normalized.get("chipId")));
            if (normalizedText.isEmpty()) {
                prefixMatches.add(normalized);
                continue;
            }
            boolean serialPrefix = !serial.isEmpty() && serial.startsWith(normalizedText);
            boolean chipPrefix = !chipId.isEmpty() && chipId.startsWith(normalizedText);
            boolean serialContains = !serial.isEmpty() && serial.contains(normalizedText);
            boolean chipContains = !chipId.isEmpty() && chipId.contains(normalizedText);

            if (serialPrefix || chipPrefix) {
                normalized.put("coincidencia", serialPrefix ? "serial" : "chipId");
                prefixMatches.add(normalized);
            } else if (serialContains || chipContains) {
                normalized.put("coincidencia", serialContains ? "serial" : "chipId");
                containsMatches.add(normalized);
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : prefixMatches) {
            if (out.size() >= top) break;
            out.add(row);
        }
        for (Map<String, Object> row : containsMatches) {
            if (out.size() >= top) break;
            out.add(row);
        }
        return out;
    }

    public Map<String, Object> validarSerieChipUnico(String serie, String chipId) {
        return validarSerieChipUnico(serie, chipId, null);
    }

    public Map<String, Object> validarSerieChipUnico(String serie, String chipId, Integer idSucursal) {
        List<Map<String, Object>> rows = template(idSucursal).queryForList(
                "SELECT TOP 1 serial, chipid, id_producto, e_eliminado " +
                        "FROM dbo.tbl_productos " +
                        "WHERE (serial = ? OR chipid = ?) AND e_eliminado = 0",
                serie,
                chipId
        );
        if (rows.isEmpty()) {
            return buildSerieChipResult(false, false, false, serie, chipId);
        }

        Map<String, Object> row = rows.get(0);
        String serieDb = normalizeText(asString(row.get("serial")));
        String chipDb = normalizeText(asString(row.get("chipid")));
        String serieInput = normalizeText(serie);
        String chipInput = normalizeText(chipId);
        boolean serieExiste = !serieDb.isEmpty() && serieDb.equals(serieInput);
        boolean chipExiste = !chipDb.isEmpty() && chipDb.equals(chipInput);
        boolean mismoRegistro = serieExiste && chipExiste;
        return buildSerieChipResult(serieExiste, chipExiste, mismoRegistro, serie, chipId);
    }

    public List<Map<String, Object>> validarSerieSaldo(String serie, Integer idProducto, Integer tipoMaterial, Integer idRuta) {
        return validarSerieSaldo(serie, idProducto, tipoMaterial, idRuta, null);
    }

    public List<Map<String, Object>> validarSerieSaldo(String serie, Integer idProducto, Integer tipoMaterial, Integer idRuta, Integer idSucursal) {
        JdbcTemplate target = template(idSucursal);
        Integer rutaConsulta = resolverRutaActivaConSaldo(target, idRuta);
        if (rutaConsulta == null) {
            rutaConsulta = idRuta;
        }
        final Integer rutaFinal = rutaConsulta;
        return target.execute((ConnectionCallback<List<Map<String, Object>>>) connection -> {
            try (CallableStatement statement = connection.prepareCall("{call spx_TraerDatoSerieChipIdCU_OT(?, ?, ?, ?)}")) {
                statement.setString(1, serie);
                statement.setInt(2, idProducto);
                statement.setInt(3, tipoMaterial);
                statement.setInt(4, rutaFinal);
                return readFirstResultSet(statement);
            }
        });
    }

    private Integer resolverRutaActivaConSaldo(JdbcTemplate target, Integer idRuta) {
        if (target == null || idRuta == null || idRuta <= 0) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = target.queryForList(
                    "SELECT TOP 1 rActiva.Id_Ruta AS idRuta " +
                            "FROM dbo.tbl_Ruta rOriginal " +
                            "INNER JOIN dbo.tbl_Ruta rActiva " +
                            "  ON rActiva.Id_Vendedor = rOriginal.Id_Vendedor " +
                            " AND ISNULL(rActiva.E_Eliminado, 0) = 0 " +
                            "WHERE rOriginal.Id_Ruta = ? " +
                            "  AND EXISTS ( " +
                            "      SELECT 1 FROM dbo.tbl_saldotarjetas s " +
                            "      WHERE s.id_ruta = rActiva.Id_Ruta " +
                            "        AND ISNULL(s.e_eliminado, 0) = 0 " +
                            "        AND s.cantidad > 0 " +
                            "  ) " +
                            "ORDER BY CASE WHEN rActiva.Id_Ruta = ? THEN 0 ELSE 1 END DESC, rActiva.Id_Ruta DESC",
                    idRuta,
                    idRuta
            );
            if (rows == null || rows.isEmpty()) {
                return null;
            }
            Object raw = rows.get(0).get("idRuta");
            if (raw instanceof Number) {
                return ((Number) raw).intValue();
            }
            return raw == null ? null : Integer.parseInt(String.valueOf(raw).trim());
        } catch (Exception ex) {
            logger.warn("No se pudo resolver ruta activa con saldo para rutaId={}: {}", idRuta, ex.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> traerDatoSerieChipIdCU(String serie) {
        return traerDatoSerieChipIdCU(serie, null);
    }

    public List<Map<String, Object>> traerDatoSerieChipIdCU(String serie, Integer idSucursal) {
        return template(idSucursal).queryForList("EXEC spx_TraerDatoSerieChipIdCU ?", serie);
    }

    public List<Map<String, Object>> traerDatoSerieChipIdCUCUNR2(String serie, String chipId) {
        return traerDatoSerieChipIdCUCUNR2(serie, chipId, null);
    }

    public List<Map<String, Object>> traerDatoSerieChipIdCUCUNR2(String serie, String chipId, Integer idSucursal) {
        return template(idSucursal).queryForList("EXEC spx_TraerDatoSerieChipIdCU_CUNR2 ?, ?", serie, chipId);
    }

    public List<Map<String, Object>> listarProductosMascara() {
        return listarProductosMascara(null);
    }

    public List<Map<String, Object>> listarProductosMascara(Integer idSucursal) {
        return template(idSucursal).queryForList("EXEC sp_TraerTodosLosProductosMascara");
    }

    public List<Map<String, Object>> listarKitsDecodificadores() {
        return listarKitsDecodificadores(null);
    }

    public List<Map<String, Object>> listarKitsDecodificadores(Integer idSucursal) {
        return template(idSucursal).queryForList("EXEC sp_ObtenerKitDecodificadores");
    }

    private JdbcTemplate template(Integer idSucursal) {
        return dbSupport.resolveTemplate(idSucursal, jdbcTemplate);
    }

    private Map<String, Object> buildSerieChipResult(
            boolean serieExiste,
            boolean chipExiste,
            boolean mismoRegistro,
            String serie,
            String chipId) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("serie", serie);
        result.put("chipId", chipId);
        result.put("serieExiste", serieExiste);
        result.put("chipExiste", chipExiste);
        result.put("mismoRegistro", mismoRegistro);
        result.put("sePuede", mismoRegistro);
        if (mismoRegistro) {
            result.put("observacion", "Serie y ChipID coinciden correctamente.");
        } else if (!serieExiste && chipExiste) {
            result.put("observacion", "La serie no existe, pero el ChipID ya esta registrado.");
        } else if (serieExiste && !chipExiste) {
            result.put("observacion", "La serie ya existe, pero el ChipID no coincide o no existe.");
        } else if (serieExiste) {
            result.put("observacion", "La serie y el ChipID existen, pero no corresponden al mismo registro.");
        } else {
            result.put("observacion", "La serie y el ChipID no existen en saldo.");
        }
        return result;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private Map<String, Object> normalizarSugerenciaSaldo(Map<String, Object> row) {
        Map<String, Object> out = new LinkedHashMap<>();
        String serial = asString(valueIgnoreCase(row, "serial", "Serial", "SERIAL"));
        String chipId = asString(valueIgnoreCase(row, "chipId", "ChipID", "chipid", "CHIPID"));
        out.put("serial", serial == null ? "" : serial.trim());
        out.put("chipId", chipId == null ? "" : chipId.trim());
        out.put("idProductos", valueIgnoreCase(row, "idProductos", "Id_Productos", "id_productos", "ID_PRODUCTOS"));
        out.put("idProducto", valueIgnoreCase(row, "idProducto", "Id_Producto", "id_producto", "ID_PRODUCTO"));
        out.put("producto", valueIgnoreCase(row, "producto", "Producto", "Nombre", "nombre"));
        out.put("idEstadoProducto", valueIgnoreCase(row, "idEstadoProducto", "Id_EstadoProducto", "id_estadoproducto"));
        out.put("estadoProducto", valueIgnoreCase(row, "estadoProducto", "EstadoProducto"));
        out.put("idRuta", valueIgnoreCase(row, "idRuta", "Id_Ruta", "id_ruta"));
        out.put("ruta", valueIgnoreCase(row, "ruta", "Ruta"));
        return out;
    }

    private Object valueIgnoreCase(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) continue;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private List<Map<String, Object>> readFirstResultSet(CallableStatement statement) throws SQLException {
        boolean hasResults = statement.execute();
        while (!hasResults && statement.getUpdateCount() != -1) {
            hasResults = statement.getMoreResults();
        }
        if (!hasResults) {
            return Collections.emptyList();
        }
        try (ResultSet resultSet = statement.getResultSet()) {
            if (resultSet == null) {
                return Collections.emptyList();
            }
            ColumnMapRowMapper mapper = new ColumnMapRowMapper();
            List<Map<String, Object>> rows = new ArrayList<>();
            int rowNum = 0;
            while (resultSet.next()) {
                rows.add(mapper.mapRow(resultSet, rowNum++));
            }
            return rows;
        }
    }
}
