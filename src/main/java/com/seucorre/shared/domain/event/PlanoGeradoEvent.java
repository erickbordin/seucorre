package com.seucorre.shared.domain.event;

import com.seucorre.shared.domain.enums.Objetivo;

import java.util.UUID;

public record PlanoGeradoEvent(
        UUID usuarioId,
        UUID planoId,
        Objetivo objetivo,
        Integer totalSemanas
) implements DomainEvent {
}
