package com.seucorre.shared.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Objetivo {
    
    EMAGRECER("Emagrecer"),
    COMPLETAR_5K("Completar 5km"),
    COMPLETAR_10K("Completar 10km"),
    COMPLETAR_MEIA_MARATONA("Completar 21km (Meia Maratona)"),
    COMPLETAR_MARATONA("Completar 42km (Maratona)"),
    MELHORAR_5K("Melhorar 5km"),
    MELHORAR_10K("Melhorar 10km"),
    MELHORAR_MEIA_MARATONA("Melhorar 21km (Meia Maratona)"),
    MELHORAR_MARATONA("Melhorar 42km (Maratona)"),
    SAUDE_GERAL("Saúde Geral");

    private final String descricao;
}