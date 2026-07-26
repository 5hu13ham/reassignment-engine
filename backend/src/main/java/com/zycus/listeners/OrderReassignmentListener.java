package com.zycus.listeners;

import com.zycus.events.OrderNeedsReassignmentEvent;
import com.zycus.service.RecommendationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to orders that need reassignment by delegating recommendation
 * generation to the application service layer.
 */
@Component
public class OrderReassignmentListener {

    private final RecommendationService recommendationService;

    public OrderReassignmentListener(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @EventListener
    public void handleOrderNeedsReassignment(OrderNeedsReassignmentEvent event) {
        recommendationService.generateRecommendation(event.getOrderId());
    }
}
