package com.zycus.mapper;

import com.zycus.dto.SuggestionResponse;
import com.zycus.entity.ReassignmentSuggestion;
import org.springframework.stereotype.Component;

@Component
public class SuggestionMapper {
    public SuggestionResponse toResponse(ReassignmentSuggestion s) {
        return new SuggestionResponse(
                s.getId(), s.getOrder().getId(), s.getRecommendedAgent().getId(),
                s.getConfidence(), s.getReasoning(), s.getStatus(), s.getTriggerReason(),
                s.getStrategyUsed(), s.isFallbackUsed());
    }
}
