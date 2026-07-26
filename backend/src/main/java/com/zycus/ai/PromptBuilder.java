package com.zycus.ai;

import com.zycus.entity.Agent;
import com.zycus.entity.Order;
import com.zycus.entity.TriggerReason;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    private static final String RESPONSE_SCHEMA = """
            Respond with JSON only using this exact schema:
            {
              "recommendedAgent": <agent id>,
              "confidence": <number between 0.0 and 1.0>,
              "reasoning": "<brief explanation>"
            }
            """;

    public String buildPrompt(Order order, List<Agent> availableAgents, TriggerReason triggerReason) {
        return switch (triggerReason) {
            case INITIAL -> buildInitialRoutingPrompt(order, availableAgents);
            case AGENT_OFFLINE -> buildReplanningPrompt(order, availableAgents);
        };
    }

    private String buildInitialRoutingPrompt(Order order, List<Agent> availableAgents) {
        return """
                You are a delivery routing assistant. Select the best available agent for a new order assignment.

                %s

                %s

                %s
                """.formatted(
                formatOrderContext(order),
                formatAvailableAgents(availableAgents),
                RESPONSE_SCHEMA
        );
    }

    private String buildReplanningPrompt(Order order, List<Agent> availableAgents) {
        return """
                You are a delivery routing assistant. The currently assigned agent has gone offline.
                Select the best replacement agent to reassign this order.

                %s

                %s

                %s
                """.formatted(
                formatOrderContext(order),
                formatAvailableAgents(availableAgents),
                RESPONSE_SCHEMA
        );
    }

    private String formatOrderContext(Order order) {
        Long assignedAgentId = order.getAssignedAgent() != null ? order.getAssignedAgent().getId() : null;

        return """
                Order details:
                - id: %d
                - description: %s
                - status: %s
                - pickupZone: %s
                - dropoffZone: %s
                - weightClass: %s
                - assignedAgentId: %s
                """.formatted(
                order.getId(),
                nullSafe(order.getDescription()),
                order.getStatus(),
                nullSafe(order.getPickupZone()),
                nullSafe(order.getDropoffZone()),
                nullSafe(order.getWeightClass()),
                assignedAgentId
        );
    }

    private String formatAvailableAgents(List<Agent> availableAgents) {
        if (availableAgents.isEmpty()) {
            return "Available agents:\n- none";
        }

        String agents = availableAgents.stream()
                .map(agent -> "- id: %d, name: %s, status: %s, activeOrderCount: %s".formatted(
                        agent.getId(),
                        nullSafe(agent.getName()),
                        agent.getStatus(),
                        agent.getActiveOrderCount()
                ))
                .collect(Collectors.joining("\n"));

        return "Available agents:\n" + agents;
    }

    private String nullSafe(String value) {
        return value != null ? value : "null";
    }
}
