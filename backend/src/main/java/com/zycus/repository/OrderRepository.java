package com.zycus.repository;

import com.zycus.entity.Order;
import com.zycus.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByAssignedAgentId(Long agentId);

    List<Order> findByAssignedAgentIdAndStatus(Long agentId, OrderStatus status);
}
