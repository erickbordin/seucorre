package com.seucorre.infra.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Service
@ConditionalOnProperty(name = "seucorre.ia.provider", havingValue = "ollama")
public class OllamaClient implements IAClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService executorService;

    @Value("${seucorre.ia.ollama.base-url:http://localhost:11434/api/generate}")
    private String baseUrl;

    @Value("${seucorre.ia.ollama.model:llama3.2}")
    private String model;

    @Value("${seucorre.ia.ollama.timeout-seconds:60}")
    private Long timeoutSeconds;

    @Value("${seucorre.ia.ollama.max-retries:3}")
    private Integer maxRetries;

    @Value("${seucorre.ia.ollama.initial-backoff-millis:500}")
    private Long initialBackoffMillis;

    @Value("${seucorre.ia.ollama.temperature:0.0}")
    private Double temperature;

    @Value("${seucorre.ia.ollama.num-predict:2600}")
    private Integer numPredict;

    @Value("${seucorre.ia.ollama.keep-alive:15m}")
    private String keepAlive;

    public OllamaClient(ObjectMapper objectMapper) {
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
            throw new IAUnavailableException("Falha ao consultar o Ollama.", exception);
        }
    }

    private String executarComTimeout(String prompt) {
        try {
            return criarTimeLimiter().executeFutureSupplier(() -> executorService.submit(() -> chamarOllama(prompt)));
        } catch (Exception exception) {
            throw new IAUnavailableException("Falha ao consultar o Ollama dentro do tempo limite.", extrairCausa(exception));
        }
    }

    private String chamarOllama(String prompt) {
        try {
            String payload = objectMapper.writeValueAsString(new OllamaRequest(
                    model,
                    prompt,
                    false,
                    false,
                    keepAlive,
                    "json",
                    new OllamaOptions(temperature, numPredict)
            ));

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                    .header("content-type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IAUnavailableException("Ollama retornou status " + response.statusCode()
                        + ": " + extrairMensagemErro(response.body()));
            }

            OllamaResponse ollamaResponse = objectMapper.readValue(response.body(), OllamaResponse.class);
            String texto = extrairTexto(ollamaResponse);
            if (texto.isBlank()) {
                throw new IAUnavailableException("Ollama retornou uma resposta vazia.");
            }
            return texto;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IAUnavailableException("Chamada ao Ollama foi interrompida.", exception);
        } catch (IOException exception) {
            throw new IAUnavailableException("Erro de I/O ao chamar o Ollama.", exception);
        }
    }

    private Retry criarRetry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(maxRetries)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(initialBackoffMillis, 2.0d))
                .retryExceptions(IAUnavailableException.class, TimeoutException.class)
                .build();
        return Retry.of("ollamaClient", retryConfig);
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
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IAUnavailableException("URL do Ollama não configurada.");
        }
        if (model == null || model.isBlank()) {
            throw new IAUnavailableException("Modelo do Ollama não configurado.");
        }
    }

    private String extrairTexto(OllamaResponse response) {
        if (response == null || response.response() == null) {
            return "";
        }
        return response.response().trim();
    }

    private String extrairMensagemErro(String responseBody) {
        try {
            OllamaErrorEnvelope errorEnvelope = objectMapper.readValue(responseBody, OllamaErrorEnvelope.class);
            if (errorEnvelope.error() != null && !errorEnvelope.error().isBlank()) {
                return errorEnvelope.error();
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

    private record OllamaRequest(
            String model,
            String prompt,
            boolean stream,
            boolean think,
            String keep_alive,
            String format,
            OllamaOptions options
    ) {
    }

    private record OllamaOptions(Double temperature, Integer num_predict) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaResponse(String response) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaErrorEnvelope(String error) {
    }
}
