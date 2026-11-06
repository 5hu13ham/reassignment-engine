package com.zycus.strategy;

import com.zycus.ai.*;
import com.zycus.dto.RoutingResult;
import com.zycus.entity.*;
import com.zycus.exception.InvalidLlmRecommendationException;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Objects;

@Component
public class AiRoutingStrategy implements RoutingStrategy {
    private final PromptBuilder prompts;
    private final LLMGateway gateway;
    private final LLMResponseParser parser;
    public AiRoutingStrategy(PromptBuilder prompts,LLMGateway gateway,LLMResponseParser parser) {
        this.prompts=prompts;this.gateway=gateway;this.parser=parser;
    }
    @Override public String strategyKey() {return "ai";}

    @Override
    public RoutingResult suggest(Order order,List<Agent> eligible,TriggerReason trigger) {
        ParsedLlmRoutingResponse parsed=parser.parse(gateway.generate(prompts.buildPrompt(order,eligible,trigger)));
        Agent agent=eligible.stream()
                .filter(a->a.getStatus()==AgentStatus.AVAILABLE)
                .filter(a->Objects.equals(a.getId(),parsed.recommendedAgent()))
                .findFirst()
                .orElseThrow(()->new InvalidLlmRecommendationException("Recommended agent is not eligible"));
        return new RoutingResult(agent,parsed.confidence(),parsed.reasoning(),"ai",false);
    }
}
