package com.zycus.service;

import com.zycus.dto.CreateOrderRequest;
import com.zycus.dto.OrderResponse;
import com.zycus.entity.Agent;
import com.zycus.entity.Order;
import com.zycus.entity.OrderStatus;
import com.zycus.exception.ResourceNotFoundException;
import com.zycus.events.OrderNeedsReassignmentEvent;
import com.zycus.mapper.OrderMapper;
import com.zycus.repository.AgentRepository;
import com.zycus.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            OrderRepository orderRepository,
            AgentRepository agentRepository,
            OrderMapper orderMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
        this.orderMapper = orderMapper;
        this.eventPublisher = eventPublisher;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        Agent assignedAgent = agentRepository.findById(request.assignedAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent", request.assignedAgentId()));

        Order order = new Order();
        order.setDescription(request.description());
        order.setAssignedAgent(assignedAgent);
        order.setStatus(OrderStatus.ASSIGNED);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(OrderStatus status) {
        List<Order> orders = status != null
                ? orderRepository.findByStatus(status)
                : orderRepository.findAll();

        return orders.stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersAssignedToAgent(Long agentId) {
        if (!agentRepository.existsById(agentId)) {
            throw new ResourceNotFoundException("Agent", agentId);
        }

        return orderRepository.findByAssignedAgentId(agentId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public void handleAgentOffline(Long agentId) {
        if (!agentRepository.existsById(agentId)) {
            throw new ResourceNotFoundException("Agent", agentId);
        }

        orderRepository.findByAssignedAgentIdAndStatus(agentId, OrderStatus.ASSIGNED)
                .forEach(order -> eventPublisher.publishEvent(
                        new OrderNeedsReassignmentEvent(order.getId())
                ));
    }
}
