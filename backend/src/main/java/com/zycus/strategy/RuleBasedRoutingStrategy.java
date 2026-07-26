package com.zycus.strategy;

import com.zycus.dto.RoutingResult;
import com.zycus.entity.Agent;
import com.zycus.entity.AgentStatus;
import com.zycus.entity.Order;
import com.zycus.entity.TriggerReason;
import com.zycus.exception.NoAvailableAgentException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class RuleBasedRoutingStrategy implements RoutingStrategy {

    private static final String REASONING =
            "Recommendation made using the rule-based strategy because the selected agent has the lowest active order count.";

    @Override
    public String strategyKey() {
        return "rule";
    }

    @Override
    public RoutingResult suggest(
            Order order,
            List<Agent> availableAgents,
            TriggerReason triggerReason
    ) {
        Agent selectedAgent = availableAgents.stream()
                .filter(agent -> agent.getStatus() == AgentStatus.AVAILABLE)
                .min(Comparator.comparing(
                        Agent::getActiveOrderCount,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .orElseThrow(NoAvailableAgentException::new);

        return new RoutingResult(selectedAgent, 1.0, REASONING);
    }
}
