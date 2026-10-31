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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnthropicClientTest {

    private final AtomicInteger chamadas = new AtomicInteger();
    private HttpServer servidor;
    private AnthropicClient client;

    @AfterEach
    void tearDown() {
        client.shutdown();
        servidor.stop(0);
    }

    @Test
    void erroDoClienteNaoRepeteAChamada() throws IOException {
        iniciar(404);

        assertThatThrownBy(() -> client.gerarResposta("plano")).isInstanceOf(IAUnavailableException.class);

        assertThat(chamadas.get()).isEqualTo(1);
    }

    @Test
    void sobrecargaRepeteAteOLimiteDeTentativas() throws IOException {
        iniciar(529);

        assertThatThrownBy(() -> client.gerarResposta("plano")).isInstanceOf(IAUnavailableException.class);

        assertThat(chamadas.get()).isEqualTo(3);
    }

    private void iniciar(int status) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/v1/messages", exchange -> {
            chamadas.incrementAndGet();
            byte[] corpo = "{\"error\":{\"type\":\"erro\",\"message\":\"falhou\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, corpo.length);
            exchange.getResponseBody().write(corpo);
            exchange.close();
        });
        servidor.start();

        client = new AnthropicClient(new ObjectMapper());
        ReflectionTestUtils.setField(client, "apiKey", "chave-de-teste");
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:" + servidor.getAddress().getPort() + "/v1/messages");
        ReflectionTestUtils.setField(client, "model", "claude-sonnet-5-5");
        ReflectionTestUtils.setField(client, "maxTokens", 100);
        ReflectionTestUtils.setField(client, "timeoutSeconds", 5L);
        ReflectionTestUtils.setField(client, "maxRetries", 3);
        ReflectionTestUtils.setField(client, "initialBackoffMillis", 1L);
    }
}
