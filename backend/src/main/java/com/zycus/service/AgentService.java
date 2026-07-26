package com.zycus.service;

import com.zycus.dto.AgentResponse;
import com.zycus.dto.UpdateAgentStatusRequest;
import com.zycus.entity.Agent;
import com.zycus.entity.AgentStatus;
import com.zycus.event.AgentStatusChangedEvent;
import com.zycus.exception.ResourceNotFoundException;
import com.zycus.mapper.AgentMapper;
import com.zycus.repository.AgentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AgentService {

    private final AgentRepository agentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AgentMapper agentMapper;

    public AgentService(
            AgentRepository agentRepository,
            ApplicationEventPublisher eventPublisher,
            AgentMapper agentMapper
    ) {
        this.agentRepository = agentRepository;
        this.eventPublisher = eventPublisher;
        this.agentMapper = agentMapper;
    }

    @Transactional(readOnly = true)
    public AgentResponse getAgentById(Long id) {
        return agentMapper.toResponse(findAgentById(id));
    }

    @Transactional(readOnly = true)
    public List<AgentResponse> getAvailableAgents() {
        return agentRepository.findByStatus(AgentStatus.AVAILABLE).stream()
                .map(agentMapper::toResponse)
                .toList();
    }

    public AgentResponse updateAgentStatus(Long id, UpdateAgentStatusRequest request) {
        Agent agent = findAgentById(id);
        AgentStatus previousStatus = agent.getStatus();
        AgentStatus newStatus = request.status();

        agent.setStatus(newStatus);
        Agent savedAgent = agentRepository.save(agent);

        if (newStatus == AgentStatus.OFFLINE && previousStatus != AgentStatus.OFFLINE) {
            eventPublisher.publishEvent(new AgentStatusChangedEvent(savedAgent.getId()));
        }

        return agentMapper.toResponse(savedAgent);
    }

    private Agent findAgentById(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", id));
    }
}
