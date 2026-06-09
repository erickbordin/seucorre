package com.seucorre.shared.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusPlano {
    
    ATIVO("Plano Ativo"),
    PAUSADO("Plano Pausado"),
    CONCLUIDO("Plano Concluído"),
    CANCELADO("Plano Cancelado");

    private final String descricao;
}