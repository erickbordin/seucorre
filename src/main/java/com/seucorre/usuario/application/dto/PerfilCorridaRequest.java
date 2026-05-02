package com.seucorre.usuario.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record PerfilCorridaRequest(
        BigDecimal pace5kMinKm,
        BigDecimal pace10kMinKm,
        BigDecimal pace21kMinKm,
        BigDecimal pace42kMinKm,
        Integer vo2Estimado,
        List<ZonaFCRequest> zonasFc
) {}
