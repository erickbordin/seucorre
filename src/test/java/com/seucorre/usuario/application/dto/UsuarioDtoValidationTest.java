package com.seucorre.usuario.application.dto;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.domain.enums.PlataformaRelogio;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void aceitaUsuarioCadastroRequestValido() {
        UsuarioCadastroRequest request = cadastroValido();

        Set<ConstraintViolation<UsuarioCadastroRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void recusaUsuarioCadastroComCamposObrigatoriosAninhadosInvalidos() {
        UsuarioCadastroRequest request = new UsuarioCadastroRequest(
                "",
                "email-invalido",
                "123",
                null,
                new DadosFisicosRequest(
                        null,
                        new BigDecimal("0.40"),
                        null,
                        "",
                        10,
                        300,
                        30,
                        null
                ),
                new PerfilAtletaRequest(
                        null,
                        null,
                        null,
                        0,
                        ""
                ),
                new PerfilCorridaRequest(
                        new BigDecimal("0.00"),
                        null,
                        null,
                        null,
                        0,
                        List.of(new ZonaFCRequest(null, "", null, 0, false))
                ),
                List.of(new CondicaoSaudeRequest("", null, true)),
                List.of(new DispositivoExternoRequest(null, "", LocalDateTime.now().minusDays(1)))
        );

        Set<String> violationPaths = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(violationPaths).contains(
                "nome",
                "email",
                "senha",
                "dadosFisicos.pesoKg",
                "dadosFisicos.alturaCm",
                "dadosFisicos.dataNascimento",
                "dadosFisicos.genero",
                "dadosFisicos.fcRepouso",
                "dadosFisicos.fcMaxima",
                "dadosFisicos.horasSonoMedia",
                "perfilAtleta.nivelCondicionamento",
                "perfilAtleta.objetivo",
                "perfilAtleta.jaCorre",
                "perfilAtleta.diasDisponiveisSemana",
                "perfilAtleta.diasSemanaTreino",
                "perfilCorrida.pace5kMinKm",
                "perfilCorrida.vo2Estimado",
                "perfilCorrida.zonasFc[0].zona",
                "perfilCorrida.zonasFc[0].nome",
                "perfilCorrida.zonasFc[0].fcMin",
                "perfilCorrida.zonasFc[0].fcMax",
                "condicoesSaude[0].tipo",
                "dispositivos[0].plataforma",
                "dispositivos[0].tokenAcesso",
                "dispositivos[0].tokenExpiresAt"
        );
    }

    @Test
    void recusaOnboardingComColecoesAninhadasInvalidas() {
        OnboardingRequest request = new OnboardingRequest(
                null,
                null,
                null,
                new PerfilCorridaRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(new ZonaFCRequest(6, "", -1, -2, false))
                ),
                List.of(new CondicaoSaudeRequest("", "asma", true)),
                List.of(new DispositivoExternoRequest(
                        PlataformaRelogio.GARMIN,
                        "",
                        LocalDateTime.now().minusHours(2)
                ))
        );

        Set<String> violationPaths = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(violationPaths).contains(
                "perfilCorrida.zonasFc[0].zona",
                "perfilCorrida.zonasFc[0].nome",
                "perfilCorrida.zonasFc[0].fcMin",
                "perfilCorrida.zonasFc[0].fcMax",
                "condicoesSaude[0].tipo",
                "dispositivos[0].tokenAcesso",
                "dispositivos[0].tokenExpiresAt"
        );
    }

    private UsuarioCadastroRequest cadastroValido() {
        return new UsuarioCadastroRequest(
                "Ana Runner",
                "ana@email.com",
                "senha123",
                null,
                new DadosFisicosRequest(
                        new BigDecimal("62.50"),
                        new BigDecimal("168.00"),
                        LocalDate.of(1995, 5, 10),
                        "FEMININO",
                        58,
                        190,
                        7,
                        false
                ),
                new PerfilAtletaRequest(
                        NivelCondicionamento.INTERMEDIARIO,
                        Objetivo.COMPLETAR_10K,
                        true,
                        4,
                        "SEG,QUA,SEX,SAB"
                ),
                new PerfilCorridaRequest(
                        new BigDecimal("5.20"),
                        new BigDecimal("5.40"),
                        null,
                        null,
                        44,
                        List.of(new ZonaFCRequest(2, "Base", 120, 140, false))
                ),
                List.of(new CondicaoSaudeRequest("ALERGIA", "Rinite", true)),
                List.of(new DispositivoExternoRequest(
                        PlataformaRelogio.GARMIN,
                        "token-valido",
                        LocalDateTime.now().plusDays(2)
                ))
        );
    }
}
