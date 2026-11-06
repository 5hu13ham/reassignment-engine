package com.zycus.service;

import com.zycus.dto.SuggestionResponse;
import com.zycus.dto.UpdateSuggestionStatusRequest;
import com.zycus.entity.*;
import com.zycus.exception.InvalidSuggestionException;
import com.zycus.exception.ResourceNotFoundException;
import com.zycus.mapper.SuggestionMapper;
import com.zycus.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ReassignmentSuggestionService {
    private final ReassignmentSuggestionRepository suggestions;
    private final OrderRepository orders;
    private final AgentRepository agents;
    private final SuggestionMapper mapper;

    public ReassignmentSuggestionService(ReassignmentSuggestionRepository suggestions,
            OrderRepository orders, AgentRepository agents, SuggestionMapper mapper) {
        this.suggestions=suggestions;this.orders=orders;this.agents=agents;this.mapper=mapper;
    }

    @Transactional(readOnly=true)
    public List<SuggestionResponse> getSuggestionsByStatus(SuggestionStatus status) {
        return suggestions.findByStatus(status).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public SuggestionResponse updateSuggestionStatus(Long id,UpdateSuggestionStatusRequest request) {
        if (request.status()==SuggestionStatus.PENDING)
            throw new InvalidSuggestionException("Only ACCEPTED or REJECTED are allowed");
        ReassignmentSuggestion suggestion=suggestions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suggestion",id));
        // Serialize approval and rejection decisions for the same order.
        Order order=orders.findByIdForUpdate(suggestion.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order",suggestion.getOrder().getId()));
        if (suggestion.getStatus()!=SuggestionStatus.PENDING)
            throw new InvalidSuggestionException("Suggestion has already been resolved");
        if (order.getStatus()==OrderStatus.DELIVERED || order.getStatus()==OrderStatus.REASSIGNED)
            throw new InvalidSuggestionException("Order is already finalized");

        if (request.status()==SuggestionStatus.ACCEPTED) {
            Long nextId=suggestion.getRecommendedAgent().getId();
            Long oldId=order.getAssignedAgent().getId();
            if (nextId.equals(oldId))
                throw new InvalidSuggestionException("Replacement must differ from current agent");
            Agent next=agents.findByIdForUpdate(nextId)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent",nextId));
            if (next.getStatus()!=AgentStatus.AVAILABLE)
                throw new InvalidSuggestionException("Recommended agent is no longer available; retry routing");
            Agent previous=agents.findByIdForUpdate(oldId)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent",oldId));

            order.setAssignedAgent(next);
            order.setStatus(OrderStatus.REASSIGNED);
            previous.setActiveOrderCount(Math.max(0,previous.getActiveOrderCount()==null?0:previous.getActiveOrderCount()-1));
            next.setActiveOrderCount((next.getActiveOrderCount()==null?0:next.getActiveOrderCount())+1);
            suggestion.setStatus(SuggestionStatus.ACCEPTED);
            // Clear other pending suggestions so no stale approval can assign the order twice.
            for(ReassignmentSuggestion pending:suggestions.findByOrderIdAndStatus(order.getId(),SuggestionStatus.PENDING)) {
                if(!pending.getId().equals(id)) pending.setStatus(SuggestionStatus.REJECTED);
            }
        } else {
            suggestion.setStatus(SuggestionStatus.REJECTED);
            if (suggestion.getTriggerReason()==TriggerReason.AGENT_OFFLINE)
                order.setStatus(OrderStatus.REASSIGNMENT_PENDING);
        }
        return mapper.toResponse(suggestions.save(suggestion));
    }
}
