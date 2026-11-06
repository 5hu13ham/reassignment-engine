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
    private final RestClient http;
    private final JsonMapper json;
    private final String key;
    private final String url;

    public GeminiLlmGateway(JsonMapper json,@Value("${llm.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${llm.model:gemini-flash-latest}") String model,
            @Value("${llm.api-key:}") String key,
            @Value("${llm.timeout-ms:10000}") long timeoutMs) {
        this.json=json;this.key=key;
        this.url=baseUrl.replaceAll("/+$","")+"/v1beta/models/"+model+":generateContent";
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.http=RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String generate(String prompt) {
        if(key==null || key.isBlank())
            throw new LlmGatewayException("Gemini API key not configured (rule-based fallback is available)");
        try {
            Map<String,Object> body=Map.of(
                    "contents",List.of(Map.of("role","user","parts",List.of(Map.of("text",prompt)))),
                    "generationConfig",Map.of("responseMimeType","application/json","temperature",0.2));
            String response=http.post().uri(url).header("x-goog-api-key",key)
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
            JsonNode text=json.readTree(response).path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text");
            if(text.isMissingNode() || text.asText().isBlank())
                throw new LlmGatewayException("Gemini produced no text");
            return text.asText();
        } catch(ResourceAccessException ex) {
            throw new LlmGatewayException("Gemini timeout or connection failure",ex);
        } catch(RestClientResponseException ex) {
            throw new LlmGatewayException("Gemini returned HTTP "+ex.getStatusCode().value(),ex);
        } catch(LlmGatewayException ex) {
            throw ex;
        } catch(Exception ex) {
            throw new LlmGatewayException("Gemini response could not be parsed",ex);
        }
    }
}
