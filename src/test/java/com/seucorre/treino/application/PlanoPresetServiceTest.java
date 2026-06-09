package com.seucorre.treino.application;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.usuario.domain.PerfilAtleta;
import com.seucorre.usuario.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

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
            assertThat(planoTreino.verificarRegra10Porcento(semana)).isTrue();
        }
        assertThat(planoTreino.getResumoIA()).contains("preset");
    }

    @Test
    void falhaQuandoCombinacaoNaoEstaNoCatalogoMvp() {
        Usuario usuario = criarUsuario(Objetivo.COMPLETAR_MARATONA, NivelCondicionamento.AVANCADO, 5, "SEG,TER,QUI,SAB,DOM");

        assertThatThrownBy(() -> service.gerarPlano(usuario, Objetivo.COMPLETAR_MARATONA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ainda não possui plano preset");
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
