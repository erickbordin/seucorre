package com.seucorre.infra.notification;

import com.seucorre.shared.domain.event.CheckinProcessadoEvent;
import com.seucorre.shared.domain.event.PlanoReescritoEvent;
import com.seucorre.shared.domain.event.TreinoConcluidoEvent;
import com.seucorre.shared.domain.enums.NivelRisco;
import com.seucorre.shared.domain.enums.TipoTreino;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificacaoServiceTest {

    private final NotificacaoService service = new NotificacaoService();

    @Test
    void processaPlanoReescritoValido() {
        PlanoReescritoEvent event = new PlanoReescritoEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                3,
                NivelRisco.ALTO
        );

        assertThatCode(() -> service.notificarPlanoReescrito(event))
                .doesNotThrowAnyException();
    }

    @Test
    void processaCheckinProcessadoValido() {
        CheckinProcessadoEvent event = new CheckinProcessadoEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                2,
                NivelRisco.MODERADO,
                true
        );

        assertThatCode(() -> service.notificarCheckinProcessado(event))
                .doesNotThrowAnyException();
    }

    @Test
    void processaTreinoConcluidoValido() {
        TreinoConcluidoEvent event = new TreinoConcluidoEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                TipoTreino.LONGO,
                false
        );

        assertThatCode(() -> service.notificarTreinoConcluido(event))
                .doesNotThrowAnyException();
    }

    @Test
    void rejeitaPlanoReescritoNulo() {
        assertThatThrownBy(() -> service.notificarPlanoReescrito(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Evento de plano reescrito é obrigatório.");
    }

    @Test
    void processaLembreteTreinoValido() {
        assertThatCode(() -> service.enviarLembreteTreino(UUID.randomUUID(), 2, 1))
                .doesNotThrowAnyException();
    }

    @Test
    void processaAberturaCheckinValida() {
        assertThatCode(() -> service.enviarAberturaCheckin(UUID.randomUUID(), 4))
                .doesNotThrowAnyException();
    }
}
