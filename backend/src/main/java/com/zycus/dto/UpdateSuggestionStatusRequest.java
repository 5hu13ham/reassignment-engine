package com.zycus.dto;

import com.zycus.entity.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSuggestionStatusRequest(
        @NotNull SuggestionStatus status
) {
}
