package com.seucorre.shared.domain.event;

import com.seucorre.shared.domain.enums.NivelRisco;

import java.util.UUID;

public record CheckinProcessadoEvent(
        UUID usuarioId,
        UUID planoId,
        UUID checkinId,
        Integer semana,
        NivelRisco nivelRisco,
        Boolean planoReescrito
) implements DomainEvent {
}
