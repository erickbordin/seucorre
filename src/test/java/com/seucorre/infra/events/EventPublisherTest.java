package com.seucorre.infra.events;

import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.domain.event.PlanoGeradoEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventPublisherTest {

    @Test
    void publicaEventoDeDominioViaSpring() {
        ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
        EventPublisher eventPublisher = new EventPublisher(applicationEventPublisher);
        PlanoGeradoEvent event = new PlanoGeradoEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Objetivo.COMPLETAR_10K,
                8
        );

        eventPublisher.publish(event);

        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    void rejeitaEventoNulo() {
        ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
        EventPublisher eventPublisher = new EventPublisher(applicationEventPublisher);

        assertThatThrownBy(() -> eventPublisher.publish(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Evento de domínio é obrigatório.");
    }
}
