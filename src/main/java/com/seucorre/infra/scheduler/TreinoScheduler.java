package com.seucorre.infra.scheduler;

import com.seucorre.avaliacao.application.AvaliacaoAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TreinoScheduler {

    private final AvaliacaoAppService avaliacaoAppService;

    @Scheduled(cron = "${seucorre.scheduler.treino.lembrete-manha-cron:0 0 6 * * *}", zone = "${seucorre.scheduler.zone:UTC}")
    public void executarLembretesMatinais() {
        avaliacaoAppService.enviarLembretesMatinais();
    }

    @Scheduled(cron = "${seucorre.scheduler.treino.abertura-checkin-cron:0 0 8 * * MON}", zone = "${seucorre.scheduler.zone:UTC}")
    public void executarAberturaCheckins() {
        avaliacaoAppService.abrirCheckinsSemanais();
    }

    @Scheduled(cron = "${seucorre.scheduler.treino.calculo-progresso-cron:0 0 23 * * *}", zone = "${seucorre.scheduler.zone:UTC}")
    public void executarCalculoProgressos() {
        avaliacaoAppService.recalcularProgressosAutomaticos();
    }
}
