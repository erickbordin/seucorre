package com.seucorre.shared.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NivelRisco {
    
    BAIXO("Risco Baixo"),
    MODERADO("Risco Moderado"),
    ALTO("Risco Alto"),
    CRITICO("Risco Crítico");

    private final String descricao;
}