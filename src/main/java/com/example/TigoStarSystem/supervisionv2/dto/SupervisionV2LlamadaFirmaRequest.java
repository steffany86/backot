package com.example.TigoStarSystem.supervisionv2.dto;

/** Firmas con las que se completa una llamada de atencion pendiente (data URL en base64). */
public class SupervisionV2LlamadaFirmaRequest {
    private String firmaTecnico;
    private String firmaTestigo;
    private String testigo;

    public String getFirmaTecnico() { return firmaTecnico; }
    public void setFirmaTecnico(String firmaTecnico) { this.firmaTecnico = firmaTecnico; }
    public String getFirmaTestigo() { return firmaTestigo; }
    public void setFirmaTestigo(String firmaTestigo) { this.firmaTestigo = firmaTestigo; }
    public String getTestigo() { return testigo; }
    public void setTestigo(String testigo) { this.testigo = testigo; }
}
