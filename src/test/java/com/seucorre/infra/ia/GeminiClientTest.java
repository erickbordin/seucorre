package com.seucorre.infra.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seucorre.shared.exception.IAUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiClientTest {

    private final AtomicInteger chamadas = new AtomicInteger();
    private final AtomicReference<String> chaveRecebida = new AtomicReference<>();
    private HttpServer servidor;
    private GeminiClient client;

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    @Test
    void devolveSoOTextoDaRespostaIgnorandoOPensamento() throws IOException {
        iniciar(200, """
                {"candidates": [{"finishReason": "STOP", "content": {"parts": [
                  {"text": "rascunho interno", "thought": true},
                  {"text": "{\\"sessoes\\": []}"}
                ]}}]}
                """);

        String resposta = client.gerarResposta("plano");

        assertThat(resposta).isEqualTo("{\"sessoes\": []}");
        assertThat(chaveRecebida.get()).isEqualTo("chave-de-teste");
    }

    @Test
    void respostaCortadaPeloLimiteDeTokensFalhaSemRepetir() throws IOException {
        iniciar(200, """
                {"candidates": [{"finishReason": "MAX_TOKENS", "content": {"parts": [{"text": "{\\"sessoes\\": ["}]}}]}
                """);

        assertThatThrownBy(() -> client.gerarResposta("plano"))
                .isInstanceOf(IAUnavailableException.class)
                .hasMessage("Resposta do Gemini cortada pelo limite de tokens.");
        assertThat(chamadas.get()).isEqualTo(1);
    }

    @Test
    void erroDoClienteNaoRepeteEServidorSobrecarregadoRepete() throws IOException {
        iniciar(400, "{\"error\": {\"message\": \"pedido invalido\"}}");
        assertThatThrownBy(() -> client.gerarResposta("plano")).isInstanceOf(IAUnavailableException.class);
        assertThat(chamadas.get()).isEqualTo(1);
        servidor.stop(0);

        chamadas.set(0);
        iniciar(503, "{\"error\": {\"message\": \"sobrecarregado\"}}");
        assertThatThrownBy(() -> client.gerarResposta("plano")).isInstanceOf(IAUnavailableException.class);
        assertThat(chamadas.get()).isEqualTo(3);
    }

    private void iniciar(int status, String corpo) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/v1beta/models/gemini-teste:generateContent", exchange -> {
            chamadas.incrementAndGet();
            chaveRecebida.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        servidor.start();

        client = new GeminiClient(new ObjectMapper());
        ReflectionTestUtils.setField(client, "apiKey", "chave-de-teste");
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:" + servidor.getAddress().getPort() + "/v1beta/models");
        ReflectionTestUtils.setField(client, "model", "gemini-teste");
        ReflectionTestUtils.setField(client, "maxOutputTokens", 100);
        ReflectionTestUtils.setField(client, "timeoutSeconds", 5L);
        ReflectionTestUtils.setField(client, "maxRetries", 3);
        ReflectionTestUtils.setField(client, "initialBackoffMillis", 1L);
    }
}
