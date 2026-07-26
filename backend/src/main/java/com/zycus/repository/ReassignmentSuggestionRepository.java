package com.zycus.repository;

import com.zycus.entity.ReassignmentSuggestion;
import com.zycus.entity.SuggestionStatus;
import com.zycus.entity.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReassignmentSuggestionRepository extends JpaRepository<ReassignmentSuggestion, Long> {

    Optional<ReassignmentSuggestion> findFirstByOrderIdAndStatusAndTriggerReason(
            Long orderId,
            SuggestionStatus status,
            TriggerReason triggerReason
    );

    List<ReassignmentSuggestion> findByStatus(SuggestionStatus status);
}
