package com.zycus.controller;

import com.zycus.dto.AgentResponse;
import com.zycus.dto.UpdateAgentStatusRequest;
import com.zycus.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/agents")
public class AgentController {
    private final AgentService agents;
    public AgentController(AgentService agents) {this.agents=agents;}

    @GetMapping
    public List<AgentResponse> list() {return agents.getAgents();}

    @PatchMapping("/{id}/status")
    public AgentResponse status(@PathVariable Long id, @Valid @RequestBody UpdateAgentStatusRequest request) {
        return agents.updateAgentStatus(id,request);
    }
}
