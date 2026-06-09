package com.seucorre.infra.scheduler;

import com.seucorre.avaliacao.application.AvaliacaoAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TreinoSchedulerTest {

    private AvaliacaoAppService avaliacaoAppService;
    private TreinoScheduler treinoScheduler;

    @BeforeEach
    void setUp() {
        avaliacaoAppService = mock(AvaliacaoAppService.class);
        treinoScheduler = new TreinoScheduler(avaliacaoAppService);
    }

    @Test
    void executaLembretesMatinaisDelegandoAoServico() {
        when(avaliacaoAppService.enviarLembretesMatinais()).thenReturn(2);

        treinoScheduler.executarLembretesMatinais();

        verify(avaliacaoAppService).enviarLembretesMatinais();
    }

    @Test
    void executaAberturaCheckinsDelegandoAoServico() {
        when(avaliacaoAppService.abrirCheckinsSemanais()).thenReturn(1);

        treinoScheduler.executarAberturaCheckins();

        verify(avaliacaoAppService).abrirCheckinsSemanais();
    }

    @Test
    void executaCalculoProgressosDelegandoAoServico() {
        when(avaliacaoAppService.recalcularProgressosAutomaticos()).thenReturn(3);

        treinoScheduler.executarCalculoProgressos();

        verify(avaliacaoAppService).recalcularProgressosAutomaticos();
    }
}
