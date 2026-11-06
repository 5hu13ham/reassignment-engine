package com.zycus;

import com.zycus.ai.LLMResponseParser;
import com.zycus.exception.LlmResponseParseException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class LlmParserTest {
    private final LLMResponseParser parser=new LLMResponseParser(JsonMapper.builder().build());

    @Test void parsesValidatedResponse() {
        var result=parser.parse("{\"recommendedAgent\":2,\"confidence\":0.75,\"reasoning\":\"Low workload\"}");
        assertEquals(2L,result.recommendedAgent());
        assertEquals(0.75,result.confidence());
    }

    @Test void rejectsOutOfRangeConfidenceAndInvalidAgent() {
        assertThrows(LlmResponseParseException.class,()->parser.parse(
                "{\"recommendedAgent\":2,\"confidence\":1.4,\"reasoning\":\"x\"}"));
        assertThrows(LlmResponseParseException.class,()->parser.parse(
                "{\"recommendedAgent\":-1,\"confidence\":0.4,\"reasoning\":\"x\"}"));
    }
}
