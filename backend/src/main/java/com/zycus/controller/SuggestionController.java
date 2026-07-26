package com.zycus.controller;

import com.zycus.dto.SuggestionResponse;
import com.zycus.dto.UpdateSuggestionStatusRequest;
import com.zycus.entity.SuggestionStatus;
import com.zycus.service.ReassignmentSuggestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/suggestions")
public class SuggestionController {

    private final ReassignmentSuggestionService suggestionService;

    public SuggestionController(ReassignmentSuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public List<SuggestionResponse> getSuggestions(
            @RequestParam(defaultValue = "PENDING") SuggestionStatus status
    ) {
        return suggestionService.getSuggestionsByStatus(status);
    }

    @PatchMapping("/{id}")
    public SuggestionResponse updateSuggestionStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSuggestionStatusRequest request
    ) {
        return suggestionService.updateSuggestionStatus(id, request);
    }
}
