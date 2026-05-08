package com.seucorre.shared.domain.event;

import com.seucorre.shared.domain.enums.NivelRisco;

import java.util.UUID;

public record PlanoReescritoEvent(
        UUID usuarioId,
        UUID planoAnteriorId,
        UUID novoPlanoId,
        UUID checkinId,
        Integer semana,
        NivelRisco nivelRisco
) implements DomainEvent {
}
