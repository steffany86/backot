package com.example.TigoStarSystem.supervisor.repository;

import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class ConformacionCuadrillaDbSupport {
    private final SucursalRepository sucursalRepository;
    private final String dbUsername;
    private final String dbPassword;
    private final String dbDriver;
    private final String dbParams;
    private final String uTecnicosHost;
    private final String uTecnicosDatabase;
    private final String centralHost;
    private final String sucreHost;
    private final String sucreDatabase;
    private final String sucreUsername;
    private final String sucrePassword;

    ConformacionCuadrillaDbSupport(
            SucursalRepository sucursalRepository,
            String dbUsername,
            String dbPassword,
            String dbDriver,
            String mainDatasourceUrl,
            String centralDatasourceUrl,
            String sucreDatasourceUrl,
            String sucreDatabase,
            String sucreUsername,
            String sucrePassword,
            String dbParams) {
        this.sucursalRepository = sucursalRepository;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.dbDriver = dbDriver;
        this.uTecnicosHost = parseHostFromJdbcUrl(mainDatasourceUrl);
        this.uTecnicosDatabase = parseDatabaseFromJdbcUrl(mainDatasourceUrl);
        String parsedCentralHost = parseHostFromJdbcUrl(centralDatasourceUrl);
        this.centralHost = isBlank(parsedCentralHost) ? this.uTecnicosHost : parsedCentralHost;
        String parsedSucreHost = parseHostFromJdbcUrl(sucreDatasourceUrl);
        String parsedSucreDatabase = parseDatabaseFromJdbcUrl(sucreDatasourceUrl);
        this.sucreHost = firstNonBlank(parsedSucreHost, this.centralHost);
        this.sucreDatabase = firstNonBlank(parsedSucreDatabase, sucreDatabase);
        this.sucreUsername = firstNonBlank(sucreUsername, this.dbUsername);
        this.sucrePassword = firstNonBlank(sucrePassword, this.dbPassword);
        this.dbParams = dbParams;
    }

    SucursalDbInfo resolverSucursalDbInfo(String sucursalParam) {
        List<Map<String, Object>> sucursales = sucursalRepository.obtenerSucursales();
        if (sucursales == null) {
            sucursales = new ArrayList<>();
        }

        Integer idBuscado = parseInteger(sucursalParam);
        String nombreBuscado = normalizeText(sucursalParam);
        Map<String, Object> sucursalMatch = null;

        for (Map<String, Object> row : sucursales) {
            Integer id = asInteger(firstNonNull(row, "idsucursal", "id_sucursal", "Id_Sucursal"));
            String nombre = asString(firstNonNull(row, "sucursal", "Sucursal"));
            boolean matchId = idBuscado != null && id != null && idBuscado.equals(id);
            boolean matchNombre = !nombreBuscado.isEmpty()
                    && !normalizeText(nombre).isEmpty()
                    && nombreBuscado.equals(normalizeText(nombre));
            if (matchId || matchNombre) {
                sucursalMatch = row;
                break;
            }
        }

        Integer idSucursal = asInteger(
                sucursalMatch == null ? null : firstNonNull(sucursalMatch, "idsucursal", "id_sucursal", "Id_Sucursal")
        );
        String nombreSucursalRaw = asString(
                sucursalMatch == null ? null : firstNonNull(sucursalMatch, "sucursal", "Sucursal")
        );
        String nombreSucursal = normalizeText(nombreSucursalRaw);
        String hostSucursal = asString(
                sucursalMatch == null ? null : firstNonNull(sucursalMatch, "ip", "IP", "ip2", "IP2")
        );
        String baseSucursal = asString(
                sucursalMatch == null ? null : firstNonNull(sucursalMatch, "basededatos", "base_de_datos", "BaseDeDatos")
        );

        if (isSucre(nombreSucursal) || isSucre(nombreBuscado)) {
            return buildDbInfo(
                    sucreHost,
                    sucreDatabase,
                    hostSucursal,
                    baseSucursal,
                    sucreUsername,
                    sucrePassword,
                    dbUsername,
                    dbPassword,
                    firstNonNull(idSucursal, idBuscado),
                    firstNonBlank(nombreSucursalRaw, sucursalParam)
            );
        }

        return buildDbInfo(
                uTecnicosHost,
                uTecnicosDatabase,
                hostSucursal,
                baseSucursal,
                dbUsername,
                dbPassword,
                dbUsername,
                dbPassword,
                firstNonNull(idSucursal, idBuscado),
                firstNonBlank(nombreSucursalRaw, sucursalParam)
        );
    }

    Set<String> construirFiltrosConsulta(String sucursalParam, SucursalDbInfo dbInfo) {
        Set<String> filtros = new LinkedHashSet<>();
        if (isBlank(sucursalParam)) {
            filtros.add(null);
            return filtros;
        }

        addFiltro(filtros, sucursalParam);
        if (dbInfo != null) {
            addFiltro(filtros, dbInfo.getIdSucursal() == null ? null : String.valueOf(dbInfo.getIdSucursal()));
            addFiltro(filtros, dbInfo.getNombreSucursal());
        }
        return filtros;
    }

    JdbcTemplate crearJdbcTemplateSucursal(SucursalDbInfo dbInfo) {
        if (dbInfo == null) {
            return null;
        }
        return crearJdbcTemplateSucursal(
                dbInfo.getHost(),
                dbInfo.getBaseDeDatos(),
                dbInfo.getUsername(),
                dbInfo.getPassword()
        );
    }

    JdbcTemplate crearJdbcTemplateSucursal(String host, String baseDeDatos, String username, String password) {
        String url;
        if (dbDriver != null && dbDriver.toLowerCase(Locale.ROOT).contains("jtds")) {
            url = "jdbc:jtds:sqlserver://" + host + "/" + baseDeDatos;
        } else {
            url = "jdbc:sqlserver://" + host + ";databaseName=" + baseDeDatos;
        }
        if (dbParams != null && !dbParams.trim().isEmpty()) {
            url = url + (dbParams.startsWith(";") ? dbParams : ";" + dbParams);
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(dbDriver);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return new JdbcTemplate(dataSource);
    }

    JdbcTemplate crearJdbcTemplateSucre() {
        return crearJdbcTemplateSucursal(sucreHost, sucreDatabase, sucreUsername, sucrePassword);
    }

    boolean isSucre(String value) {
        return value != null && value.contains("sucre");
    }

    boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replaceAll("[\\s_\\-]+", "");
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    Object firstNonNull(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key);
            }
        }
        return null;
    }

    Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer parseInteger(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void addFiltro(Set<String> filtros, String value) {
        if (filtros == null || isBlank(value)) {
            return;
        }
        filtros.add(value.trim());
    }

    private SucursalDbInfo buildDbInfo(
            String preferredHost,
            String preferredDatabase,
            String fallbackHost,
            String fallbackDatabase,
            String preferredUsername,
            String preferredPassword,
            String fallbackUsername,
            String fallbackPassword,
            Integer idSucursal,
            String nombreSucursal) {
        String host = firstNonBlank(preferredHost, fallbackHost);
        String database = firstNonBlank(preferredDatabase, fallbackDatabase);
        String username = firstNonBlank(preferredUsername, fallbackUsername);
        String password = firstNonBlank(preferredPassword, fallbackPassword);
        if (isBlank(host) || isBlank(database) || isBlank(username) || isBlank(password)) {
            return null;
        }
        return new SucursalDbInfo(
                host.trim(),
                database.trim(),
                username.trim(),
                password.trim(),
                idSucursal,
                isBlank(nombreSucursal) ? null : nombreSucursal.trim()
        );
    }

    private Integer firstNonNull(Integer preferred, Integer fallback) {
        return preferred != null ? preferred : fallback;
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (!isBlank(preferred)) {
            return preferred.trim();
        }
        return fallback;
    }

    private String parseHostFromJdbcUrl(String jdbcUrl) {
        if (isBlank(jdbcUrl)) {
            return null;
        }
        String url = jdbcUrl.trim();
        int idx = url.indexOf("://");
        if (idx < 0) {
            return null;
        }

        String rest = url.substring(idx + 3);
        int sepSlash = rest.indexOf('/');
        int sepSemicolon = rest.indexOf(';');
        int end = -1;
        if (sepSlash >= 0 && sepSemicolon >= 0) {
            end = Math.min(sepSlash, sepSemicolon);
        } else if (sepSlash >= 0) {
            end = sepSlash;
        } else if (sepSemicolon >= 0) {
            end = sepSemicolon;
        }

        String hostPort = end >= 0 ? rest.substring(0, end) : rest;
        int comma = hostPort.indexOf(',');
        if (comma >= 0) {
            hostPort = hostPort.substring(0, comma);
        }
        int colon = hostPort.indexOf(':');
        if (colon >= 0) {
            hostPort = hostPort.substring(0, colon);
        }

        String host = hostPort.trim();
        return host.isEmpty() ? null : host;
    }

    private String parseDatabaseFromJdbcUrl(String jdbcUrl) {
        if (isBlank(jdbcUrl)) {
            return null;
        }
        String url = jdbcUrl.trim();
        String lower = url.toLowerCase(Locale.ROOT);
        String token = "databasename=";
        int idxDbName = lower.indexOf(token);
        if (idxDbName >= 0) {
            int start = idxDbName + token.length();
            int end = url.indexOf(';', start);
            String db = (end >= 0 ? url.substring(start, end) : url.substring(start)).trim();
            return db.isEmpty() ? null : db;
        }

        int idx = url.indexOf("://");
        if (idx < 0) {
            return null;
        }
        String rest = url.substring(idx + 3);
        int slash = rest.indexOf('/');
        if (slash < 0 || slash + 1 >= rest.length()) {
            return null;
        }

        String afterSlash = rest.substring(slash + 1);
        int end = afterSlash.indexOf(';');
        String db = (end >= 0 ? afterSlash.substring(0, end) : afterSlash).trim();
        return db.isEmpty() ? null : db;
    }

    static final class SucursalDbInfo {
        private final String host;
        private final String baseDeDatos;
        private final String username;
        private final String password;
        private final Integer idSucursal;
        private final String nombreSucursal;

        SucursalDbInfo(
                String host,
                String baseDeDatos,
                String username,
                String password,
                Integer idSucursal,
                String nombreSucursal) {
            this.host = host;
            this.baseDeDatos = baseDeDatos;
            this.username = username;
            this.password = password;
            this.idSucursal = idSucursal;
            this.nombreSucursal = nombreSucursal;
        }

        String getHost() {
            return host;
        }

        String getBaseDeDatos() {
            return baseDeDatos;
        }

        String getUsername() {
            return username;
        }

        String getPassword() {
            return password;
        }

        Integer getIdSucursal() {
            return idSucursal;
        }

        String getNombreSucursal() {
            return nombreSucursal;
        }
    }
}
