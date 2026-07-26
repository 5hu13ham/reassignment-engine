package com.zycus.service;

import com.zycus.dto.RoutingResult;
import com.zycus.entity.Agent;
import com.zycus.entity.AgentStatus;
import com.zycus.entity.Order;
import com.zycus.entity.ReassignmentSuggestion;
import com.zycus.entity.SuggestionStatus;
import com.zycus.entity.TriggerReason;
import com.zycus.exception.ResourceNotFoundException;
import com.zycus.repository.AgentRepository;
import com.zycus.repository.OrderRepository;
import com.zycus.repository.ReassignmentSuggestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrates recommendation generation by loading domain data, delegating
 * routing decisions to {@link RoutingService}, and persisting pending suggestions.
 */
@Service
@Transactional
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;
    private final ReassignmentSuggestionRepository suggestionRepository;
    private final RoutingService routingService;

    public RecommendationService(
            OrderRepository orderRepository,
            AgentRepository agentRepository,
            ReassignmentSuggestionRepository suggestionRepository,
            RoutingService routingService
    ) {
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
        this.suggestionRepository = suggestionRepository;
        this.routingService = routingService;
    }

    public void generateRecommendation(Long orderId, TriggerReason triggerReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (suggestionRepository.findFirstByOrderIdAndStatusAndTriggerReason(
                orderId,
                SuggestionStatus.PENDING,
                triggerReason
        ).isPresent()) {
            log.info("Pending suggestion already exists for order {}. Skipping recommendation generation.", orderId);
            return;
        }

        List<Agent> availableAgents = agentRepository.findByStatus(AgentStatus.AVAILABLE);
        if (availableAgents.isEmpty()) {
            log.warn("No available agents found for order {}. Skipping recommendation.", orderId);
            return;
        }

        RoutingResult routingResult = routingService.suggest(
                order,
                availableAgents,
                triggerReason
        );

        ReassignmentSuggestion suggestion = new ReassignmentSuggestion();
        suggestion.setOrder(order);
        suggestion.setRecommendedAgent(routingResult.recommendedAgent());
        suggestion.setConfidence(routingResult.confidence());
        suggestion.setReasoning(routingResult.reasoning());
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setTriggerReason(triggerReason);

        suggestionRepository.save(suggestion);
    }
}
