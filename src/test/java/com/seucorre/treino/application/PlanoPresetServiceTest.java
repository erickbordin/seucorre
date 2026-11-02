package com.seucorre.treino.application;

import com.seucorre.shared.domain.RegraDezPorCento;
import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.domain.SessaoTreino;
import com.seucorre.usuario.domain.PerfilAtleta;
import com.seucorre.usuario.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanoPresetServiceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-06-09T12:00:00Z"), ZoneOffset.UTC);
    private final PlanoPresetService service = new PlanoPresetService(new DefaultResourceLoader(), fixedClock);

    @Test
    void geraPlanoPresetComDatasDistribuidasNosDiasSelecionados() {
        Usuario usuario = criarUsuario(Objetivo.COMPLETAR_5K, NivelCondicionamento.INICIANTE, 3, "SEG,QUA,SEX");

        PlanoTreino planoTreino = service.gerarPlano(usuario, Objetivo.COMPLETAR_5K);

        assertThat(planoTreino.getTotalSemanas()).isEqualTo(8);
        assertThat(planoTreino.getDataInicio()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(planoTreino.getSessoes()).hasSize(24);
        assertThat(planoTreino.getSessoes().get(0).getDataPrevista()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(planoTreino.getSessoes().get(1).getDataPrevista()).isEqualTo(LocalDate.of(2026, 6, 12));
        assertThat(planoTreino.getSessoes().get(2).getDataPrevista()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(planoTreino.getKmTotais()).isNotNull();
        assertThat(planoTreino.getKmTotais()).isGreaterThan(BigDecimal.ZERO);
        for (int semana = 2; semana <= planoTreino.getTotalSemanas(); semana++) {
            assertThat(RegraDezPorCento.excede(
                    planoTreino.calcularVolumeSemanal(semana - 1),
                    planoTreino.calcularVolumeSemanal(semana)
            )).isFalse();
        }
        assertThat(planoTreino.getResumoIA()).startsWith("Plano de 8 semanas");
    }

    @Test
    void falhaQuandoCombinacaoNaoEstaNoCatalogoMvp() {
        Usuario usuario = criarUsuario(Objetivo.COMPLETAR_MARATONA, NivelCondicionamento.AVANCADO, 5, "SEG,TER,QUI,SAB,DOM");

        assertThatThrownBy(() -> service.gerarPlano(usuario, Objetivo.COMPLETAR_MARATONA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ainda não tem plano disponível");
    }

    @Test
    void geraMeiaMaratonaDe12SemanasQueTerminaNaProvaSemQuebrarARegraDosDezPorCento() {
        for (NivelCondicionamento nivel : new NivelCondicionamento[]{NivelCondicionamento.INICIANTE, NivelCondicionamento.INTERMEDIARIO}) {
            for (String dias : new String[]{"SEG,QUA,SAB", "SEG,QUA,SEX,DOM"}) {
                Usuario usuario = criarUsuario(Objetivo.COMPLETAR_MEIA_MARATONA, nivel, dias.split(",").length, dias);

                PlanoTreino planoTreino = service.gerarPlano(usuario, Objetivo.COMPLETAR_MEIA_MARATONA);

                assertThat(planoTreino.getTotalSemanas()).isEqualTo(12);
                assertThat(planoTreino.getSessoes().get(planoTreino.getSessoes().size() - 1).getDistanciaKm())
                        .isEqualByComparingTo("21.1");
                for (int semana = 2; semana <= 12; semana++) {
                    assertThat(RegraDezPorCento.excede(
                            planoTreino.calcularVolumeSemanal(semana - 1),
                            planoTreino.calcularVolumeSemanal(semana)
                    )).isFalse();
                }
            }
        }
    }

    @Test
    void nivelAvancadoUsaOPlanoIntermediario() {
        Usuario usuario = criarUsuario(Objetivo.COMPLETAR_10K, NivelCondicionamento.AVANCADO, 4, "SEG,QUA,SEX,DOM");

        PlanoTreino planoTreino = service.gerarPlano(usuario, Objetivo.COMPLETAR_10K);

        assertThat(planoTreino.getResumoIA()).contains("nível intermediario");
    }

    @Test
    void comDataDaProvaOPlanoTerminaNaProvaEGanhaSemanasDeBase() {
        Usuario usuario = criarUsuario(Objetivo.COMPLETAR_MEIA_MARATONA, NivelCondicionamento.INICIANTE, 3, "SEG,QUA,SAB");
        usuario.getPerfilAtleta().setDataProva(LocalDate.of(2026, 9, 27));
        usuario.getPerfilAtleta().setNomeProva("Meia de Teste");

        PlanoTreino planoTreino = service.gerarPlano(usuario, Objetivo.COMPLETAR_MEIA_MARATONA);

        List<SessaoTreino> sessoes = planoTreino.getSessoes();
        SessaoTreino prova = sessoes.get(sessoes.size() - 1);
        assertThat(prova.getDataPrevista()).isEqualTo(LocalDate.of(2026, 9, 27));
        assertThat(prova.getDistanciaKm()).isEqualByComparingTo("21.1");
        assertThat(planoTreino.getDataFim()).isEqualTo(LocalDate.of(2026, 9, 27));
        assertThat(planoTreino.getDataProva()).isEqualTo(LocalDate.of(2026, 9, 27));
        assertThat(planoTreino.getNomeProva()).isEqualTo("Meia de Teste");
        assertThat(planoTreino.getTotalSemanas()).isEqualTo(16);
        assertThat(planoTreino.getDataInicio()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(planoTreino.getResumoIA()).endsWith("Começa com 4 semanas de base para terminar no dia da prova.");
        for (int semana = 2; semana <= planoTreino.getTotalSemanas(); semana++) {
            assertThat(RegraDezPorCento.excede(
                    planoTreino.calcularVolumeSemanal(semana - 1),
                    planoTreino.calcularVolumeSemanal(semana)
            )).isFalse();
        }
    }

    @Test
    void provaDistanteLimitaAsSemanasDeBaseEComecaMaisTarde() {
        Usuario usuario = criarUsuario(Objetivo.COMPLETAR_MEIA_MARATONA, NivelCondicionamento.INICIANTE, 3, "SEG,QUA,SAB");
        usuario.getPerfilAtleta().setDataProva(LocalDate.of(2027, 6, 9));

        PlanoTreino planoTreino = service.gerarPlano(usuario, Objetivo.COMPLETAR_MEIA_MARATONA);

        assertThat(planoTreino.getTotalSemanas()).isEqualTo(16);
        assertThat(planoTreino.getDataInicio()).isEqualTo(LocalDate.of(2027, 2, 20));
        assertThat(planoTreino.getDataFim()).isEqualTo(LocalDate.of(2027, 6, 9));
    }

    @Test
    void provaPertoDemaisInformaAPrimeiraDataPossivel() {
        Usuario usuario = criarUsuario(Objetivo.COMPLETAR_MEIA_MARATONA, NivelCondicionamento.INICIANTE, 3, "SEG,QUA,SAB");
        usuario.getPerfilAtleta().setDataProva(LocalDate.of(2026, 7, 1));

        assertThatThrownBy(() -> service.gerarPlano(usuario, Objetivo.COMPLETAR_MEIA_MARATONA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Escolha uma prova a partir de 30/08/2026");
    }

    @Test
    void dataDaProvaSoValeParaOObjetivoDoPerfil() {
        Usuario usuario = criarUsuario(Objetivo.COMPLETAR_MEIA_MARATONA, NivelCondicionamento.INICIANTE, 3, "SEG,QUA,SAB");
        usuario.getPerfilAtleta().setDataProva(LocalDate.of(2026, 9, 27));

        PlanoTreino planoTreino = service.gerarPlano(usuario, Objetivo.COMPLETAR_5K);

        assertThat(planoTreino.getDataProva()).isNull();
        assertThat(planoTreino.getDataInicio()).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    private Usuario criarUsuario(Objetivo objetivo,
                                 NivelCondicionamento nivelCondicionamento,
                                 Integer diasDisponiveisSemana,
                                 String diasSemanaTreino) {
        Usuario usuario = new Usuario();
        usuario.setNome("Preset Runner");
        usuario.setEmail("preset@seucorre.dev");
        usuario.setPesoKg(new BigDecimal("70.0"));
        usuario.setAlturaCm(new BigDecimal("175.0"));
        usuario.setDataNascimento(LocalDate.of(1990, 1, 10));

        PerfilAtleta perfilAtleta = new PerfilAtleta();
        perfilAtleta.setNivelCondicionamento(nivelCondicionamento);
        perfilAtleta.setObjetivo(objetivo);
        perfilAtleta.setJaCorre(Boolean.TRUE);
        perfilAtleta.setDiasDisponiveisSemana(diasDisponiveisSemana);
        perfilAtleta.setDiasSemanaTreino(diasSemanaTreino);
        usuario.setPerfilAtleta(perfilAtleta);
        return usuario;
    }
}
