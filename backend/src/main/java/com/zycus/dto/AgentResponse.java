package com.zycus.dto;

import com.zycus.entity.AgentStatus;

public record AgentResponse(
        Long id,
        String name,
        Integer activeOrderCount,
        AgentStatus status
) {
}
