package com.seucorre.treino.application.dto;

import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.domain.enums.StatusPlano;
import com.seucorre.shared.domain.enums.StatusTreino;
import com.seucorre.shared.domain.enums.TipoTreino;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.domain.RegistroTreino;
import com.seucorre.treino.domain.SessaoTreino;
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

class TreinoDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void aceitaRequestsValidosDeGeracaoERegistro() {
        GerarPlanoRequest gerarPlanoRequest = new GerarPlanoRequest(Objetivo.COMPLETAR_10K);
        RegistroTreinoRequest registroTreinoRequest = new RegistroTreinoRequest(
                UUID.randomUUID(),
                StatusTreino.CONCLUIDO,
                new BigDecimal("7.50"),
                48,
                150,
                176,
                320,
                7,
                false,
                null,
                false,
                false,
                "Treino concluido conforme planejado"
        );

        assertThat(validator.validate(gerarPlanoRequest)).isEmpty();
        assertThat(validator.validate(registroTreinoRequest)).isEmpty();
    }

    @Test
    void recusaRequestsInvalidosDeGeracaoERegistro() {
        GerarPlanoRequest gerarPlanoRequest = new GerarPlanoRequest(null);
        RegistroTreinoRequest registroTreinoRequest = new RegistroTreinoRequest(
                null,
                null,
                BigDecimal.ZERO,
                0,
                0,
                500,
                0,
                11,
                true,
                "dor",
                null,
                null,
                "obs"
        );

        Set<String> gerarPaths = validator.validate(gerarPlanoRequest).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
        Set<String> registroPaths = validator.validate(registroTreinoRequest).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertThat(gerarPaths).contains("objetivo");
        assertThat(registroPaths).contains(
                "treinoId",
                "status",
                "distanciaRealKm",
                "duracaoRealMin",
                "fcMedia",
                "fcMaxima",
                "paceMedioReal",
                "esforcoPercebido"
        );
    }

    @Test
    void montaPlanoTreinoDtoComSessoesERegistro() {
        PlanoTreino planoTreino = new PlanoTreino();
        planoTreino.setId(UUID.randomUUID());
        planoTreino.setTotalSemanas(2);
        planoTreino.setDataInicio(LocalDate.of(2026, 5, 5));
        planoTreino.setDataFim(LocalDate.of(2026, 5, 18));
        planoTreino.setKmTotais(new BigDecimal("18.00"));
        planoTreino.setStatus(StatusPlano.ATIVO);
        planoTreino.setResumoIA("Plano base 10k");
        planoTreino.setGuiaTiposTreino("rodagem leve, longo");

        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setId(UUID.randomUUID());
        sessaoTreino.setNumeroSemana(1);
        sessaoTreino.setDataPrevista(LocalDate.of(2026, 5, 5));
        sessaoTreino.setTipo(TipoTreino.LONGO);
        sessaoTreino.setDistanciaKm(new BigDecimal("8.00"));
        sessaoTreino.setDuracaoMinutos(50);
        sessaoTreino.setIntensidade("leve");
        sessaoTreino.setZonaFcAlvo("Z2");
        sessaoTreino.setPaceAlvo(new BigDecimal("6.10"));
        sessaoTreino.setDescricao("Longo de base");

        RegistroTreino registroTreino = new RegistroTreino();
        registroTreino.setId(UUID.randomUUID());
        registroTreino.setStatus(StatusTreino.CONCLUIDO);
        registroTreino.setDistanciaRealKm(new BigDecimal("8.10"));
        registroTreino.setDuracaoRealMin(51);
        registroTreino.setFcMedia(148);
        registroTreino.setFcMaxima(172);
        registroTreino.setPaceMedioReal(365);
        registroTreino.setEsforcoPercebido(6);
        registroTreino.setSentiuDor(false);
        registroTreino.setDoente(false);
        registroTreino.setViagem(false);
        registroTreino.setObservacao("Boa execucao");
        sessaoTreino.registrarExecucao(registroTreino);

        planoTreino.adicionarSessao(sessaoTreino);

        PlanoTreinoDTO planoTreinoDTO = PlanoTreinoDTO.from(planoTreino);

        assertThat(planoTreinoDTO.id()).isEqualTo(planoTreino.getId());
        assertThat(planoTreinoDTO.totalSemanas()).isEqualTo(2);
        assertThat(planoTreinoDTO.sessoes()).hasSize(1);
        assertThat(planoTreinoDTO.sessoes().get(0).tipo()).isEqualTo(TipoTreino.LONGO);
        assertThat(planoTreinoDTO.sessoes().get(0).registro()).isNotNull();
        assertThat(planoTreinoDTO.sessoes().get(0).registro().status()).isEqualTo(StatusTreino.CONCLUIDO);
        assertThat(planoTreinoDTO.sessoes().get(0).registro().duracaoRealMin()).isEqualTo(51);
        assertThat(planoTreinoDTO.sessoes().get(0).registro().alertaSaude()).isFalse();
        assertThat(planoTreinoDTO.progressoGeral()).isEqualTo(100.0d);
    }
}
