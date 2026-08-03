package com.example.TigoStarSystem.system;

import com.example.TigoStarSystem.common.ApiException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MaintenanceService {
    private static final String SQL_CREATE_TABLE =
            "IF OBJECT_ID('dbo.tbl_system_maintenance', 'U') IS NULL " +
                    "BEGIN " +
                    "CREATE TABLE dbo.tbl_system_maintenance (" +
                    "id INT NOT NULL PRIMARY KEY, " +
                    "active BIT NOT NULL, " +
                    "message NVARCHAR(300) NULL, " +
                    "changed_by NVARCHAR(80) NULL, " +
                    "changed_at DATETIME2 NOT NULL" +
                    "); " +
                    "INSERT INTO dbo.tbl_system_maintenance (id, active, message, changed_by, changed_at) " +
                    "VALUES (1, 0, 'SISTEMA ABAJO. CAMBIOS EN PROCESO.', 'sistemas', GETDATE()); " +
                    "END;";
    public static final String SISTEMAS_USUARIO = "sistemas";
    public static final String SISTEMAS_PASSWORD = "123";
    public static final String DEFAULT_MESSAGE = "SISTEMA ABAJO. CAMBIOS EN PROCESO.";

    private final JdbcTemplate jdbcTemplate;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private volatile String message = DEFAULT_MESSAGE;
    private volatile String changedBy = SISTEMAS_USUARIO;
    private volatile OffsetDateTime changedAt = OffsetDateTime.now();

    public MaintenanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        loadState();
    }

    public boolean isActive() {
        return active.get();
    }

    public boolean isSistemasCredentials(String usuario, String password) {
        return normalize(usuario).equals(SISTEMAS_USUARIO) && SISTEMAS_PASSWORD.equals(password);
    }

    public boolean isSistemasUser(String usuario) {
        return normalize(usuario).equals(SISTEMAS_USUARIO);
    }

    public void validateSistemasCredentials(String usuario, String password) {
        if (!isSistemasCredentials(usuario, password)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SISTEMAS_CREDENTIALS", "Credenciales de sistemas invalidas.");
        }
    }

    public Map<String, Object> setActive(boolean nextActive, String nextMessage, String usuario) {
        active.set(nextActive);
        message = normalizeMessage(nextMessage);
        changedBy = isSistemasUser(usuario) ? SISTEMAS_USUARIO : normalize(usuario);
        changedAt = OffsetDateTime.now();
        persistState();
        return status();
    }

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("active", active.get());
        result.put("message", message);
        result.put("changedBy", changedBy);
        result.put("changedAt", changedAt);
        return result;
    }

    public ApiException maintenanceException() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "MAINTENANCE_ACTIVE", message);
    }

    private String normalizeMessage(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_MESSAGE;
        }
        return value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void loadState() {
        try {
            ensureTable();
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT TOP 1 active, message, changed_by, changed_at FROM dbo.tbl_system_maintenance WHERE id = 1"
            );
            if (rows == null || rows.isEmpty()) {
                persistState();
                return;
            }
            Map<String, Object> row = rows.get(0);
            active.set(toBoolean(row.get("active")));
            message = normalizeMessage(row.get("message") == null ? null : String.valueOf(row.get("message")));
            changedBy = normalize(row.get("changed_by") == null ? SISTEMAS_USUARIO : String.valueOf(row.get("changed_by")));
            changedAt = toOffsetDateTime(row.get("changed_at"));
        } catch (DataAccessException ex) {
            persistFallbackState();
        }
    }

    private void persistState() {
        try {
            ensureTable();
            jdbcTemplate.update(
                    "UPDATE dbo.tbl_system_maintenance SET active = ?, message = ?, changed_by = ?, changed_at = ? WHERE id = 1",
                    active.get(),
                    message,
                    changedBy,
                    Timestamp.from(changedAt.toInstant())
            );
        } catch (DataAccessException ex) {
            persistFallbackState();
        }
    }

    private void ensureTable() {
        jdbcTemplate.execute(SQL_CREATE_TABLE);
    }

    private void persistFallbackState() {
        changedAt = changedAt == null ? OffsetDateTime.now() : changedAt;
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value instanceof OffsetDateTime) return (OffsetDateTime) value;
        if (value instanceof java.util.Date) {
            return OffsetDateTime.ofInstant(((java.util.Date) value).toInstant(), ZoneOffset.UTC);
        }
        return OffsetDateTime.now();
    }
}
