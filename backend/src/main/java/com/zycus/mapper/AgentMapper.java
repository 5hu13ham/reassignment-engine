package com.zycus.mapper;

import com.zycus.dto.AgentResponse;
import com.zycus.entity.Agent;
import org.springframework.stereotype.Component;

@Component
public class AgentMapper {

    public AgentResponse toResponse(Agent agent) {
        return new AgentResponse(
                agent.getId(),
                agent.getName(),
                agent.getActiveOrderCount(),
                agent.getStatus()
        );
    }
}
