package com.zycus.listeners;

import com.zycus.events.AgentStatusChangedEvent;
import com.zycus.service.OrderService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to agent status changes by triggering order-level orchestration when
 * an agent goes offline, without embedding reassignment rules in the listener.
 */
@Component
public class AgentStatusChangedListener {

    private static final String OFFLINE_STATUS = "OFFLINE";

    private final OrderService orderService;

    public AgentStatusChangedListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @EventListener
    public void handleAgentStatusChanged(AgentStatusChangedEvent event) {
        if (!OFFLINE_STATUS.equals(event.getCurrentStatus())) {
            return;
        }

        orderService.handleAgentOffline(event.getAgentId());
    }
}
