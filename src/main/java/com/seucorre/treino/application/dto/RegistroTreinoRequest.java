package com.seucorre.treino.application.dto;

import com.seucorre.shared.domain.enums.StatusTreino;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record RegistroTreinoRequest(
        @NotNull UUID treinoId,
        @NotNull StatusTreino status,
        @DecimalMin(value = "0.0", inclusive = false, message = "Distância real deve ser maior que zero")
        BigDecimal distanciaRealKm,
        @Min(1) @Max(240) Integer fcMedia,
        @Min(1) @Max(240) Integer fcMaxima,
        @Min(1) Integer paceMedioReal,
        @Min(0) @Max(10) Integer esforcoPercebido,
        Boolean sentiuDor,
        @Size(max = 100) String localDor,
        Boolean doente,
        Boolean viagem,
        @Size(max = 1000) String observacao
) {
}
