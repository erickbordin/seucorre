package com.seucorre.avaliacao.application.dto;

import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.shared.domain.enums.NivelRisco;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.usuario.domain.Usuario;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AvaliacaoDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void aceitaCheckinSemanalRequestValido() {
        CheckinSemanalRequest request = new CheckinSemanalRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                3,
                LocalDate.now(),
                7,
                2,
                8,
                "Semana controlada e sem dor relevante"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void recusaCheckinSemanalRequestInvalido() {
        CheckinSemanalRequest request = new CheckinSemanalRequest(
                null,
                null,
                0,
                LocalDate.now().plusDays(1),
                11,
                -1,
                25,
                "x".repeat(101)
        );

        Set<String> violationPaths = validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertThat(violationPaths).contains(
                "usuarioId",
                "planoId",
                "semana",
                "dataCheckin",
                "nivelEsforco",
                "nivelDor",
                "horasSonoSemana",
                "avaliacao"
        );
    }

    @Test
    void montaAnaliseRiscoDtoComCamposDerivadosDoCheckin() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());

        PlanoTreino planoTreino = new PlanoTreino();
        planoTreino.setId(UUID.randomUUID());

        CheckinSemanal checkinSemanal = new CheckinSemanal();
        checkinSemanal.setId(UUID.randomUUID());
        checkinSemanal.setUsuario(usuario);
        checkinSemanal.setPlano(planoTreino);
        checkinSemanal.setSemana(4);
        checkinSemanal.setDataCheckin(LocalDate.of(2026, 5, 4));
        checkinSemanal.setNivelEsforco(9);
        checkinSemanal.setNivelDor(7);
        checkinSemanal.setHorasSonoSemana(4);
        checkinSemanal.setAvaliacao("Cansaco acima do normal");
        checkinSemanal.setAnaliseIA("Reduzir carga da semana.");
        checkinSemanal.setPlanoReescrito(true);

        AnaliseRiscoDTO dto = AnaliseRiscoDTO.from(checkinSemanal);

        assertThat(dto.checkinId()).isEqualTo(checkinSemanal.getId());
        assertThat(dto.usuarioId()).isEqualTo(usuario.getId());
        assertThat(dto.planoId()).isEqualTo(planoTreino.getId());
        assertThat(dto.nivelRisco()).isEqualTo(NivelRisco.CRITICO);
        assertThat(dto.sobrecarga()).isTrue();
        assertThat(dto.recuperacaoInsuficiente()).isTrue();
        assertThat(dto.precisaReescreverPlano()).isTrue();
        assertThat(dto.analiseIA()).isEqualTo("Reduzir carga da semana.");
        assertThat(dto.planoReescrito()).isTrue();
    }

    @Test
    void montaProgressoSemanalDtoComMetricasCalculadas() {
        PlanoTreino planoTreino = new PlanoTreino();
        planoTreino.setId(UUID.randomUUID());

        ProgressoSemanal progressoSemanal = new ProgressoSemanal();
        progressoSemanal.setId(UUID.randomUUID());
        progressoSemanal.setPlano(planoTreino);
        progressoSemanal.setNumeroSemana(3);
        progressoSemanal.setDataInicioSemana(LocalDate.of(2026, 5, 18));
        progressoSemanal.setVolumeKm(new BigDecimal("24.50"));
        progressoSemanal.setPaceMedio(new BigDecimal("5.45"));
        progressoSemanal.setFcRepousoMedio(56);
        progressoSemanal.setTreinosConcluidos(2);
        progressoSemanal.setTreinosParciais(1);
        progressoSemanal.setTreinosPerdidos(1);

        ProgressoSemanalDTO dto = ProgressoSemanalDTO.from(progressoSemanal);

        assertThat(dto.id()).isEqualTo(progressoSemanal.getId());
        assertThat(dto.planoId()).isEqualTo(planoTreino.getId());
        assertThat(dto.totalTreinos()).isEqualTo(4);
        assertThat(dto.taxaAdesao()).isEqualTo(75.0d);
        assertThat(dto.resumo()).contains("Semana 3");
    }
}
