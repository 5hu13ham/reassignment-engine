package com.zycus.events;

import com.zycus.entity.AgentStatus;

/** Domain event published inside the agent status transaction. */
public record AgentStatusChangedEvent(Long agentId, AgentStatus previousStatus, AgentStatus currentStatus) {}
