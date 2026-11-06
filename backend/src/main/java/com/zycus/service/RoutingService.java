package com.zycus.service;

import com.zycus.dto.RoutingResult;
import com.zycus.entity.*;
import com.zycus.exception.InvalidSuggestionException;
import com.zycus.strategy.RoutingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RoutingService {
    private static final Logger log=LoggerFactory.getLogger(RoutingService.class);
    private final Map<String,RoutingStrategy> strategies;
    private final AtomicReference<String> selected;

    public RoutingService(Map<String,RoutingStrategy> strategies,
                          @Value("${routing.strategy:rule}") String initialStrategy) {
        this.strategies=strategies;
        if(!strategies.containsKey(initialStrategy) || !strategies.containsKey("rule")) {
            throw new IllegalStateException("Missing routing strategy: "+initialStrategy);
        }
        selected=new AtomicReference<>(initialStrategy);
    }

    public String getActiveStrategy() {return selected.get();}

    public String setActiveStrategy(String key) {
        if(key==null || !strategies.containsKey(key))
            throw new InvalidSuggestionException("Unknown routing strategy: "+key);
        selected.set(key);
        return key;
    }

    public RoutingResult suggest(Order order,List<Agent> candidates,TriggerReason trigger) {
        String key=selected.get();
        if(!"ai".equals(key)) return strategies.get(key).suggest(order,candidates,trigger);
        try {
            return strategies.get("ai").suggest(order,candidates,trigger);
        } catch(Exception ex) {
            log.warn("AI routing unavailable for order {}. Falling back to rule-based strategy.",order.getId());
            RoutingResult fallback=strategies.get("rule").suggest(order,candidates,trigger);
            return new RoutingResult(fallback.recommendedAgent(),fallback.confidence(),
                    "AI unavailable; "+fallback.reasoning(),"rule",true);
        }
    }
}
