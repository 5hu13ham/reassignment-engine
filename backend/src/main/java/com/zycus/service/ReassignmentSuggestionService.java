package com.zycus.service;

import com.zycus.dto.SuggestionResponse;
import com.zycus.dto.UpdateSuggestionStatusRequest;
import com.zycus.entity.Agent;
import com.zycus.entity.Order;
import com.zycus.entity.ReassignmentSuggestion;
import com.zycus.entity.SuggestionStatus;
import com.zycus.entity.TriggerReason;
import com.zycus.exception.ResourceNotFoundException;
import com.zycus.mapper.SuggestionMapper;
import com.zycus.repository.AgentRepository;
import com.zycus.repository.OrderRepository;
import com.zycus.repository.ReassignmentSuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReassignmentSuggestionService {

    private final ReassignmentSuggestionRepository suggestionRepository;
    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;
    private final SuggestionMapper suggestionMapper;

    public ReassignmentSuggestionService(
            ReassignmentSuggestionRepository suggestionRepository,
            OrderRepository orderRepository,
            AgentRepository agentRepository,
            SuggestionMapper suggestionMapper
    ) {
        this.suggestionRepository = suggestionRepository;
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
        this.suggestionMapper = suggestionMapper;
    }

    public SuggestionResponse saveSuggestion(
            Long orderId,
            Long recommendedAgentId,
            Double confidence,
            String reasoning,
            TriggerReason triggerReason
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        Agent recommendedAgent = agentRepository.findById(recommendedAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", recommendedAgentId));

        ReassignmentSuggestion suggestion = new ReassignmentSuggestion();
        suggestion.setOrder(order);
        suggestion.setRecommendedAgent(recommendedAgent);
        suggestion.setConfidence(confidence);
        suggestion.setReasoning(reasoning);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setTriggerReason(triggerReason);

        return suggestionMapper.toResponse(suggestionRepository.save(suggestion));
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponse> getSuggestionsByStatus(SuggestionStatus status) {
        return suggestionRepository.findByStatus(status).stream()
                .map(suggestionMapper::toResponse)
                .toList();
    }

    public SuggestionResponse acceptSuggestion(Long id) {
        ReassignmentSuggestion suggestion = findSuggestionById(id);
        suggestion.setStatus(SuggestionStatus.ACCEPTED);
        return suggestionMapper.toResponse(suggestionRepository.save(suggestion));
    }

    public SuggestionResponse rejectSuggestion(Long id) {
        ReassignmentSuggestion suggestion = findSuggestionById(id);
        suggestion.setStatus(SuggestionStatus.REJECTED);
        return suggestionMapper.toResponse(suggestionRepository.save(suggestion));
    }

    public SuggestionResponse updateSuggestionStatus(Long id, UpdateSuggestionStatusRequest request) {
        return switch (request.status()) {
            case ACCEPTED -> acceptSuggestion(id);
            case REJECTED -> rejectSuggestion(id);
            case PENDING -> throw new IllegalArgumentException(
                    "Suggestion status cannot be updated to PENDING"
            );
        };
    }

    private ReassignmentSuggestion findSuggestionById(Long id) {
        return suggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suggestion", id));
    }
}
