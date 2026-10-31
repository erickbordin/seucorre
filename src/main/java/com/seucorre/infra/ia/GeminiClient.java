package com.seucorre.infra.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seucorre.shared.exception.IAUnavailableException;
import com.seucorre.treino.domain.IAClient;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "seucorre.ia.provider", havingValue = "gemini")
public class GeminiClient implements IAClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${seucorre.ia.gemini.api-key:}")
    private String apiKey;

    @Value("${seucorre.ia.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    @Value("${seucorre.ia.gemini.model:gemini-3.6-flash}")
    private String model;

    @Value("${seucorre.ia.gemini.max-output-tokens:8192}")
    private Integer maxOutputTokens;

    @Value("${seucorre.ia.gemini.timeout-seconds:60}")
    private Long timeoutSeconds;

    @Value("${seucorre.ia.gemini.max-retries:3}")
    private Integer maxRetries;

    @Value("${seucorre.ia.gemini.initial-backoff-millis:500}")
    private Long initialBackoffMillis;

    public GeminiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String gerarResposta(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt é obrigatório.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IAUnavailableException("Chave do Gemini não configurada.");
        }

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(maxRetries)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(initialBackoffMillis, 2.0d))
                .retryOnException(GeminiClient::falhaTemporaria)
                .build();
        try {
            return Retry.decorateCallable(Retry.of("geminiClient", retryConfig), () -> chamarGemini(prompt)).call();
        } catch (IAUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IAUnavailableException("Falha ao consultar o Gemini.", exception);
        }
    }

    private String chamarGemini(String prompt) throws IOException {
        String payload = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("maxOutputTokens", maxOutputTokens)
        ));

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + model + ":generateContent"))
                .header("content-type", "application/json")
                .header("x-goog-api-key", apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IAUnavailableException("Chamada ao Gemini foi interrompida.", exception);
        }

        if (response.statusCode() == 429 || response.statusCode() >= 500) {
            throw new FalhaTemporaria("Gemini retornou status " + response.statusCode() + ": " + extrairMensagemErro(response.body()));
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IAUnavailableException("Gemini retornou status " + response.statusCode() + ": " + extrairMensagemErro(response.body()));
        }

        return extrairTexto(objectMapper.readValue(response.body(), GeminiResponse.class));
    }

    private String extrairTexto(GeminiResponse resposta) {
        if (resposta.promptFeedback() != null && resposta.promptFeedback().blockReason() != null) {
            throw new IAUnavailableException("Gemini bloqueou o pedido: " + resposta.promptFeedback().blockReason());
        }
        if (resposta.candidates() == null || resposta.candidates().isEmpty()) {
            throw new IAUnavailableException("Gemini retornou uma resposta vazia.");
        }

        GeminiCandidate candidato = resposta.candidates().get(0);
        if ("MAX_TOKENS".equals(candidato.finishReason())) {
            throw new IAUnavailableException("Resposta do Gemini cortada pelo limite de tokens.");
        }

        String texto = candidato.content() == null || candidato.content().parts() == null
                ? ""
                : candidato.content().parts().stream()
                .filter(parte -> !Boolean.TRUE.equals(parte.thought()))
                .map(GeminiPart::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining())
                .trim();
        if (texto.isBlank()) {
            throw new IAUnavailableException("Gemini retornou uma resposta vazia (" + candidato.finishReason() + ").");
        }
        return texto;
    }

    private String extrairMensagemErro(String corpo) {
        try {
            GeminiErrorEnvelope envelope = objectMapper.readValue(corpo, GeminiErrorEnvelope.class);
            if (envelope.error() != null && envelope.error().message() != null) {
                return envelope.error().message();
            }
        } catch (Exception ignored) {
            return corpo;
        }
        return corpo;
    }

    private static boolean falhaTemporaria(Throwable erro) {
        for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
            if (atual instanceof JsonProcessingException) {
                return false;
            }
            if (atual instanceof FalhaTemporaria || atual instanceof IOException) {
                return true;
            }
        }
        return false;
    }

    private static final class FalhaTemporaria extends IAUnavailableException {

        private FalhaTemporaria(String message) {
            super(message);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiResponse(List<GeminiCandidate> candidates, GeminiPromptFeedback promptFeedback) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiCandidate(GeminiContent content, String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiContent(List<GeminiPart> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiPart(String text, Boolean thought) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiPromptFeedback(String blockReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiErrorEnvelope(GeminiError error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiError(String message) {
    }
}
