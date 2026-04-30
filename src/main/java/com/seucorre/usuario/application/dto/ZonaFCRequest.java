package com.seucorre.usuario.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ZonaFCRequest(
        @Min(1) @Max(5) Integer zona,
        String nome,
        @Min(1) Integer fcMin,
        @Min(1) Integer fcMax,
        Boolean personalizada
) {}
