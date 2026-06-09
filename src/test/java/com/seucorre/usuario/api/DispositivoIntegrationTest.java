package com.seucorre.usuario.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.usuario.application.dto.DispositivoExternoRequest;
import com.seucorre.usuario.application.dto.LoginRequest;
import com.seucorre.usuario.application.dto.LoginResponse;
import com.seucorre.usuario.application.dto.UsuarioCadastroRequest;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class DispositivoIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparBanco() {
        jdbcTemplate.execute("TRUNCATE TABLE zona_fc, perfil_corrida, condicao_saude, relogio, usuario RESTART IDENTITY CASCADE");
    }

    @Test
    void vinculoListagemESincronizacaoDeDispositivoFuncionamEmFluxoAutenticado() throws Exception {
        UsuarioCadastroRequest cadastro = new UsuarioCadastroRequest(
                "Runner Wearable",
                "wearables.integration@seucorre.dev",
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
                .andExpect(status().isCreated());

        String token = autenticar(cadastro.email(), cadastro.senha());
        DispositivoExternoRequest request = new DispositivoExternoRequest(
                PlataformaRelogio.GARMIN,
                "token-garmin-integracao",
                LocalDateTime.now().plusDays(7).withSecond(0).withNano(0)
        );

        mockMvc.perform(get("/api/dispositivos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/dispositivos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plataforma").value("GARMIN"))
                .andExpect(jsonPath("$.descricaoPlataforma").value("Garmin Connect"))
                .andExpect(jsonPath("$.tokenValido").value(true))
                .andExpect(jsonPath("$.sincronizacaoDisponivel").value(true));

        mockMvc.perform(get("/api/dispositivos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].plataforma").value("GARMIN"));

        mockMvc.perform(post("/api/dispositivos/GARMIN/sincronizar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plataforma").value("GARMIN"))
                .andExpect(jsonPath("$.registrosImportados").value(0))
                .andExpect(jsonPath("$.registros").isArray())
                .andExpect(jsonPath("$.registros").isEmpty());
    }

    private String autenticar(String email, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, senha))))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
        return response.token();
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
