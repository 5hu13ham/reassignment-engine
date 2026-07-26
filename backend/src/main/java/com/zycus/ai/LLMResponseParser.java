package com.zycus.ai;

import com.zycus.exception.LlmResponseParseException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.regex.Pattern;

@Component
public class LLMResponseParser {

    private static final Pattern MARKDOWN_JSON_BLOCK = Pattern.compile(
            "^```(?:json)?\\s*([\\s\\S]*?)\\s*```$",
            Pattern.CASE_INSENSITIVE
    );

    private final JsonMapper jsonMapper;

    public LLMResponseParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ParsedLlmRoutingResponse parse(String response) {
        if (response == null || response.isBlank()) {
            throw new LlmResponseParseException("LLM response is empty");
        }

        String normalizedResponse = unwrapMarkdownJson(response.trim());

        try {
            JsonNode root = jsonMapper.readTree(normalizedResponse);

            Long recommendedAgent = readRequiredLong(root, "recommendedAgent");
            Double confidence = readRequiredDouble(root, "confidence");
            String reasoning = readRequiredText(root, "reasoning");

            return new ParsedLlmRoutingResponse(recommendedAgent, confidence, reasoning);
        } catch (LlmResponseParseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LlmResponseParseException("Failed to parse LLM response as JSON", exception);
        }
    }

    private String unwrapMarkdownJson(String response) {
        var matcher = MARKDOWN_JSON_BLOCK.matcher(response);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return response;
    }

    private Long readRequiredLong(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            throw new LlmResponseParseException("Missing required field: " + fieldName);
        }
        if (!node.canConvertToLong()) {
            throw new LlmResponseParseException("Invalid value for field: " + fieldName);
        }
        return node.longValue();
    }

    private Double readRequiredDouble(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            throw new LlmResponseParseException("Missing required field: " + fieldName);
        }
        if (!node.isNumber()) {
            throw new LlmResponseParseException("Invalid value for field: " + fieldName);
        }
        return node.doubleValue();
    }

    private String readRequiredText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
            throw new LlmResponseParseException("Missing required field: " + fieldName);
        }
        return node.asText();
    }
}
