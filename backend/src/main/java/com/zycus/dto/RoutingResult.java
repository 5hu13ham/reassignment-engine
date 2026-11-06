package com.zycus.dto;
import com.zycus.entity.Agent;

/** Null confidence for deterministic rule-based decisions: not an AI probability. */
public record RoutingResult(Agent recommendedAgent, Double confidence, String reasoning,
                            String strategyUsed, boolean fallbackUsed) {}
