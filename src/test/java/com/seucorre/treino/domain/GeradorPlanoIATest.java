package com.seucorre.treino.domain;

import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.domain.enums.TipoTreino;
import com.seucorre.usuario.domain.PerfilAtleta;
import com.seucorre.usuario.domain.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorPlanoIATest {

    private static final String PLANO_DA_IA = """
            {"totalSemanas": 3, "sessoes": [
              {"numeroSemana": 1, "tipo": "longo", "distanciaKm": 10},
              {"numeroSemana": 2, "tipo": "longo", "distanciaKm": 20},
              {"numeroSemana": 3, "tipo": "longo", "distanciaKm": 30}
            ]}
            """;

    @Test
    void limitaCadaSemanaA110PorCentoDaAnteriorMesmoQuandoAIaExagera() {
        GeradorPlanoIA gerador = new GeradorPlanoIA(prompt -> PLANO_DA_IA, new PromptBuilder());

        PlanoTreino plano = gerador.gerarPlano(criarUsuario(), Objetivo.COMPLETAR_10K);

        assertThat(plano.calcularVolumeSemanal(1)).isEqualByComparingTo("10.00");
        assertThat(plano.calcularVolumeSemanal(2)).isEqualByComparingTo("11.00");
        assertThat(plano.calcularVolumeSemanal(3)).isEqualByComparingTo("12.10");
    }

    @Test
    void naReescritaASemanaUmNaoPassaDe110PorCentoDaSemanaAtualDoPlanoAntigo() {
        GeradorPlanoIA gerador = new GeradorPlanoIA(prompt -> PLANO_DA_IA, new PromptBuilder());
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
