package com.example.TigoStarSystem.ot.service;

import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.ot.repository.OtClontraerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class OtClontraerService {
    private final OtClontraerRepository otClontraerRepository;

    public OtClontraerService(OtClontraerRepository otClontraerRepository) {
        this.otClontraerRepository = otClontraerRepository;
    }

    public Map<String, Object> listarPorTecnico(LocalDate fecha, String tecnico) {
        if (tecnico == null || tecnico.trim().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "El parametro tecnico es obligatorio."
            );
        }

        String tecnicoConsulta = tecnico.trim();
        if (!otClontraerRepository.existeTecnicoSalesforce(tecnicoConsulta)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "No existe un tecnico con ese salesforce en BDControlOrdenes."
            );
        }

        LocalDate fechaConsulta = fecha == null ? LocalDate.now() : fecha;
        return otClontraerRepository.traerOtPorTecnico(fechaConsulta, tecnicoConsulta);
    }
}
