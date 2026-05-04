package com.seucorre.treino.domain;

import com.seucorre.shared.domain.enums.StatusTreino;
import com.seucorre.shared.domain.enums.TipoTreino;
import com.seucorre.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessaoTreinoTest {

    @Test
    void calculaCargaComBaseNaDistanciaENoFatorDoTipo() {
        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setTipo(TipoTreino.LONGO);
        sessaoTreino.setDistanciaKm(new BigDecimal("12.50"));

        assertThat(sessaoTreino.calcularCarga()).isEqualTo(87.5d);
    }

    @Test
    void registraExecucaoUmaUnicaVez() {
        SessaoTreino sessaoTreino = new SessaoTreino();
        RegistroTreino registroTreino = new RegistroTreino();
        registroTreino.setStatus(StatusTreino.CONCLUIDO);

        sessaoTreino.registrarExecucao(registroTreino);

        assertThat(sessaoTreino.foiExecutada()).isTrue();
        assertThat(sessaoTreino.estaPendente()).isFalse();
        assertThat(sessaoTreino.obterRegistro()).isSameAs(registroTreino);
        assertThat(registroTreino.getSessaoTreino()).isSameAs(sessaoTreino);

        assertThatThrownBy(() -> sessaoTreino.registrarExecucao(new RegistroTreino()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A sessão de treino já possui registro de execução.");
    }

    @Test
    void identificaSessaoPendenteEAtrasada() {
        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setDataPrevista(LocalDate.now().minusDays(1));

        assertThat(sessaoTreino.estaPendente()).isTrue();
        assertThat(sessaoTreino.estaAtrasada()).isTrue();
    }

    @Test
    void naoMarcaComoAtrasadaQuandoJaFoiExecutada() {
        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setDataPrevista(LocalDate.now().minusDays(2));
        sessaoTreino.registrarExecucao(new RegistroTreino());

        assertThat(sessaoTreino.estaAtrasada()).isFalse();
    }
}
