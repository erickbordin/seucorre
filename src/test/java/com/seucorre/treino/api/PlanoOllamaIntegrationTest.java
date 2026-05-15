package com.seucorre.treino.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.shared.domain.enums.StatusPlano;
import com.seucorre.treino.application.dto.GerarPlanoRequest;
import com.seucorre.treino.application.dto.PlanoTreinoDTO;
import com.seucorre.treino.infrastructure.PlanoRepository;
import com.seucorre.usuario.application.dto.CondicaoSaudeRequest;
import com.seucorre.usuario.application.dto.DadosFisicosRequest;
import com.seucorre.usuario.application.dto.DispositivoExternoRequest;
import com.seucorre.usuario.application.dto.LoginRequest;
import com.seucorre.usuario.application.dto.LoginResponse;
import com.seucorre.usuario.application.dto.PerfilAtletaRequest;
import com.seucorre.usuario.application.dto.PerfilCorridaRequest;
import com.seucorre.usuario.application.dto.UsuarioCadastroRequest;
import com.seucorre.usuario.application.dto.ZonaFCRequest;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.AfterAll;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PlanoOllamaIntegrationTest {

    private static final ObjectMapper SERVER_OBJECT_MAPPER = new ObjectMapper();
    private static final ConcurrentLinkedQueue<String> OLLAMA_BODIES = new ConcurrentLinkedQueue<>();
    private static final HttpServer OLLAMA_SERVER = iniciarOllamaServer();

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
        registry.add("seucorre.ia.provider", () -> "ollama");
        registry.add(
                "seucorre.ia.ollama.base-url",
                () -> "http://localhost:" + OLLAMA_SERVER.getAddress().getPort() + "/api/generate"
        );
        registry.add("seucorre.ia.ollama.model", () -> "llama3.2");
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
    private PlanoRepository planoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparEstado() {
        OLLAMA_BODIES.clear();
        BucketProxy bucket = mock(BucketProxy.class);
        when(rateLimiterProxyManager.getProxy(anyString(), any())).thenReturn(bucket);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.consumed(4L, 0L));
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    checkin_semanal,
                    progresso_semanal,
                    registro_treino,
                    treino,
                    plano_treino,
                    zona_fc,
                    perfil_corrida,
                    condicao_saude,
                    relogio,
                    usuario
                RESTART IDENTITY CASCADE
                """);
    }

    @AfterAll
    static void encerrarServidorOllama() {
        OLLAMA_SERVER.stop(0);
    }

    @Test
    void geraPlanoComFluxoRealPersistindoPlanoESessoesViaOllama() throws Exception {
        UsuarioCadastroRequest cadastro = cadastroValido("plano.ollama@seucorre.dev", "SEG,QUA,SEX");

        mockMvc.perform(post("/api/usuarios/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cadastro)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());

        Usuario usuario = usuarioRepository.findByEmail(cadastro.email()).orElseThrow();
        String token = autenticar(cadastro.email(), cadastro.senha());

        MvcResult gerarPlanoResult = mockMvc.perform(post("/api/planos/gerar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new GerarPlanoRequest(Objetivo.COMPLETAR_10K))))
                .andReturn();

        assertThat(gerarPlanoResult.getResponse().getStatus()).isEqualTo(201);

        PlanoTreinoDTO planoGerado = fromJson(gerarPlanoResult, new TypeReference<>() {
        });

        assertThat(planoGerado.id()).isNotNull();
        assertThat(planoGerado.totalSemanas()).isEqualTo(2);
        assertThat(planoGerado.dataInicio()).isEqualTo(LocalDate.of(2099, 2, 2));
        assertThat(planoGerado.dataFim()).isEqualTo(LocalDate.of(2099, 2, 11));
        assertThat(planoGerado.kmTotais()).isEqualByComparingTo("19.50");
        assertThat(planoGerado.status()).isEqualTo(StatusPlano.ATIVO);
        assertThat(planoGerado.resumoIA()).isEqualTo("Plano gerado via Llama com progressao controlada.");
        assertThat(planoGerado.guiaTiposTreino()).isEqualTo("Regenerativo, intervalado e longo.");
        assertThat(planoGerado.sessoes()).hasSize(4);
        assertThat(planoGerado.sessoes())
                .extracting(sessao -> sessao.tipo().name())
                .containsExactly("REGENERATIVO", "INTERVALADO", "REGENERATIVO", "LONGO");

        assertThat(planoRepository.findByUsuarioId(usuario.getId())).hasSize(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from plano_treino where usuario_id = ?", Integer.class, usuario.getId()))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from treino where plano_id = ?", Integer.class, planoGerado.id()))
                .isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("select km_totais from plano_treino where id = ?", BigDecimal.class, planoGerado.id()))
                .isEqualByComparingTo("19.50");

        String ollamaBody = OLLAMA_BODIES.poll();
        assertThat(ollamaBody).isNotBlank();
        assertThat(OLLAMA_BODIES).isEmpty();

        JsonNode ollamaRequest = objectMapper.readTree(ollamaBody);
        String prompt = ollamaRequest.path("prompt").asText();
        assertThat(ollamaRequest.path("model").asText()).isEqualTo("llama3.2");
        assertThat(ollamaRequest.path("format").asText()).isEqualTo("json");
        assertThat(ollamaRequest.path("stream").asBoolean()).isFalse();
        assertThat(prompt).contains("Ana Runner");
        assertThat(prompt).contains("Completar 10km");
        assertThat(prompt).contains("SEG,QUA,SEX");
        assertThat(prompt).contains("Apto para treinar: true");
    }

    private static HttpServer iniciarOllamaServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/generate", new OllamaStubHandler());
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao iniciar o stub HTTP do Ollama para o teste.", exception);
        }
    }

    private String autenticar(String email, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, senha))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();

        LoginResponse response = fromJson(result, new TypeReference<>() {
        });
        assertThat(response.token()).isNotBlank();
        return response.token();
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

    private static final class OllamaStubHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (InputStream requestBody = exchange.getRequestBody()) {
                OLLAMA_BODIES.add(new String(requestBody.readAllBytes(), StandardCharsets.UTF_8));
            }

            byte[] response = SERVER_OBJECT_MAPPER.writeValueAsBytes(Map.of(
                    "response", """
                            {
                              "plano": {
                                "totalSemanas": 2,
                                "dataInicio": "2099-02-02",
                                "dataFim": "2099-02-11",
                                "kmTotais": 19.50,
                                "resumoIA": "Plano gerado via Llama com progressao controlada.",
                                "guiaTiposTreino": "Regenerativo, intervalado e longo.",
                                "semanas": [
                                  {
                                    "numeroSemana": 1,
                                    "sessoes": [
                                      {
                                        "dataPrevista": "2099-02-02",
                                        "tipo": "REGENERATIVO",
                                        "distanciaKm": 4.50,
                                        "duracaoMinutos": 32,
                                        "intensidade": "leve",
                                        "zonaFcAlvo": "Z1",
                                        "paceAlvo": 6.25,
                                        "descricao": "Rodagem leve"
                                      },
                                      {
                                        "dataPrevista": "2099-02-04",
                                        "tipo": "INTERVALADO",
                                        "distanciaKm": 5.50,
                                        "duracaoMinutos": 38,
                                        "intensidade": "moderada",
                                        "zonaFcAlvo": "Z3",
                                        "paceAlvo": 5.35,
                                        "descricao": "Tiros curtos"
                                      }
                                    ]
                                  },
                                  {
                                    "numeroSemana": 2,
                                    "sessoes": [
                                      {
                                        "dataPrevista": "2099-02-09",
                                        "tipo": "REGENERATIVO",
                                        "distanciaKm": 4.50,
                                        "duracaoMinutos": 34,
                                        "intensidade": "leve",
                                        "zonaFcAlvo": "Z1",
                                        "paceAlvo": 6.15,
                                        "descricao": "Rodagem de recuperacao"
                                      },
                                      {
                                        "dataPrevista": "2099-02-11",
                                        "tipo": "LONGO",
                                        "distanciaKm": 5.00,
                                        "duracaoMinutos": 39,
                                        "intensidade": "moderada",
                                        "zonaFcAlvo": "Z2",
                                        "paceAlvo": 5.58,
                                        "descricao": "Longo progressivo"
                                      }
                                    ]
                                  }
                                ]
                              }
                            }
                            """
            ));

            exchange.getResponseHeaders().add("content-type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(response);
            }
        }
    }
}
