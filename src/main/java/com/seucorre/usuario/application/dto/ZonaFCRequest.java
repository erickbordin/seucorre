package com.seucorre.usuario.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ZonaFCRequest(
        @NotNull @Min(1) @Max(5) Integer zona,
        @NotBlank String nome,
        @NotNull @Min(1) Integer fcMin,
        @NotNull @Min(1) Integer fcMax,
        Boolean personalizada
) {}
