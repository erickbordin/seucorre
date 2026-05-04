package com.seucorre.usuario.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosFisicosRequest(
        @DecimalMin(value = "1.0", message = "Peso deve ser maior que zero") BigDecimal pesoKg,
        @DecimalMin(value = "0.5", message = "Altura deve ser informada em centímetros") BigDecimal alturaCm,
        LocalDate dataNascimento,
        String genero,
        @Min(20) @Max(240) Integer fcRepouso,
        @Min(80) @Max(240) Integer fcMaxima,
        @Min(0) @Max(24) Integer horasSonoMedia,
        Boolean sedentario
) {}
