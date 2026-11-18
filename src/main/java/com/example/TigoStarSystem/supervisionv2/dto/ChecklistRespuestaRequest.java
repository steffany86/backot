package com.example.TigoStarSystem.supervisionv2.dto;

/** Respuesta a un item del checklist de supervision (tbl_SupChecklistItem). */
public class ChecklistRespuestaRequest {
    private Integer idItem;
    private String respuesta;
    private String observacion;

    public Integer getIdItem() { return idItem; }
    public void setIdItem(Integer idItem) { this.idItem = idItem; }
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
