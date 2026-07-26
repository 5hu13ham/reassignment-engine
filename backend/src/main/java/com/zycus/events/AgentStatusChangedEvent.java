package com.zycus.events;

/**
 * Published when an agent's status changes, signaling that downstream
 * orchestration may need to react (for example, re-planning assigned orders
 * when an agent goes offline).
 */
public final class AgentStatusChangedEvent {

    private final Long agentId;
    private final String previousStatus;
    private final String currentStatus;

    public AgentStatusChangedEvent(Long agentId, String previousStatus, String currentStatus) {
        this.agentId = agentId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
    }

    public Long getAgentId() {
        return agentId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}
