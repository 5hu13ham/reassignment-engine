package com.zycus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="reassignment_suggestions", indexes={
    @Index(name="idx_suggestion_order_status", columnList="order_id,status")
})
@Getter @Setter @NoArgsConstructor
public class ReassignmentSuggestion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    private Order order;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    private Agent recommendedAgent;
    /** Nullable for deterministic rule-based strategies. */
    private Double confidence;
    @Column(length=3000)
    private String reasoning;
    @Enumerated(EnumType.STRING)
    private SuggestionStatus status;
    @Enumerated(EnumType.STRING)
    private TriggerReason triggerReason;
    private String strategyUsed;
    private boolean fallbackUsed;
}
