package com.seucorre.avaliacao.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CheckinSemanalRequest(
        @NotNull UUID usuarioId,
        @NotNull UUID planoId,
        @NotNull @Min(1) Integer semana,
        @NotNull @PastOrPresent LocalDate dataCheckin,
        @NotNull @Min(0) @Max(10) Integer nivelEsforco,
        @NotNull @Min(0) @Max(10) Integer nivelDor,
        @NotNull @Min(0) @Max(24) Integer horasSonoSemana,
        @Size(max = 100) String avaliacao
) {
}
