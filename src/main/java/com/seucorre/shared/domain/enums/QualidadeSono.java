package com.seucorre.shared.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QualidadeSono {

    RUIM("Ruim"),
    REGULAR("Regular"),
    BOA("Boa"),
    OTIMA("Ótima");

    private final String descricao;
}
