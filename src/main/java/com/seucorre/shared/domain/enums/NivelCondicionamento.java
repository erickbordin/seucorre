package com.seucorre.shared.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NivelCondicionamento {
    
    INICIANTE("Iniciante"),
    INTERMEDIARIO("Intermediário"),
    AVANCADO("Avançado");

    private final String descricao;
}
