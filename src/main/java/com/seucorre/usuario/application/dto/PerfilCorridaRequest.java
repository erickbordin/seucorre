package com.seucorre.usuario.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.List;

public record PerfilCorridaRequest(
        @DecimalMin(value = "0.1", message = "Pace 5k deve ser maior que zero") BigDecimal pace5kMinKm,
        @DecimalMin(value = "0.1", message = "Pace 10k deve ser maior que zero") BigDecimal pace10kMinKm,
        @DecimalMin(value = "0.1", message = "Pace 21k deve ser maior que zero") BigDecimal pace21kMinKm,
        @DecimalMin(value = "0.1", message = "Pace 42k deve ser maior que zero") BigDecimal pace42kMinKm,
        @Min(1) Integer vo2Estimado,
        List<@Valid ZonaFCRequest> zonasFc
) {}
