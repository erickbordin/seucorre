package com.seucorre.usuario.application.dto;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PerfilAtletaRequest(
        NivelCondicionamento nivelCondicionamento,
        Objetivo objetivo,
        Boolean jaCorre,
        @Min(1) @Max(7) Integer diasDisponiveisSemana,
        String diasSemanaTreino
) {}
