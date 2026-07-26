package com.zycus.strategy;

import com.zycus.dto.RoutingResult;
import com.zycus.entity.Agent;
import com.zycus.entity.Order;
import com.zycus.entity.TriggerReason;

import java.util.List;

public interface RoutingStrategy {

    String strategyKey();

    RoutingResult suggest(
            Order order,
            List<Agent> availableAgents,
            TriggerReason triggerReason
    );
}
