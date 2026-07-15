package com.example.TigoStarSystem.ot.repository;

import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class OtClontraerRepository {
	private static final DateTimeFormatter LEGACY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final JdbcTemplate centralJdbcTemplate;
	private final JdbcTemplate localJdbcTemplate;

	public OtClontraerRepository(
			@Qualifier("centralJdbcTemplate") JdbcTemplate centralJdbcTemplate,
			JdbcTemplate localJdbcTemplate) {
		this.centralJdbcTemplate = centralJdbcTemplate;
		this.localJdbcTemplate = localJdbcTemplate;
	}

	public Map<String, Object> traerOtPorTecnico(LocalDate fecha, String tecnico) {
		LocalDate fechaConsulta = fecha == null ? LocalDate.now() : fecha;
		Date fechaSql = Date.valueOf(fechaConsulta);
		String fechaLegacy = fechaConsulta.format(LEGACY_DATE_FORMAT);
		String tecnicoConsulta = tecnico == null ? "" : tecnico.trim();

		if (tecnicoConsulta.isEmpty()) {
			throw new IllegalArgumentException("El tecnico es obligatorio para consultar OT.");
		}

		List<Map<String, Object>> data = new ArrayList<>();
		String spFuente = "SIN_DATOS";
		List<Map<String, Object>> trazas = new ArrayList<>();

		List<Map<String, Object>> rowsSp1 = ejecutar(
				"BDControlOrdenes.dbo.spy_Ultimo_Estado_Dia_BO_CITA_MAKIRO",
				trazas,
				fechaLegacy,
				tecnicoConsulta
		);
		if (!rowsSp1.isEmpty() && data.isEmpty()) {
			data = rowsSp1;
			spFuente = "BDControlOrdenes.dbo.spy_Ultimo_Estado_Dia_BO_CITA_MAKIRO";
		}

		if (data.isEmpty()) {
			List<Map<String, Object>> rowsSp2 = ejecutarLocal(
					"dbo.sp_ObtenerListaOrdenesTrabajo_OTWEB_clon_paginacion",
					trazas,
					fechaSql
			);
			if (!rowsSp2.isEmpty()) {
				data = rowsSp2;
				spFuente = "dbo.sp_ObtenerListaOrdenesTrabajo_OTWEB_clon_paginacion";
			}
		}

		if (data.isEmpty()) {
			List<Map<String, Object>> rowsSp3 = ejecutarLocal(
					"dbo.sp_ObtenerListaOrdenesTrabajo_clon_paginacion",
					trazas,
					fechaSql
			);
			if (!rowsSp3.isEmpty()) {
				data = rowsSp3;
				spFuente = "dbo.sp_ObtenerListaOrdenesTrabajo_clon_paginacion";
			}
		}
		agregarValidacionVentaDetalle(data, fechaSql, trazas);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("fecha", fechaConsulta.toString());
		out.put("tecnico", tecnicoConsulta);
		out.put("salesforceValidado", true);
		out.put("spFuente", spFuente);
		out.put("fuenteMensaje", "Estoy llegando de este SP: " + spFuente);
		out.put("filas", data.size());
		out.put("ejecuciones", trazas);
		out.put("data", data);
		return out;
	}

	public boolean existeTecnicoSalesforce(String salesforce) {
		String valor = salesforce == null ? "" : salesforce.trim();
		if (valor.isEmpty()) {
			return false;
		}

		String sqlControlOrdenes =
				"SELECT TOP 1 1 " +
				"FROM BDControlOrdenes.dbo.tbl_ConformacionCuadrillaDiario " +
				"WHERE UPPER(LTRIM(RTRIM(ISNULL(salesforce, '')))) = UPPER(?) " +
				"AND ISNULL(e_eliminado, 0) = 0";

		try {
			List<Map<String, Object>> rows = centralJdbcTemplate.queryForList(sqlControlOrdenes, valor);
			if (rows != null && !rows.isEmpty()) {
				return true;
			}
		} catch (DataAccessException ex) {
			// Intentar variante sin prefijo de base por compatibilidad de contexto.
		}

		String sqlLocalContexto =
				"SELECT TOP 1 1 " +
				"FROM dbo.tbl_ConformacionCuadrillaDiario " +
				"WHERE UPPER(LTRIM(RTRIM(ISNULL(salesforce, '')))) = UPPER(?) " +
				"AND ISNULL(e_eliminado, 0) = 0";

		try {
			List<Map<String, Object>> rows = centralJdbcTemplate.queryForList(sqlLocalContexto, valor);
			return rows != null && !rows.isEmpty();
		} catch (DataAccessException ex) {
			return false;
		}
	}

	private List<Map<String, Object>> ejecutar(String spName, List<Map<String, Object>> trazas, Object... params) {
		try {
			List<Map<String, Object>> rows = centralJdbcTemplate.queryForList(
					buildExec(spName, params.length),
					params
			);
			Map<String, Object> traza = new LinkedHashMap<>();
			traza.put("sp", spName);
			traza.put("ok", true);
			traza.put("filas", rows == null ? 0 : rows.size());
			trazas.add(traza);
			return rows == null ? new ArrayList<>() : rows;
		} catch (DataAccessException ex) {
			Map<String, Object> traza = new LinkedHashMap<>();
			traza.put("sp", spName);
			traza.put("ok", false);
			traza.put("filas", 0);
			traza.put("error", ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage());
			trazas.add(traza);
			return new ArrayList<>();
		}
	}

	private List<Map<String, Object>> ejecutarLocal(String spName, List<Map<String, Object>> trazas, Object... params) {
		try {
			List<Map<String, Object>> rows = localJdbcTemplate.queryForList(
					buildExec(spName, params.length),
					params
			);
			Map<String, Object> traza = new LinkedHashMap<>();
			traza.put("sp", spName);
			traza.put("ok", true);
			traza.put("filas", rows == null ? 0 : rows.size());
			trazas.add(traza);
			return rows == null ? new ArrayList<>() : rows;
		} catch (DataAccessException ex) {
			Map<String, Object> traza = new LinkedHashMap<>();
			traza.put("sp", spName);
			traza.put("ok", false);
			traza.put("filas", 0);
			traza.put("error", ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage());
			trazas.add(traza);
			return new ArrayList<>();
		}
	}

	private void agregarValidacionVentaDetalle(List<Map<String, Object>> rows, Date fechaSql, List<Map<String, Object>> trazas) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		for (Map<String, Object> row : rows) {
			agregarCamposVentaDetalle(row, fechaSql, trazas);
			row.put("estadoVentaDetalle", calcularEstadoVentaDetalle(row));
			removerCamposInternosVentaDetalle(row);
		}
	}

	private void agregarCamposVentaDetalle(Map<String, Object> row, Date fechaSql, List<Map<String, Object>> trazas) {
		Integer nroOt = toInteger(findValue(row,
				"OrdenTrabajo", "ordenTrabajo", "orden_trabajo", "NroOT", "nroOT", "OT", "ot", "Codigo", "codigo"));
		Integer numeroCliente = toInteger(findValue(row,
				"CodigoCliente", "codigoCliente", "codigo_cliente", "NumeroCliente", "numeroCliente", "Cliente_Nro", "cliente_nro", "CODIGO"));

		if (nroOt == null || nroOt <= 0 || numeroCliente == null || numeroCliente <= 0) {
			row.putIfAbsent("ExisteVenta", 0);
			row.putIfAbsent("CantidadVentas", 0);
			row.putIfAbsent("TieneDetalle", 0);
			row.putIfAbsent("TieneDetalleEnCodigoVenta", 0);
			row.putIfAbsent("CantidadDetalles", 0);
			row.putIfAbsent("AddMaterial_o_CargoUsuario", 0);
			row.putIfAbsent("HabilitarCargarMaterial", 0);
			return;
		}

		List<Map<String, Object>> validacion = ejecutarLocal(
				"dbo.spx_ValidarVentaYDetallewb_clon_paginacion",
				trazas,
				fechaSql,
				nroOt,
				numeroCliente
		);
		if (validacion.isEmpty()) {
			row.putIfAbsent("ExisteVenta", 0);
			row.putIfAbsent("CantidadVentas", 0);
			row.putIfAbsent("TieneDetalle", 0);
			row.putIfAbsent("TieneDetalleEnCodigoVenta", 0);
			row.putIfAbsent("CantidadDetalles", 0);
			row.putIfAbsent("AddMaterial_o_CargoUsuario", 0);
			row.putIfAbsent("HabilitarCargarMaterial", 0);
			return;
		}

		Map<String, Object> validationRow = validacion.get(0);
		putFromValidation(row, validationRow, "ExisteVenta", "ExisteVenta", "existeventa");
		putFromValidation(row, validationRow, "CantidadVentas", "CantidadVentas", "cantidadventas");
		putFromValidation(row, validationRow, "TieneDetalle", "TieneDetalle", "tienedetalle");
		putFromValidation(row, validationRow, "TieneDetalleEnCodigoVenta", "TieneDetalleEnCodigoVenta", "tienedetalleencodigoventa");
		putFromValidation(row, validationRow, "CantidadDetalles", "CantidadDetalles", "cantidaddetalles");
		putFromValidation(row, validationRow, "AddMaterial_o_CargoUsuario",
				"AddMaterial_o_CargoUsuario", "addmaterial_o_cargousuario", "addMaterialOCargoUsuario");
		putFromValidation(row, validationRow, "HabilitarCargarMaterial",
				"HabilitarCargarMaterial", "habilitarcargarmaterial", "puedeCargarMaterial");
	}

	private void putFromValidation(Map<String, Object> target, Map<String, Object> source, String targetKey, String... sourceKeys) {
		Object value = findValue(source, sourceKeys);
		if (value != null) {
			target.put(targetKey, value);
		}
	}

	private String calcularEstadoVentaDetalle(Map<String, Object> row) {
		Boolean existeVentaFlag = toBoolean(findValue(row, "ExisteVenta", "existeventa"));
		Integer cantidadVentas = toInteger(findValue(row, "CantidadVentas", "cantidadventas"));
		boolean existeVenta = Boolean.TRUE.equals(existeVentaFlag)
				|| (cantidadVentas != null && cantidadVentas > 0);

		if (!existeVenta) {
			return "pendiente";
		}

		boolean tieneDetalle = Boolean.TRUE.equals(toBoolean(findValue(row, "TieneDetalle", "tienedetalle")));
		Boolean detalleCodigoFlag = toBoolean(findValue(row, "TieneDetalleEnCodigoVenta", "tienedetalleencodigoventa"));
		Integer cantidadDetalles = toInteger(findValue(row, "CantidadDetalles", "cantidaddetalles"));
		boolean tieneDetalleEnCodigoVenta = Boolean.TRUE.equals(detalleCodigoFlag)
				|| (cantidadDetalles != null && cantidadDetalles > 0);

		if (tieneDetalle && !tieneDetalleEnCodigoVenta) {
			return "pendiente-de-material";
		}

		return "finalizada";
	}

	private void removerCamposInternosVentaDetalle(Map<String, Object> row) {
		if (row == null || row.isEmpty()) {
			return;
		}
		removeByNormalizedKey(row, "ExisteVenta");
		removeByNormalizedKey(row, "CantidadVentas");
		removeByNormalizedKey(row, "TieneDetalle");
		removeByNormalizedKey(row, "TieneDetalleEnCodigoVenta");
		removeByNormalizedKey(row, "CantidadDetalles");
		removeByNormalizedKey(row, "AddMaterial_o_CargoUsuario");
		removeByNormalizedKey(row, "HabilitarCargarMaterial");
	}

	private void removeByNormalizedKey(Map<String, Object> row, String key) {
		String normalizedKey = normalizeKey(key);
		List<String> keysToRemove = new ArrayList<>();
		for (String existingKey : row.keySet()) {
			if (normalizeKey(existingKey).equals(normalizedKey)) {
				keysToRemove.add(existingKey);
			}
		}
		for (String existingKey : keysToRemove) {
			row.remove(existingKey);
		}
	}

	private Object findValue(Map<String, Object> row, String... candidates) {
		if (row == null || row.isEmpty() || candidates == null || candidates.length == 0) {
			return null;
		}
		for (String candidate : candidates) {
			String normalizedCandidate = normalizeKey(candidate);
			for (Map.Entry<String, Object> entry : row.entrySet()) {
				if (normalizeKey(entry.getKey()).equals(normalizedCandidate)) {
					return entry.getValue();
				}
			}
		}
		return null;
	}

	private Boolean toBoolean(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		}
		String normalized = value.toString().trim().toLowerCase();
		if (normalized.isEmpty()) {
			return null;
		}
		if ("si".equals(normalized) || "true".equals(normalized) || "1".equals(normalized) || "s".equals(normalized)) {
			return true;
		}
		if ("no".equals(normalized) || "false".equals(normalized) || "0".equals(normalized) || "n".equals(normalized)) {
			return false;
		}
		return null;
	}

	private Integer toInteger(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		try {
			return Integer.parseInt(value.toString().trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private String normalizeKey(String key) {
		if (key == null) {
			return "";
		}
		return key.replace("_", "")
				.replace("-", "")
				.replace(" ", "")
				.toLowerCase();
	}

	private String buildExec(String spName, int paramCount) {
		if (paramCount <= 0) {
			return "EXEC " + spName;
		}
		StringBuilder sql = new StringBuilder("EXEC ").append(spName).append(" ");
		for (int i = 0; i < paramCount; i++) {
			if (i > 0) {
				sql.append(", ");
			}
			sql.append("?");
		}
		return sql.toString();
	}

}
