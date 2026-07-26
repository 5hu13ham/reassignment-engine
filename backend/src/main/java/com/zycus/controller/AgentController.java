package com.zycus.controller;

import com.zycus.dto.AgentResponse;
import com.zycus.dto.UpdateAgentStatusRequest;
import com.zycus.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PatchMapping("/{id}/status")
    public AgentResponse updateAgentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAgentStatusRequest request
    ) {
        return agentService.updateAgentStatus(id, request);
    }
}
