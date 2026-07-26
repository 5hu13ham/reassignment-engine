package com.zycus.dto;

import com.zycus.entity.OrderStatus;

public record OrderResponse(
        Long id,
        String description,
        Long assignedAgentId,
        OrderStatus status
) {
}
