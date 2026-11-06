package com.zycus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(@NotBlank @Size(max=500) String description,
                                 @NotNull Long assignedAgentId) {}
