package com.seucorre.shared.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.shared.exception.IAUnavailableException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(handler)
                .setValidator(validator)
                .build();
    }

    @Test
    void handleBusinessRuleExceptionRetornaBadRequestNoPadraoApiResponse() {
        var response = handler.handleBusinessRuleException(new BusinessRuleException("Regra inválida."));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(ApiResponse.erro("Regra inválida."));
    }

    @Test
    void handleEntityNotFoundExceptionRetornaNotFoundNoPadraoApiResponse() {
        var response = handler.handleEntityNotFoundException(new EntityNotFoundException("Plano não encontrado."));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isEqualTo(ApiResponse.erro("Plano não encontrado."));
    }

    @Test
    void handleIAUnavailableExceptionRetornaServiceUnavailableNoPadraoApiResponse() {
        var response = handler.handleIAUnavailableException(new IAUnavailableException("Falha externa."));

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).isEqualTo(ApiResponse.erro("Serviço de IA temporariamente indisponível. Tente novamente mais tarde."));
    }

    @Test
    void handleIllegalArgumentExceptionRetornaBadRequestNoPadraoApiResponse() {
        var response = handler.handleIllegalArgumentException(new IllegalArgumentException("Argumento inválido."));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(ApiResponse.erro("Argumento inválido."));
    }

    @Test
    void handleConstraintViolationExceptionRetornaBadRequestComListaDeErros() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set violations = validator.validate(new ParametroInvalido(0));

        var response = handler.handleConstraintViolationException(new ConstraintViolationException(violations));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensagem()).isEqualTo("Erro de validação nos parâmetros");
        assertThat(response.getBody().erros()).isNotEmpty();
    }

    @Test
    void handleHttpMessageNotReadableExceptionRetornaBadRequestNoPadraoApiResponse() {
        var response = handler.handleHttpMessageNotReadableException(new HttpMessageNotReadableException("JSON inválido"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(ApiResponse.erro("Corpo da requisição inválido ou malformado."));
    }

    @Test
    void handleExceptionRetornaErroInternoSemExporMensagemDaExcecao() {
        var response = handler.handleException(new RuntimeException("stacktrace interno"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isEqualTo(ApiResponse.erro("Erro interno no servidor."));
    }

    @Test
    void mockMvcRetornaErroDeValidacaoParaCorpoInvalido() throws Exception {
        mockMvc.perform(post("/test/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new TestRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sucesso").value(false))
                .andExpect(jsonPath("$.mensagem").value("Erro de validação nos campos"))
                .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    void mockMvcRetornaErroParaJsonMalformado() throws Exception {
        mockMvc.perform(post("/test/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sucesso").value(false))
                .andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido ou malformado."));
    }

    @RestController
    static class TestController {

        @PostMapping("/test/validacao")
        void validar(@RequestBody @Valid TestRequest request) {
        }
    }

    record TestRequest(@NotBlank String nome) {
    }

    record ParametroInvalido(@Min(1) Integer semana) {
    }
}
