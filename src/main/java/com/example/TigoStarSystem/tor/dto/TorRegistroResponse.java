package com.example.TigoStarSystem.tor.dto;

public class TorRegistroResponse {
    private final Integer id;
    private final String usuarioRegistra;

    public TorRegistroResponse(Integer id, String usuarioRegistra) {
        this.id = id;
        this.usuarioRegistra = usuarioRegistra;
    }

    public Integer getId() {
        return id;
    }

    public String getUsuarioRegistra() {
        return usuarioRegistra;
    }
}
