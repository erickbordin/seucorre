package com.seucorre.treino.application.dto;

import com.seucorre.shared.domain.enums.Objetivo;
import jakarta.validation.constraints.NotNull;

public record GerarPlanoRequest(
        @NotNull Objetivo objetivo
) {
}
