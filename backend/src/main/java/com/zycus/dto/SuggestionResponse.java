package com.zycus.dto;

import com.zycus.entity.SuggestionStatus;
import com.zycus.entity.TriggerReason;

public record SuggestionResponse(
        Long id, Long orderId, Long recommendedAgentId, Double confidence,
        String reasoning, SuggestionStatus status, TriggerReason triggerReason,
        String strategyUsed, boolean fallbackUsed) {}
