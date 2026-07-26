package com.zycus.mapper;

import com.zycus.dto.OrderResponse;
import com.zycus.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getDescription(),
                order.getAssignedAgent() != null ? order.getAssignedAgent().getId() : null,
                order.getStatus()
        );
    }
}
