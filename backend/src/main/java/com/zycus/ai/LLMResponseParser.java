package com.zycus.ai;

import com.zycus.exception.LlmResponseParseException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.util.regex.Pattern;

@Component
public class LLMResponseParser {
    private final JsonMapper json;
    private static final Pattern FENCED=Pattern.compile(
            "^\x60\x60\x60(?:json)?\\s*([\\s\\S]*?)\\s*\x60\x60\x60$",Pattern.CASE_INSENSITIVE);

    public LLMResponseParser(JsonMapper json) {this.json=json;}

    public ParsedLlmRoutingResponse parse(String raw) {
        if(raw==null || raw.isBlank()) throw new LlmResponseParseException("LLM response is empty");
        var matcher=FENCED.matcher(raw.strip());
        String normalized=matcher.matches()?matcher.group(1):raw.strip();
        try {
            JsonNode root=json.readTree(normalized);
            JsonNode agent=root.path("recommendedAgent");
            JsonNode conf=root.path("confidence");
            JsonNode reason=root.path("reasoning");
            if(!agent.canConvertToLong() || agent.longValue()<=0)
                throw new LlmResponseParseException("Invalid recommendedAgent ID");
            if(!conf.isNumber() || !Double.isFinite(conf.doubleValue())
                    || conf.doubleValue()<0 || conf.doubleValue()>1)
                throw new LlmResponseParseException("Confidence must be between 0 and 1");
            if(!reason.isTextual() || reason.asText().isBlank() || reason.asText().length()>3000)
                throw new LlmResponseParseException("Missing or oversized reasoning");
            return new ParsedLlmRoutingResponse(agent.longValue(),conf.doubleValue(),reason.asText());
        } catch(LlmResponseParseException ex) {throw ex;}
        catch(Exception ex) {throw new LlmResponseParseException("Invalid LLM JSON",ex);}
    }
}
