package com.zycus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotBlank String description,
        @NotNull Long assignedAgentId
) {
}
