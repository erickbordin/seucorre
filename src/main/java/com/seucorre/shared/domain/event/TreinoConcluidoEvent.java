package com.seucorre.shared.domain.event;

import com.seucorre.shared.domain.enums.TipoTreino;

import java.util.UUID;

public record TreinoConcluidoEvent(
        UUID usuarioId,
        UUID planoId,
        UUID treinoId,
        UUID registroId,
        Integer numeroSemana,
        TipoTreino tipo,
        Boolean alertaSaude
) implements DomainEvent {
}
