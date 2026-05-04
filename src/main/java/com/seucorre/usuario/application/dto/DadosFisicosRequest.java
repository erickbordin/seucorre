package com.seucorre.usuario.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosFisicosRequest(
        @NotNull @DecimalMin(value = "1.0", message = "Peso deve ser maior que zero") BigDecimal pesoKg,
        @NotNull @DecimalMin(value = "0.5", message = "Altura deve ser informada em centímetros") BigDecimal alturaCm,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank String genero,
        @Min(20) @Max(240) Integer fcRepouso,
        @Min(80) @Max(240) Integer fcMaxima,
        @Min(0) @Max(24) Integer horasSonoMedia,
        Boolean sedentario
) {}
