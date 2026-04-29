package com.seucorre.shared.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoTreino {
    
    FORCA("Treino de Força (Musculação)", 1.2),
    CARDIO("Treino Cardiovascular", 1.0),
    HIIT("Treino Intervalado de Alta Intensidade", 1.5),
    MOBILIDADE("Mobilidade e Alongamento", 0.8);

    private final String descricao;
    private final double fatorCarga;
}