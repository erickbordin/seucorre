package com.seucorre.infra.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seucorre.shared.exception.IAUnavailableException;
import com.seucorre.treino.domain.IAClient;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Service
@ConditionalOnProperty(name = "seucorre.ia.provider", havingValue = "anthropic")
public class AnthropicClient implements IAClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService executorService;

    @Value("${spring.ai.anthropic.api-key}")
    private String apiKey;

    @Value("${seucorre.ia.anthropic.base-url:https://api.anthropic.com/v1/messages}")
    private String baseUrl;

    @Value("${seucorre.ia.anthropic.model:claude-sonnet-4}")
    private String model;

    @Value("${seucorre.ia.anthropic.max-tokens:8192}")
    private Integer maxTokens;

    @Value("${seucorre.ia.anthropic.timeout-seconds:30}")
    private Long timeoutSeconds;

    @Value("${seucorre.ia.anthropic.max-retries:3}")
    private Integer maxRetries;

    @Value("${seucorre.ia.anthropic.initial-backoff-millis:500}")
    private Long initialBackoffMillis;

    public AnthropicClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    @Override
    public String gerarResposta(String prompt) {
        validarPrompt(prompt);

        Callable<String> chamadaComRetry = Retry.decorateCallable(criarRetry(), () -> executarComTimeout(prompt));
        try {
            return chamadaComRetry.call();
        } catch (IAUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IAUnavailableException("Falha ao consultar a Anthropic.", exception);
        }
    }

    private String executarComTimeout(String prompt) {
        try {
            return criarTimeLimiter().executeFutureSupplier(() -> executorService.submit(() -> chamarAnthropic(prompt)));
        } catch (Exception exception) {
            throw new IAUnavailableException("Falha ao consultar a Anthropic dentro do tempo limite.", extrairCausa(exception));
        }
    }

    private String chamarAnthropic(String prompt) {
        try {
            String payload = objectMapper.writeValueAsString(new AnthropicRequest(
                    model,
                    maxTokens,
                    List.of(new AnthropicMessage("user", prompt))
            ));

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IAUnavailableException("Anthropic retornou status " + response.statusCode()
                        + ": " + extrairMensagemErro(response.body()));
            }

            AnthropicResponse anthropicResponse = objectMapper.readValue(response.body(), AnthropicResponse.class);
            String texto = extrairTexto(anthropicResponse);
            if (texto.isBlank()) {
                throw new IAUnavailableException("Anthropic retornou uma resposta vazia.");
            }
            return texto;
        } catch (JsonProcessingException exception) {
            throw new IAUnavailableException("Falha ao serializar ou desserializar a comunicação com a Anthropic.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IAUnavailableException("Chamada à Anthropic foi interrompida.", exception);
        } catch (IOException exception) {
            throw new IAUnavailableException("Erro de I/O ao chamar a Anthropic.", exception);
        }
    }

    private Retry criarRetry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(maxRetries)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(initialBackoffMillis, 2.0d))
                .retryExceptions(IAUnavailableException.class, TimeoutException.class)
                .build();
        return Retry.of("anthropicClient", retryConfig);
    }

    private TimeLimiter criarTimeLimiter() {
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(timeoutSeconds))
                .cancelRunningFuture(true)
                .build();
        return TimeLimiter.of(timeLimiterConfig);
    }

    private void validarPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt é obrigatório.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IAUnavailableException("Chave da Anthropic não configurada.");
        }
    }

    private String extrairTexto(AnthropicResponse response) {
        if (response == null || response.content() == null) {
            return "";
        }
        return response.content().stream()
                .filter(content -> "text".equals(content.type()) && content.text() != null)
                .map(AnthropicContent::text)
                .reduce("", (texto1, texto2) -> texto1.isBlank() ? texto2 : texto1 + "\n" + texto2)
                .trim();
    }

    private String extrairMensagemErro(String responseBody) {
        try {
            AnthropicErrorEnvelope errorEnvelope = objectMapper.readValue(responseBody, AnthropicErrorEnvelope.class);
            if (errorEnvelope.error() != null && errorEnvelope.error().message() != null) {
                return errorEnvelope.error().message();
            }
        } catch (Exception ignored) {
            return responseBody;
        }
        return responseBody;
    }

    private Throwable extrairCausa(Exception exception) {
        if (exception.getCause() != null) {
            return exception.getCause();
        }
        return exception;
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdownNow();
    }

    private record AnthropicRequest(
            String model,
            @JsonProperty("max_tokens") Integer maxTokens,
            List<AnthropicMessage> messages
    ) {
    }

    private record AnthropicMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicResponse(List<AnthropicContent> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicContent(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicErrorEnvelope(AnthropicError error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicError(String type, String message) {
    }
}
