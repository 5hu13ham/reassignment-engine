package com.zycus.service;

import com.zycus.dto.RoutingResult;
import com.zycus.entity.*;
import com.zycus.exception.InvalidSuggestionException;
import com.zycus.exception.NoAvailableAgentException;
import com.zycus.exception.ResourceNotFoundException;
import com.zycus.repository.AgentRepository;
import com.zycus.repository.OrderRepository;
import com.zycus.repository.ReassignmentSuggestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Serializes decisions for an order using a row-level lock; returns existing pending suggestion. */
@Service
public class RecommendationService {
    private static final Logger log=LoggerFactory.getLogger(RecommendationService.class);
    private final OrderRepository orders;
    private final AgentRepository agents;
    private final ReassignmentSuggestionRepository suggestions;
    private final RoutingService routing;

    public RecommendationService(OrderRepository orders,AgentRepository agents,
            ReassignmentSuggestionRepository suggestions,RoutingService routing) {
        this.orders=orders;this.agents=agents;this.suggestions=suggestions;this.routing=routing;
    }

    @Transactional
    public ReassignmentSuggestion generateRecommendation(Long orderId, TriggerReason trigger) {
        Order order=orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order",orderId));
        if (order.getStatus()==OrderStatus.DELIVERED ||
                order.getStatus()==OrderStatus.REASSIGNED) {
            throw new InvalidSuggestionException("Order is not eligible for reassignment");
        }
        if (trigger==TriggerReason.AGENT_OFFLINE &&
                order.getAssignedAgent().getStatus()!=AgentStatus.OFFLINE) {
            return null; // Agent returned online before async handler executed.
        }
        var existing=suggestions.findFirstByOrderIdAndStatus(orderId,SuggestionStatus.PENDING);
        if (existing.isPresent()) return existing.get();

        if (trigger==TriggerReason.AGENT_OFFLINE && order.getStatus()!=OrderStatus.REASSIGNMENT_PENDING) {
            order.setStatus(OrderStatus.REASSIGNMENT_PENDING);
            orders.save(order);
        }
        List<Agent> eligible=agents.findByStatus(AgentStatus.AVAILABLE).stream()
                .filter(agent -> !agent.getId().equals(order.getAssignedAgent().getId()))
                .toList();
        if (eligible.isEmpty()) {
            if (trigger==TriggerReason.AGENT_OFFLINE) {
                log.warn("Order {} stranded; no replacement available",orderId);
                return null; // Pending order remains visible; ops can retry later.
            }
            throw new NoAvailableAgentException();
        }
        RoutingResult result=routing.suggest(order,eligible,trigger);
        if (result.recommendedAgent()==null ||
                eligible.stream().noneMatch(a->a.getId().equals(result.recommendedAgent().getId()))) {
            throw new InvalidSuggestionException("Strategy recommended an ineligible agent");
        }
        ReassignmentSuggestion suggestion=new ReassignmentSuggestion();
        suggestion.setOrder(order);
        suggestion.setRecommendedAgent(result.recommendedAgent());
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setTriggerReason(trigger);
        suggestion.setReasoning(result.reasoning());
        suggestion.setConfidence(result.confidence());
        suggestion.setStrategyUsed(result.strategyUsed());
        suggestion.setFallbackUsed(result.fallbackUsed());
        return suggestions.save(suggestion);
    }
}
