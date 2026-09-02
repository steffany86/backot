package com.example.TigoStarSystem.system;

import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/system/maintenance")
public class MaintenanceController {
    private final MaintenanceService maintenanceService;
    private final AuthService authService;

    public MaintenanceController(MaintenanceService maintenanceService, AuthService authService) {
        this.maintenanceService = maintenanceService;
        this.authService = authService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        return ResponseEntity.ok(ApiResponse.of(maintenanceService.status(), "Estado de mantenimiento."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody MaintenanceRequest request) {
        maintenanceService.validateSistemasCredentials(request.getUsuario(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.of(maintenanceService.status(), "Credenciales de sistemas validas."));
    }

    @PostMapping("/on")
    public ResponseEntity<ApiResponse<Map<String, Object>>> activar(@RequestBody MaintenanceRequest request) {
        maintenanceService.validateSistemasCredentials(request.getUsuario(), request.getPassword());
        Map<String, Object> status = maintenanceService.setActive(true, request.getMessage(), request.getUsuario());
        authService.cerrarSesionesNoSistemas();
        return ResponseEntity.ok(ApiResponse.of(status, "Mantenimiento activado."));
    }

    @PostMapping("/off")
    public ResponseEntity<ApiResponse<Map<String, Object>>> desactivar(@RequestBody MaintenanceRequest request) {
        maintenanceService.validateSistemasCredentials(request.getUsuario(), request.getPassword());
        Map<String, Object> status = maintenanceService.setActive(false, request.getMessage(), request.getUsuario());
        return ResponseEntity.ok(ApiResponse.of(status, "Mantenimiento desactivado."));
    }

    public static class MaintenanceRequest {
        private String usuario;
        private String password;
        private String message;

        public String getUsuario() {
            return usuario;
        }

        public void setUsuario(String usuario) {
            this.usuario = usuario;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
