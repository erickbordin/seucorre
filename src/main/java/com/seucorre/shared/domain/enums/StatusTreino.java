package com.seucorre.shared.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusTreino {
    
    PENDENTE("Pendente"),
    CONCLUIDO("Concluído"),
    PARCIAL("Concluído Parcialmente"),
    PERDIDO("Perdido/Não Realizado");

    private final String descricao;
}