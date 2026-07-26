package com.zycus.events;

/**
 * Published after an order requires reassignment, signaling that a new routing
 * recommendation should be generated for the affected order.
 */
public final class OrderNeedsReassignmentEvent {

    private final Long orderId;

    public OrderNeedsReassignmentEvent(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
