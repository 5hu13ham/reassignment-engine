package com.zycus.ai;

import com.zycus.exception.LlmGatewayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GeminiLlmGateway implements LLMGateway {

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String generateContentUrl;

    public GeminiLlmGateway(
            JsonMapper jsonMapper,
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.model}") String model,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.timeout-ms:30000}") long timeoutMs
    ) {
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
        this.generateContentUrl = "%s/v1beta/models/%s:generateContent".formatted(
                trimTrailingSlash(baseUrl),
                model
        );
        this.restClient = RestClient.builder()
                .requestFactory(createRequestFactory(timeoutMs))
                .build();
    }

    @Override
    public String generate(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.2
                )
        );

        try {
            String responseBody = restClient.post()
                    .uri(generateContentUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return extractGeneratedText(responseBody);
        } catch (ResourceAccessException exception) {
            throw new LlmGatewayException("LLM request timed out or failed to connect", exception);
        } catch (RestClientResponseException exception) {
            throw new LlmGatewayException(
                    "LLM request failed with status " + exception.getStatusCode().value(),
                    exception
            );
        } catch (LlmGatewayException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LlmGatewayException("Unexpected LLM gateway failure", exception);
        }
    }

    private String extractGeneratedText(String responseBody) {
        try {
            JsonNode root = jsonMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new LlmGatewayException("LLM response did not contain generated text");
            }

            return textNode.asText();
        } catch (LlmGatewayException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LlmGatewayException("Failed to read LLM response payload", exception);
        }
    }

    private org.springframework.http.client.SimpleClientHttpRequestFactory createRequestFactory(long timeoutMs) {
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return requestFactory;
    }

    private String trimTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
