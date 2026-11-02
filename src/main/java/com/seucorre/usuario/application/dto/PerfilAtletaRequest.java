package com.seucorre.usuario.application.dto;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PerfilAtletaRequest(
        @NotNull NivelCondicionamento nivelCondicionamento,
        @NotNull Objetivo objetivo,
        @NotNull Boolean jaCorre,
        @NotNull @Min(1) @Max(7) Integer diasDisponiveisSemana,
        @NotBlank String diasSemanaTreino,
        @Future LocalDate dataProva,
        @Size(max = 120) String nomeProva
) {
    public PerfilAtletaRequest(NivelCondicionamento nivelCondicionamento,
                               Objetivo objetivo,
                               Boolean jaCorre,
                               Integer diasDisponiveisSemana,
                               String diasSemanaTreino) {
        this(nivelCondicionamento, objetivo, jaCorre, diasDisponiveisSemana, diasSemanaTreino, null, null);
    }
}
