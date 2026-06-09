package com.seucorre.usuario.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.usuario.application.dto.CondicaoSaudeRequest;
import com.seucorre.usuario.application.dto.DadosFisicosRequest;
import com.seucorre.usuario.application.dto.DispositivoExternoRequest;
import com.seucorre.usuario.application.dto.LoginRequest;
import com.seucorre.usuario.application.dto.LoginResponse;
import com.seucorre.usuario.application.dto.OnboardingRequest;
import com.seucorre.usuario.application.dto.PerfilAtletaRequest;
import com.seucorre.usuario.application.dto.PerfilCorridaRequest;
import com.seucorre.usuario.application.dto.UsuarioCadastroRequest;
import com.seucorre.usuario.application.dto.UsuarioResponse;
import com.seucorre.usuario.application.dto.ZonaFCRequest;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class UsuarioIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("seucorre_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:postgresql://host.docker.internal:" + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName()
        );
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add(
                "spring.flyway.url",
                () -> "jdbc:postgresql://host.docker.internal:" + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName()
        );
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @MockBean(name = "rateLimiterRedisClient")
    private RedisClient rateLimiterRedisClient;

    @MockBean(name = "rateLimiterRedisConnection")
    private StatefulRedisConnection<String, byte[]> rateLimiterRedisConnection;

    @MockBean(name = "rateLimiterProxyManager")
    private LettuceBasedProxyManager<String> rateLimiterProxyManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparBanco() {
        jdbcTemplate.execute("TRUNCATE TABLE zona_fc, perfil_corrida, condicao_saude, relogio, usuario RESTART IDENTITY CASCADE");
    }

    @Test
    void cadastroLoginEConsultaPerfilFuncionamComPostgresReal() throws Exception {
        UsuarioCadastroRequest cadastro = cadastroValido("ana.integration@seucorre.dev", "SEG,QUA,SEX");

        MvcResult cadastroResult = mockMvc.perform(post("/api/usuarios/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cadastro)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Ana Runner"))
                .andExpect(jsonPath("$.email").value("ana.integration@seucorre.dev"))
                .andExpect(jsonPath("$.aptoParaTreinar").value(true))
                .andReturn();

        UsuarioResponse cadastroResponse = fromJson(cadastroResult, new TypeReference<>() {
        });
        Usuario usuarioPersistido = usuarioRepository.findByEmail(cadastro.email()).orElseThrow();

        assertThat(usuarioPersistido.getId()).isEqualTo(cadastroResponse.id());
        assertThat(usuarioPersistido.getSenhaHash()).isNotBlank().isNotEqualTo(cadastro.senha());
        assertThat(usuarioRepository.findPerfilCorridaByUsuarioId(usuarioPersistido.getId())).isPresent();

        String token = autenticar(cadastro.email(), cadastro.senha());

        MvcResult perfilResult = mockMvc.perform(get("/api/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cadastroResponse.id().toString()))
                .andExpect(jsonPath("$.perfilCorrida.vo2Estimado").value(38))
                .andExpect(jsonPath("$.condicoesSaude[0].tipo").value("ALERGIA"))
                .andExpect(jsonPath("$.dispositivos[0].plataforma").value("GARMIN"))
                .andReturn();

        UsuarioResponse perfilResponse = fromJson(perfilResult, new TypeReference<>() {
        });
        assertThat(perfilResponse.perfilCorrida()).isNotNull();
        assertThat(perfilResponse.perfilCorrida().zonasFc()).hasSize(1);
        assertThat(perfilResponse.fcRepouso()).isEqualTo(62);
        assertThat(perfilResponse.diasSemanaTreino()).isEqualTo("SEG,QUA,SEX");
    }

    @Test
    void onboardingAutenticadoAtualizaPerfilEPersistenciaRelacionada() throws Exception {
        UsuarioCadastroRequest cadastro = cadastroValido("bia.integration@seucorre.dev", "SEG,QUI");

        mockMvc.perform(post("/api/usuarios/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cadastro)))
                .andExpect(status().isCreated());

        String token = autenticar(cadastro.email(), cadastro.senha());
        OnboardingRequest onboarding = onboardingAtualizado();

        MvcResult onboardingResult = mockMvc.perform(put("/api/usuarios/me/onboarding")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(onboarding)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefone").doesNotExist())
                .andExpect(jsonPath("$.nivelCondicionamento").value("INTERMEDIARIO"))
                .andExpect(jsonPath("$.objetivo").value("COMPLETAR_10K"))
                .andExpect(jsonPath("$.perfilCorrida.vo2Estimado").value(44))
                .andReturn();

        UsuarioResponse onboardingResponse = fromJson(onboardingResult, new TypeReference<>() {
        });
        assertThat(onboardingResponse.pesoKg()).isEqualByComparingTo("60.10");
        assertThat(onboardingResponse.diasDisponiveisSemana()).isEqualTo(4);
        assertThat(onboardingResponse.diasSemanaTreino()).isEqualTo("TER,QUI,SAB");
        assertThat(onboardingResponse.condicoesSaude()).hasSize(2);
        assertThat(onboardingResponse.dispositivos()).hasSize(1);

        Usuario usuarioPersistido = usuarioRepository.findByEmail(cadastro.email()).orElseThrow();
        assertThat(usuarioPersistido.getPesoKg()).isEqualByComparingTo("60.10");
        assertThat(usuarioPersistido.getDiasSemanaTreino()).isEqualTo("TER,QUI,SAB");
        assertThat(usuarioRepository.findPerfilCorridaByUsuarioId(usuarioPersistido.getId()))
                .isPresent()
                .get()
                .extracting(perfil -> perfil.getVo2Estimado())
                .isEqualTo(44);
        assertThat(usuarioRepository.findCondicoesSaudeByUsuarioId(usuarioPersistido.getId())).hasSize(2);
        assertThat(usuarioRepository.findDispositivosByUsuarioId(usuarioPersistido.getId())).hasSize(1);
    }

    @Test
    void cadastroBasicoPermiteLoginEOnboardingPosterior() throws Exception {
        UsuarioCadastroRequest cadastro = new UsuarioCadastroRequest(
                "Conta Básica",
                "basico.integration@seucorre.dev",
                "senha123",
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/usuarios/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cadastro)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Conta Básica"))
                .andExpect(jsonPath("$.aptoParaTreinar").value(false));

        String token = autenticar(cadastro.email(), cadastro.senha());

        mockMvc.perform(get("/api/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(cadastro.email()))
                .andExpect(jsonPath("$.objetivo").doesNotExist())
                .andExpect(jsonPath("$.aptoParaTreinar").value(false));

        mockMvc.perform(put("/api/usuarios/me/onboarding")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(onboardingAtualizado())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objetivo").value("COMPLETAR_10K"))
                .andExpect(jsonPath("$.aptoParaTreinar").value(true));
    }

    @Test
    void cicloJwtValidaLoginAcessoNegacaoERefreshToken() throws Exception {
        UsuarioCadastroRequest cadastro = cadastroValido("jwt.integration@seucorre.dev", "SEG,QUA,SEX");

        mockMvc.perform(post("/api/usuarios/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cadastro)))
                .andExpect(status().isCreated());

        LoginResponse loginResponse = autenticarCompleto(cadastro.email(), cadastro.senha());

        mockMvc.perform(get("/api/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginResponse.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(cadastro.email()));

        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer("token-invalido")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginResponse.refreshToken())))
                .andExpect(status().isUnauthorized());

        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginResponse.refreshToken())))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse refreshResponse = fromJson(refreshResult, new TypeReference<>() {
        });
        assertThat(refreshResponse.token()).isNotBlank();
        assertThat(refreshResponse.refreshToken()).isNotBlank();
        assertThat(refreshResponse.token()).isNotEqualTo(loginResponse.token());

        mockMvc.perform(post("/auth/refresh")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginResponse.token())))
                .andExpect(status().isUnauthorized());
    }

    private String autenticar(String email, String senha) throws Exception {
        return autenticarCompleto(email, senha).token();
    }

    private LoginResponse autenticarCompleto(String email, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, senha))))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse response = fromJson(result, new TypeReference<>() {
        });
        assertThat(response.token()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        return response;
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private <T> T fromJson(MvcResult result, TypeReference<T> typeReference) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), typeReference);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private UsuarioCadastroRequest cadastroValido(String email, String diasSemanaTreino) {
        return new UsuarioCadastroRequest(
                "Ana Runner",
                email,
                "senha123",
                "11999999999",
                new DadosFisicosRequest(
                        new BigDecimal("62.50"),
                        new BigDecimal("168.00"),
                        LocalDate.of(1995, 5, 10),
                        "FEMININO",
                        62,
                        190,
                        7,
                        false
                ),
                new PerfilAtletaRequest(
                        NivelCondicionamento.INICIANTE,
                        Objetivo.COMPLETAR_5K,
                        true,
                        3,
                        diasSemanaTreino
                ),
                new PerfilCorridaRequest(
                        new BigDecimal("6.20"),
                        null,
                        null,
                        null,
                        38,
                        List.of(new ZonaFCRequest(1, "Leve", 95, 115, false))
                ),
                List.of(new CondicaoSaudeRequest("ALERGIA", "Rinite", true)),
                List.of(new DispositivoExternoRequest(
                        PlataformaRelogio.GARMIN,
                        "token-garmin",
                        LocalDateTime.now().plusDays(1)
                ))
        );
    }

    private OnboardingRequest onboardingAtualizado() {
        return new OnboardingRequest(
                "11888887777",
                new DadosFisicosRequest(
                        new BigDecimal("60.10"),
                        new BigDecimal("167.00"),
                        LocalDate.of(1994, 8, 21),
                        "FEMININO",
                        58,
                        188,
                        8,
                        false
                ),
                new PerfilAtletaRequest(
                        NivelCondicionamento.INTERMEDIARIO,
                        Objetivo.COMPLETAR_10K,
                        true,
                        4,
                        "TER,QUI,SAB"
                ),
                new PerfilCorridaRequest(
                        new BigDecimal("5.40"),
                        new BigDecimal("5.55"),
                        null,
                        null,
                        44,
                        List.of(
                                new ZonaFCRequest(1, "Regenerativo", 92, 112, false),
                                new ZonaFCRequest(2, "Base", 113, 132, false)
                        )
                ),
                List.of(
                        new CondicaoSaudeRequest("ALERGIA", "Rinite", true),
                        new CondicaoSaudeRequest("LESAO_ANTERIOR", "Tornozelo direito", false)
                ),
                List.of(new DispositivoExternoRequest(
                        PlataformaRelogio.STRAVA,
                        "token-strava",
                        LocalDateTime.now().plusDays(2)
                ))
        );
    }
}
