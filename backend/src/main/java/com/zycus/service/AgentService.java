package com.zycus.service;

import com.zycus.dto.AgentResponse;
import com.zycus.dto.UpdateAgentStatusRequest;
import com.zycus.entity.Agent;
import com.zycus.entity.AgentStatus;
import com.zycus.events.AgentStatusChangedEvent;
import com.zycus.exception.ResourceNotFoundException;
import com.zycus.mapper.AgentMapper;
import com.zycus.repository.AgentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AgentService {
    private final AgentRepository agents;
    private final ApplicationEventPublisher events;
    private final AgentMapper mapper;

    public AgentService(AgentRepository agents, ApplicationEventPublisher events, AgentMapper mapper) {
        this.agents = agents; this.events = events; this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AgentResponse> getAgents() {
        return agents.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public AgentResponse updateAgentStatus(Long id, UpdateAgentStatusRequest request) {
        Agent agent = agents.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", id));
        AgentStatus previous = agent.getStatus();
        AgentStatus next = request.status();
        if (previous != next) {
            agent.setStatus(next);
            agents.save(agent);
            events.publishEvent(new AgentStatusChangedEvent(id, previous, next));
        }
        return mapper.toResponse(agent);
    }
}
