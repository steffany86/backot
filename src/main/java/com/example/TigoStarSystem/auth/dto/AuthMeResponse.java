package com.example.TigoStarSystem.auth.dto;

import java.time.OffsetDateTime;

public class AuthMeResponse {
    private final AuthLoginResponse usuario;
    private final OffsetDateTime expira;

    public AuthMeResponse(AuthLoginResponse usuario, OffsetDateTime expira) {
        this.usuario = usuario;
        this.expira = expira;
    }

    public AuthLoginResponse getUsuario() {
        return usuario;
    }

    public OffsetDateTime getExpira() {
        return expira;
    }
}
