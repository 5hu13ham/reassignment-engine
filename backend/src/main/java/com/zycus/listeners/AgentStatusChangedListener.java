package com.zycus.listeners;

import com.zycus.entity.AgentStatus;
import com.zycus.entity.TriggerReason;
import com.zycus.events.AgentStatusChangedEvent;
import com.zycus.service.OrderService;
import com.zycus.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Non-blocking re-plan after the OFFLINE transition commits. */
@Component
public class AgentStatusChangedListener {
    private static final Logger log = LoggerFactory.getLogger(AgentStatusChangedListener.class);
    private final OrderService orders;
    private final RecommendationService recommendations;

    public AgentStatusChangedListener(OrderService orders, RecommendationService recommendations) {
        this.orders = orders;
        this.recommendations = recommendations;
    }

    @Async("reassignmentExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgentStatusChanged(AgentStatusChangedEvent event) {
        if (event.currentStatus() != AgentStatus.OFFLINE ||
                event.previousStatus() == AgentStatus.OFFLINE) return;
        for (Long orderId : orders.findAffectedOrderIds(event.agentId())) {
            try {
                recommendations.generateRecommendation(orderId, TriggerReason.AGENT_OFFLINE);
            } catch (Exception ex) {
                // Preserve other stranded orders; each order has its own transaction.
                log.error("Failed to re-plan order {} for agent {}", orderId, event.agentId(), ex);
            }
        }
    }
}
