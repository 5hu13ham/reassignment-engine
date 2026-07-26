package com.zycus.strategy;

import com.zycus.ai.LLMGateway;
import com.zycus.ai.LLMResponseParser;
import com.zycus.ai.ParsedLlmRoutingResponse;
import com.zycus.ai.PromptBuilder;
import com.zycus.dto.RoutingResult;
import com.zycus.entity.Agent;
import com.zycus.entity.Order;
import com.zycus.entity.TriggerReason;
import com.zycus.exception.InvalidLlmRecommendationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class AiRoutingStrategy implements RoutingStrategy {

    private final PromptBuilder promptBuilder;
    private final LLMGateway llmGateway;
    private final LLMResponseParser llmResponseParser;

    public AiRoutingStrategy(
            PromptBuilder promptBuilder,
            LLMGateway llmGateway,
            LLMResponseParser llmResponseParser
    ) {
        this.promptBuilder = promptBuilder;
        this.llmGateway = llmGateway;
        this.llmResponseParser = llmResponseParser;
    }

    @Override
    public String strategyKey() {
        return "ai";
    }

    @Override
    public RoutingResult suggest(
            Order order,
            List<Agent> availableAgents,
            TriggerReason triggerReason
    ) {
        String prompt = promptBuilder.buildPrompt(order, availableAgents, triggerReason);
        String llmResponse = llmGateway.generate(prompt);
        ParsedLlmRoutingResponse parsedResponse = llmResponseParser.parse(llmResponse);
        Agent recommendedAgent = resolveRecommendedAgent(parsedResponse.recommendedAgent(), availableAgents);

        return new RoutingResult(
                recommendedAgent,
                parsedResponse.confidence(),
                parsedResponse.reasoning()
        );
    }

    private Agent resolveRecommendedAgent(Long recommendedAgentId, List<Agent> availableAgents) {
        return availableAgents.stream()
                .filter(agent -> Objects.equals(agent.getId(), recommendedAgentId))
                .findFirst()
                .orElseThrow(() -> new InvalidLlmRecommendationException(
                        "Recommended agent " + recommendedAgentId + " is not in the available agents list"
                ));
    }
}
