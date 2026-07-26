package com.zycus.ai;

public record ParsedLlmRoutingResponse(
        Long recommendedAgent,
        Double confidence,
        String reasoning
) {
}
