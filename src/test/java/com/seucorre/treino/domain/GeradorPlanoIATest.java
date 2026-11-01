package com.seucorre.treino.domain;

import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.shared.domain.RegraDezPorCento;
import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.domain.enums.TipoTreino;
import com.seucorre.usuario.domain.PerfilAtleta;
import com.seucorre.usuario.domain.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorPlanoIATest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-10-31T15:00:00Z"), ZoneOffset.UTC);

    private static final String PLANO_DA_IA = """
            {"totalSemanas": 3, "sessoes": [
              {"numeroSemana": 1, "tipo": "longo", "distanciaKm": 10},
              {"numeroSemana": 2, "tipo": "longo", "distanciaKm": 20},
              {"numeroSemana": 3, "tipo": "longo", "distanciaKm": 30}
            ]}
            """;

    @Test
    void limitaCadaSemanaA110PorCentoDaAnteriorMesmoQuandoAIaExagera() {
        GeradorPlanoIA gerador = new GeradorPlanoIA(prompt -> PLANO_DA_IA, new PromptBuilder(), RELOGIO);

        PlanoTreino plano = gerador.gerarPlano(criarUsuario(), Objetivo.COMPLETAR_10K);

        assertThat(plano.calcularVolumeSemanal(1)).isEqualByComparingTo("10.00");
        assertThat(plano.calcularVolumeSemanal(2)).isEqualByComparingTo("11.00");
        assertThat(plano.calcularVolumeSemanal(3)).isEqualByComparingTo("12.10");
    }

    @Test
    void ignoraAsDatasDaIaEAgendaAPartirDeHojeNosDiasEscolhidos() {
        String planoComDataNoPassado = """
                {"totalSemanas": 1, "sessoes": [
                  {"numeroSemana": 1, "dataPrevista": "2026-04-06", "tipo": "longo", "distanciaKm": 5, "paceAlvo": 0},
                  {"numeroSemana": 1, "dataPrevista": "2026-04-08", "tipo": "regenerativo", "distanciaKm": 3}
                ]}
                """;
        GeradorPlanoIA gerador = new GeradorPlanoIA(prompt -> planoComDataNoPassado, new PromptBuilder(), RELOGIO);

        PlanoTreino plano = gerador.gerarPlano(criarUsuario(), Objetivo.COMPLETAR_10K);

        assertThat(plano.getSessoes()).extracting(SessaoTreino::getDataPrevista)
                .containsExactly(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4));
        assertThat(plano.getDataInicio()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(plano.getSessoes().get(0).getPaceAlvo()).isNull();
    }

    @Test
    void oArredondamentoDepoisDoCorteNaoPassaDoLimite() {
        String planoQueArredondaParaCima = """
                {"totalSemanas": 2, "sessoes": [
                  {"numeroSemana": 1, "tipo": "longo", "distanciaKm": 7.85},
                  {"numeroSemana": 2, "tipo": "longo", "distanciaKm": 3},
                  {"numeroSemana": 2, "tipo": "regenerativo", "distanciaKm": 3},
                  {"numeroSemana": 2, "tipo": "intervalado", "distanciaKm": 3}
                ]}
                """;
        GeradorPlanoIA gerador = new GeradorPlanoIA(prompt -> planoQueArredondaParaCima, new PromptBuilder(), RELOGIO);

        PlanoTreino plano = gerador.gerarPlano(criarUsuario(), Objetivo.COMPLETAR_10K);

        assertThat(RegraDezPorCento.excede(plano.calcularVolumeSemanal(1), plano.calcularVolumeSemanal(2))).isFalse();
    }

    @Test
    void naReescritaASemanaUmNaoPassaDe110PorCentoDaSemanaAtualDoPlanoAntigo() {
        GeradorPlanoIA gerador = new GeradorPlanoIA(prompt -> PLANO_DA_IA, new PromptBuilder(), RELOGIO);
        PlanoTreino planoAntigo = new PlanoTreino();
        planoAntigo.setUsuario(criarUsuario());
        planoAntigo.adicionarSessao(sessao(3, "5.00"));
        CheckinSemanal checkin = new CheckinSemanal();
        checkin.setSemana(3);
        checkin.setNivelEsforco(8);
        checkin.setNivelDor(8);
        checkin.setHorasSonoSemana(5);

        PlanoTreino planoReescrito = gerador.reescreverPlano(planoAntigo, checkin);

        assertThat(planoReescrito.calcularVolumeSemanal(1)).isEqualByComparingTo("5.50");
        assertThat(planoReescrito.calcularVolumeSemanal(2)).isEqualByComparingTo("6.05");
    }

    @Test
    void comRiscoCriticoASemanaUmFicaComNoMaximo70PorCentoESemIntervalado() {
        String planoComIntervalado = """
                {"totalSemanas": 2, "sessoes": [
                  {"numeroSemana": 1, "tipo": "intervalado", "distanciaKm": 4},
                  {"numeroSemana": 1, "tipo": "longo", "distanciaKm": 6},
                  {"numeroSemana": 2, "tipo": "intervalado", "distanciaKm": 10}
                ]}
                """;
        GeradorPlanoIA gerador = new GeradorPlanoIA(prompt -> planoComIntervalado, new PromptBuilder(), RELOGIO);
        PlanoTreino planoAntigo = new PlanoTreino();
        planoAntigo.setUsuario(criarUsuario());
        planoAntigo.adicionarSessao(sessao(3, "10.00"));
        CheckinSemanal checkin = new CheckinSemanal();
        checkin.setSemana(3);
        checkin.setNivelEsforco(10);
        checkin.setNivelDor(10);
        checkin.setHorasSonoSemana(3);

        PlanoTreino planoReescrito = gerador.reescreverPlano(planoAntigo, checkin);

        assertThat(planoReescrito.calcularVolumeSemanal(1)).isLessThanOrEqualTo(new BigDecimal("7.00"));
        assertThat(planoReescrito.obterSessoesDaSemana(1)).extracting(SessaoTreino::getTipo)
                .doesNotContain(TipoTreino.INTERVALADO);
        assertThat(RegraDezPorCento.excede(planoReescrito.calcularVolumeSemanal(1), planoReescrito.calcularVolumeSemanal(2)))
                .isFalse();
    }

    @Test
    void naReescritaAsSemanasNovasComecamDepoisDoUltimoTreinoDaSemanaDoCheckin() {
        GeradorPlanoIA gerador = new GeradorPlanoIA(prompt -> PLANO_DA_IA, new PromptBuilder(), RELOGIO);
        PlanoTreino planoAntigo = new PlanoTreino();
        planoAntigo.setUsuario(criarUsuario());
        SessaoTreino ultimoTreino = sessao(3, "5.00");
        ultimoTreino.setDataPrevista(LocalDate.of(2026, 11, 4));
        planoAntigo.adicionarSessao(ultimoTreino);
        CheckinSemanal checkin = new CheckinSemanal();
        checkin.setSemana(3);
        checkin.setNivelEsforco(8);
        checkin.setNivelDor(8);
        checkin.setHorasSonoSemana(5);

        PlanoTreino planoReescrito = gerador.reescreverPlano(planoAntigo, checkin);

        assertThat(planoReescrito.getDataInicio()).isEqualTo(LocalDate.of(2026, 11, 6));
    }

    @Test
    void oPromptDaAnaliseLevaADecisaoDoSistemaEPedeTextoPuro() {
        StringBuilder promptRecebido = new StringBuilder();
        GeradorPlanoIA gerador = new GeradorPlanoIA(prompt -> {
            promptRecebido.append(prompt);
            return "ok";
        }, new PromptBuilder(), RELOGIO);
        CheckinSemanal checkin = new CheckinSemanal();
        checkin.setSemana(2);
        checkin.setNivelEsforco(6);
        checkin.setNivelDor(2);
        checkin.setHorasSonoSemana(6);

        gerador.gerarAnaliseCheckin(checkin, true);

        assertThat(promptRecebido.toString())
                .contains("Risco Moderado")
                .contains("as proximas semanas do plano serao ajustadas")
                .contains("nunca contradiz")
                .contains("sem markdown")
                .doesNotContain("sobrecarga=")
                .doesNotContain("=false");
    }

    private SessaoTreino sessao(int semana, String distanciaKm) {
        SessaoTreino sessao = new SessaoTreino();
        sessao.setNumeroSemana(semana);
        sessao.setTipo(TipoTreino.LONGO);
        sessao.setDistanciaKm(new BigDecimal(distanciaKm));
        return sessao;
    }

    private Usuario criarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setNome("Ana");
        usuario.setEmail("ana@seucorre.dev");
        usuario.setPesoKg(new BigDecimal("60.0"));
        usuario.setAlturaCm(new BigDecimal("165.0"));
        PerfilAtleta perfilAtleta = new PerfilAtleta();
        perfilAtleta.setNivelCondicionamento(NivelCondicionamento.INICIANTE);
        perfilAtleta.setObjetivo(Objetivo.COMPLETAR_10K);
        perfilAtleta.setDiasDisponiveisSemana(3);
        perfilAtleta.setDiasSemanaTreino("SEG,QUA,SEX");
        usuario.setPerfilAtleta(perfilAtleta);
        return usuario;
    }
}
