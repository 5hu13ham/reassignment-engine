package com.zycus.dto;

import com.zycus.entity.Agent;

public record RoutingResult(
        Agent recommendedAgent,
        Double confidence,
        String reasoning
) {
}
