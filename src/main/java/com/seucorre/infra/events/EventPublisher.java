package com.seucorre.infra.events;

import com.seucorre.shared.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    public void publish(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Evento de domínio é obrigatório.");
        }
        applicationEventPublisher.publishEvent(event);
    }
}
