package com.seucorre.infra.notification;

import com.seucorre.shared.domain.event.CheckinProcessadoEvent;
import com.seucorre.shared.domain.event.PlanoReescritoEvent;
import com.seucorre.shared.domain.event.TreinoConcluidoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;

import java.util.UUID;

@Service
public class NotificacaoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificacaoService.class);

    @Async
    @EventListener
    public void notificarPlanoReescrito(PlanoReescritoEvent event) {
        validarEvento(event, "Evento de plano reescrito é obrigatório.");
        enviarPush(
                event.usuarioId(),
                "Plano atualizado",
                "Seu plano da semana " + event.semana() + " foi reescrito com base no check-in."
        );
    }

    @Async
    @EventListener
    public void notificarCheckinProcessado(CheckinProcessadoEvent event) {
        validarEvento(event, "Evento de check-in processado é obrigatório.");

        String mensagem = Boolean.TRUE.equals(event.planoReescrito())
                ? "Seu check-in da semana " + event.semana() + " gerou ajustes no plano."
                : "Seu check-in da semana " + event.semana() + " foi processado com sucesso.";

        enviarPush(event.usuarioId(), "Check-in processado", mensagem);
    }

    @Async
    @EventListener
    public void notificarTreinoConcluido(TreinoConcluidoEvent event) {
        validarEvento(event, "Evento de treino concluído é obrigatório.");

        String mensagem = Boolean.TRUE.equals(event.alertaSaude())
                ? "Seu treino da semana " + event.numeroSemana() + " foi registrado com alerta de saúde."
                : "Seu treino da semana " + event.numeroSemana() + " foi registrado com sucesso.";

        enviarPush(event.usuarioId(), "Treino registrado", mensagem);
    }

    @Async
    public void enviarLembreteTreino(UUID usuarioId, Integer semana, int totalTreinosPendentes) {
        if (semana == null) {
            throw new IllegalArgumentException("Semana do lembrete é obrigatória.");
        }
        if (totalTreinosPendentes <= 0) {
            throw new IllegalArgumentException("Quantidade de treinos pendentes deve ser positiva.");
        }

        String mensagem = totalTreinosPendentes == 1
                ? "Voce tem 1 treino previsto para hoje na semana " + semana + "."
                : "Voce tem " + totalTreinosPendentes + " treinos previstos para hoje na semana " + semana + ".";

        enviarPush(usuarioId, "Lembrete de treino", mensagem);
    }

    @Async
    public void enviarAberturaCheckin(UUID usuarioId, Integer semana) {
        if (semana == null) {
            throw new IllegalArgumentException("Semana do check-in é obrigatória.");
        }

        enviarPush(
                usuarioId,
                "Check-in liberado",
                "Seu check-in da semana " + semana + " ja esta disponivel para preenchimento."
        );
    }

    private void enviarPush(UUID usuarioId, String titulo, String mensagem) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("Usuário destinatário é obrigatório.");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Título da notificação é obrigatório.");
        }
        if (mensagem == null || mensagem.isBlank()) {
            throw new IllegalArgumentException("Mensagem da notificação é obrigatória.");
        }

        LOGGER.info("Disparando push FCM/APNs para usuarioId={} titulo='{}' mensagem='{}'", usuarioId, titulo, mensagem);
    }

    private void validarEvento(Object event, String mensagem) {
        if (event == null) {
            throw new IllegalArgumentException(mensagem);
        }
    }
}
