package com.seucorre.usuario.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CondicaoSaudeTest {

    @Test
    void restringeAtividadeParaTiposRestritivosMesmoComEspacosECaseDiferente() {
        CondicaoSaude condicaoSaude = new CondicaoSaude();
        condicaoSaude.setTipo("  cardiaca ");
        condicaoSaude.setAtiva(true);

        assertThat(condicaoSaude.restringeAtividade()).isTrue();
        assertThat(condicaoSaude.exigeAutorizacaoMedica()).isTrue();
    }

    @Test
    void exigeMonitoramentoFcParaHipertensaoAtiva() {
        CondicaoSaude condicaoSaude = new CondicaoSaude();
        condicaoSaude.setTipo("hipertensao");
        condicaoSaude.setAtiva(true);

        assertThat(condicaoSaude.restringeAtividade()).isFalse();
        assertThat(condicaoSaude.exigeMonitoramentoFC()).isTrue();
        assertThat(condicaoSaude.exigeAutorizacaoMedica()).isTrue();
    }

    @Test
    void ignoraRestricoesQuandoCondicaoEstaInativa() {
        CondicaoSaude condicaoSaude = new CondicaoSaude();
        condicaoSaude.setTipo("LESAO_GRAVE");
        condicaoSaude.setAtiva(false);

        assertThat(condicaoSaude.restringeAtividade()).isFalse();
        assertThat(condicaoSaude.exigeMonitoramentoFC()).isFalse();
        assertThat(condicaoSaude.exigeAutorizacaoMedica()).isFalse();
    }
}
