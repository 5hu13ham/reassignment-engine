package com.zycus.service;

import com.zycus.dto.RoutingResult;
import com.zycus.entity.Agent;
import com.zycus.entity.Order;
import com.zycus.entity.TriggerReason;
import com.zycus.strategy.RoutingStrategy;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);
    private static final String AI_STRATEGY_KEY = "ai";
    private static final String RULE_STRATEGY_KEY = "rule";

    private final Map<String, RoutingStrategy> routingStrategies;
    private final String activeStrategyKey;

    public RoutingService(
            Map<String, RoutingStrategy> routingStrategies,
            @Value("${routing.strategy}") String activeStrategyKey
    ) {
        this.routingStrategies = routingStrategies;
        this.activeStrategyKey = activeStrategyKey;
    }

    @PostConstruct
    void validateActiveStrategy() {
        if (!routingStrategies.containsKey(activeStrategyKey)) {
            throw new IllegalStateException(
                    "Unknown routing strategy configured: " + activeStrategyKey
            );
        }

        if (AI_STRATEGY_KEY.equals(activeStrategyKey) && !routingStrategies.containsKey(RULE_STRATEGY_KEY)) {
            throw new IllegalStateException(
                    "Rule-based routing strategy must be available when AI routing is configured"
            );
        }
    }

    public RoutingResult suggest(
            Order order,
            List<Agent> availableAgents,
            TriggerReason triggerReason
    ) {
        if (AI_STRATEGY_KEY.equals(activeStrategyKey)) {
            return suggestWithAiFallback(order, availableAgents, triggerReason);
        }

        return routingStrategies.get(activeStrategyKey)
                .suggest(order, availableAgents, triggerReason);
    }

    private RoutingResult suggestWithAiFallback(
            Order order,
            List<Agent> availableAgents,
            TriggerReason triggerReason
    ) {
        RoutingStrategy aiStrategy = routingStrategies.get(AI_STRATEGY_KEY);
        RoutingStrategy ruleStrategy = routingStrategies.get(RULE_STRATEGY_KEY);

        try {
            return aiStrategy.suggest(order, availableAgents, triggerReason);
        } catch (Exception exception) {
            log.warn(
                    "AI routing failed for order {} ({}). Falling back to rule-based routing.",
                    order.getId(),
                    exception.getMessage()
            );
            return ruleStrategy.suggest(order, availableAgents, triggerReason);
        }
    }
}
