package com.seucorre.treino.domain;

import com.seucorre.shared.domain.enums.StatusPlano;
import com.seucorre.shared.domain.enums.StatusTreino;
import com.seucorre.shared.domain.enums.TipoTreino;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlanoTreinoTest {

    @Test
    void calculaCargaSemanalESegueRegraDosDezPorCento() {
        PlanoTreino planoTreino = new PlanoTreino();
        planoTreino.adicionarSessao(criarSessao(1, "10.00", TipoTreino.LONGO));
        planoTreino.adicionarSessao(criarSessao(2, "11.00", TipoTreino.LONGO));

        assertThat(planoTreino.calcularCargaSemanal(1)).isEqualTo(70.0d);
        assertThat(planoTreino.calcularCargaSemanal(2)).isEqualTo(77.0d);
        assertThat(planoTreino.verificarRegra10Porcento(2)).isTrue();
    }

    @Test
    void identificaQuebraDaRegraDosDezPorCento() {
        PlanoTreino planoTreino = new PlanoTreino();
        planoTreino.adicionarSessao(criarSessao(1, "10.00", TipoTreino.LONGO));
        planoTreino.adicionarSessao(criarSessao(2, "12.00", TipoTreino.LONGO));

        assertThat(planoTreino.verificarRegra10Porcento(2)).isFalse();
    }

    @Test
    void calculaProgressoGeralPelasSessoesExecutadas() {
        PlanoTreino planoTreino = new PlanoTreino();

        SessaoTreino sessao1 = criarSessao(1, "5.00", TipoTreino.REGENERATIVO);
        SessaoTreino sessao2 = criarSessao(1, "8.00", TipoTreino.LONGO);
        SessaoTreino sessao3 = criarSessao(2, "6.00", TipoTreino.INTERVALADO);

        RegistroTreino registroTreino = new RegistroTreino();
        registroTreino.setStatus(StatusTreino.CONCLUIDO);
        sessao1.registrarExecucao(registroTreino);

        planoTreino.adicionarSessao(sessao1);
        planoTreino.adicionarSessao(sessao2);
        planoTreino.adicionarSessao(sessao3);

        assertThat(planoTreino.calcularProgressoGeral()).isEqualTo(33.333333333333336d);
    }

    @Test
    void controlaStatusDoPlano() {
        PlanoTreino planoTreino = new PlanoTreino();

        planoTreino.pausar();
        assertThat(planoTreino.getStatus()).isEqualTo(StatusPlano.PAUSADO);

        planoTreino.reativar();
        assertThat(planoTreino.getStatus()).isEqualTo(StatusPlano.ATIVO);

        planoTreino.concluir();
        assertThat(planoTreino.getStatus()).isEqualTo(StatusPlano.CONCLUIDO);
    }

    private SessaoTreino criarSessao(int semana, String distanciaKm, TipoTreino tipoTreino) {
        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setNumeroSemana(semana);
        sessaoTreino.setDistanciaKm(new BigDecimal(distanciaKm));
        sessaoTreino.setTipo(tipoTreino);
        return sessaoTreino;
    }
}
