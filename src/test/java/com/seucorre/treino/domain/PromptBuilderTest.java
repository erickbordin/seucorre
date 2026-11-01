package com.seucorre.treino.domain;

import com.seucorre.avaliacao.domain.CheckinSemanal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    @Test
    void promptDeReescritaDizQuantasSemanasGerarEMostraAEstruturaSemDatas() {
        PlanoTreino plano = new PlanoTreino();
        plano.setTotalSemanas(8);
        CheckinSemanal checkin = new CheckinSemanal();
        checkin.setSemana(3);

        String prompt = new PromptBuilder().construirPromptReescrita(plano, checkin, List.of());

        assertThat(prompt).contains("gerar todas as semanas de 1 a 5, com totalSemanas igual a 5");
        assertThat(prompt).contains("\"numeroSemana\": 1");
        assertThat(prompt).doesNotContain("dataPrevista");
        assertThat(prompt).doesNotContain("\"paceAlvo\": 0");
    }
}
