package com.zycus.strategy;

import com.zycus.dto.RoutingResult;
import com.zycus.entity.*;
import com.zycus.exception.NoAvailableAgentException;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;

@Component
public class RuleBasedRoutingStrategy implements RoutingStrategy {
    @Override public String strategyKey() {return "rule";}

    @Override
    public RoutingResult suggest(Order order,List<Agent> agents,TriggerReason trigger) {
        Agent candidate=agents.stream()
                .filter(a->a.getStatus()==AgentStatus.AVAILABLE)
                .min(Comparator.comparing((Agent a)->a.getActiveOrderCount()==null
                        ?Integer.MAX_VALUE:a.getActiveOrderCount()).thenComparing(Agent::getId))
                .orElseThrow(NoAvailableAgentException::new);
        return new RoutingResult(candidate,null,
                "Rule-based: least active orders; ties broken by agent ID. " +
                (trigger==TriggerReason.AGENT_OFFLINE?"Recovery from offline agent.":"Manual suggestion."),
                "rule",false);
    }
}
