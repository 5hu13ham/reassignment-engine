package com.zycus.mapper;

import com.zycus.dto.SuggestionResponse;
import com.zycus.entity.ReassignmentSuggestion;
import org.springframework.stereotype.Component;

@Component
public class SuggestionMapper {

    public SuggestionResponse toResponse(ReassignmentSuggestion suggestion) {
        return new SuggestionResponse(
                suggestion.getId(),
                suggestion.getOrder().getId(),
                suggestion.getRecommendedAgent().getId(),
                suggestion.getConfidence(),
                suggestion.getReasoning(),
                suggestion.getStatus(),
                suggestion.getTriggerReason()
        );
    }
}
