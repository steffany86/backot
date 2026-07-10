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

	public OtClontraerRepository(@Qualifier("centralJdbcTemplate") JdbcTemplate centralJdbcTemplate) {
		this.centralJdbcTemplate = centralJdbcTemplate;
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

		List<Map<String, Object>> rowsSp2 = ejecutar(
				"BDControlOrdenes.dbo.sp_ObtenerListaOrdenesTrabajo_OTWEB_clon_paginacion",
				trazas,
				fechaSql
		);
		if (!rowsSp2.isEmpty() && data.isEmpty()) {
			data = rowsSp2;
			spFuente = "BDControlOrdenes.dbo.sp_ObtenerListaOrdenesTrabajo_OTWEB_clon_paginacion";
		}

		List<Map<String, Object>> rowsSp3 = ejecutar(
				"BDControlOrdenes.dbo.sp_ObtenerListaOrdenesTrabajo_clon_paginacion",
				trazas,
				fechaSql
		);
		if (!rowsSp3.isEmpty() && data.isEmpty()) {
			data = rowsSp3;
			spFuente = "BDControlOrdenes.dbo.sp_ObtenerListaOrdenesTrabajo_clon_paginacion";
		}

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
