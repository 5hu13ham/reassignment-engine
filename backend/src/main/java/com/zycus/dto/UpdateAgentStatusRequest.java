package com.zycus.dto;

import com.zycus.entity.AgentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAgentStatusRequest(
        @NotNull AgentStatus status
) {
}
