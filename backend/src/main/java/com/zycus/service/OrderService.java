package com.zycus.service;

import com.zycus.dto.CreateOrderRequest;
import com.zycus.dto.OrderResponse;
import com.zycus.entity.Agent;
import com.zycus.entity.AgentStatus;
import com.zycus.entity.Order;
import com.zycus.entity.OrderStatus;
import com.zycus.exception.InvalidSuggestionException;
import com.zycus.exception.ResourceNotFoundException;
import com.zycus.mapper.OrderMapper;
import com.zycus.repository.AgentRepository;
import com.zycus.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final AgentRepository agents;
    private final OrderMapper mapper;

    public OrderService(OrderRepository orders, AgentRepository agents, OrderMapper mapper) {
        this.orders=orders; this.agents=agents; this.mapper=mapper;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Agent agent=agents.findByIdForUpdate(request.assignedAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent",request.assignedAgentId()));
        if (agent.getStatus()==AgentStatus.OFFLINE)
            throw new InvalidSuggestionException("Cannot assign a new order to an offline agent");
        Order order=new Order();
        order.setDescription(request.description().strip());
        order.setAssignedAgent(agent);
        order.setStatus(OrderStatus.ASSIGNED);
        Order saved=orders.save(order);
        agent.setActiveOrderCount(Math.max(0,agent.getActiveOrderCount()==null?0:agent.getActiveOrderCount())+1);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly=true)
    public List<OrderResponse> getOrders(OrderStatus status) {
        return (status==null?orders.findAll():orders.findByStatus(status))
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly=true)
    public List<Long> findAffectedOrderIds(Long agentId) {
        return orders.findByAssignedAgentIdAndStatus(agentId,OrderStatus.ASSIGNED)
                .stream().map(Order::getId).toList();
    }
}
