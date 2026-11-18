package com.example.TigoStarSystem.supervisionv2.dto;

/** Fila del cruce de materiales de una supervision (tbl_SupMaterialCruce / tbl_SupervisionCruceMaterial). */
public class MaterialCruceRequest {
    private Integer idMaterial;
    private Integer cantSistema;
    private Integer cantFisico;
    private String observacion;

    public Integer getIdMaterial() { return idMaterial; }
    public void setIdMaterial(Integer idMaterial) { this.idMaterial = idMaterial; }
    public Integer getCantSistema() { return cantSistema; }
    public void setCantSistema(Integer cantSistema) { this.cantSistema = cantSistema; }
    public Integer getCantFisico() { return cantFisico; }
    public void setCantFisico(Integer cantFisico) { this.cantFisico = cantFisico; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
