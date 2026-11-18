package com.example.TigoStarSystem.supervision.controller;

import com.example.TigoStarSystem.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Supervision V1 retirada. El flujo vigente es /supervisor/supervision_v2.
 * Toda peticion a /supervisor/supervision y /supervisor/supervision/** responde 410.
 * (La version anterior de este controlador esta en el historial de git.)
 */
@RestController
public class SupervisionController {

    @RequestMapping({"/supervisor/supervision", "/supervisor/supervision/**"})
    public void supervisionV1Deshabilitada() {
        throw new ApiException(
                HttpStatus.GONE,
                "SUPERVISION_V1_DISABLED",
                "Supervision V1 ya no esta disponible. Usa Supervision V2."
        );
    }
}
