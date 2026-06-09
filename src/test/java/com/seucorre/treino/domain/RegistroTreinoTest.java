package com.seucorre.treino.domain;

import com.seucorre.shared.domain.enums.StatusTreino;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroTreinoTest {

    @Test
    void detectaAlertaDeSaudeQuandoHaDorOuDoenca() {
        RegistroTreino registroTreino = new RegistroTreino();
        registroTreino.setSentiuDor(true);

        assertThat(registroTreino.temAlertaDeSaude()).isTrue();

        registroTreino.setSentiuDor(false);
        registroTreino.setDoente(true);

        assertThat(registroTreino.temAlertaDeSaude()).isTrue();
    }

    @Test
    void calculaDesvioPercentualDeDistancia() {
        RegistroTreino registroTreino = new RegistroTreino();
        registroTreino.setDistanciaRealKm(new BigDecimal("9.00"));

        assertThat(registroTreino.calcularDesvioDistancia(new BigDecimal("10.00"))).isEqualTo(-10.0d);
    }

    @Test
    void calculaDesvioAbsolutoDePace() {
        RegistroTreino registroTreino = new RegistroTreino();
        registroTreino.setPaceMedioReal(330);

        assertThat(registroTreino.calcularDesvioPace(new BigDecimal("320"))).isEqualTo(10.0d);
    }

    @Test
    void geraResumoParaIaComSinaisMaisRelevantes() {
        RegistroTreino registroTreino = new RegistroTreino();
        registroTreino.setStatus(StatusTreino.PARCIAL);
        registroTreino.setDistanciaRealKm(new BigDecimal("7.50"));
        registroTreino.setFcMedia(158);
        registroTreino.setPaceMedioReal(345);
        registroTreino.setSentiuDor(true);
        registroTreino.setObservacao("perna pesada");

        assertThat(registroTreino.gerarResumoParaIA())
                .contains("status=PARCIAL")
                .contains("distanciaKm=7.50")
                .contains("fcMedia=158")
                .contains("paceMedio=345")
                .contains("alertaSaude=true")
                .contains("observacao=perna pesada");
    }
}
