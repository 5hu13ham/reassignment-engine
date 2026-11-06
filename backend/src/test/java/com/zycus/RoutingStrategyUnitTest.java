package com.zycus;

import com.zycus.dto.RoutingResult;
import com.zycus.entity.*;
import com.zycus.service.RoutingService;
import com.zycus.strategy.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RoutingStrategyUnitTest {
    @Test
    void ruleChoosesLeastLoadedAvailableAgentWithStableTieBreak() {
        Agent busy=new Agent();busy.setId(1L);busy.setStatus(AgentStatus.BUSY);busy.setActiveOrderCount(0);
        Agent high=new Agent();high.setId(2L);high.setStatus(AgentStatus.AVAILABLE);high.setActiveOrderCount(5);
        Agent low=new Agent();low.setId(3L);low.setStatus(AgentStatus.AVAILABLE);low.setActiveOrderCount(1);
        RoutingResult result=new RuleBasedRoutingStrategy().suggest(new Order(),
                List.of(busy,high,low),TriggerReason.AGENT_OFFLINE);
        assertEquals(3L,result.recommendedAgent().getId());
        assertNull(result.confidence());
        assertEquals("rule",result.strategyUsed());
    }

    @Test
    void runtimeSwitchAndFallbackToRuleWhenAiFails() {
        RoutingStrategy rule=new RuleBasedRoutingStrategy();
        RoutingStrategy ai=new RoutingStrategy() {
            @Override public String strategyKey() {return "ai";}
            @Override public RoutingResult suggest(Order order,List<Agent> agents,TriggerReason reason) {
                throw new IllegalStateException("Offline AI");
            }
        };
        RoutingService service=new RoutingService(Map.of("rule",rule,"ai",ai),"rule");
        assertEquals("ai",service.setActiveStrategy("ai"));
        Agent agent=new Agent();agent.setId(4L);agent.setStatus(AgentStatus.AVAILABLE);agent.setActiveOrderCount(0);
        Order order=new Order();order.setId(9L);
        RoutingResult output=service.suggest(order,List.of(agent),TriggerReason.INITIAL);
        assertEquals(4L,output.recommendedAgent().getId());
        assertTrue(output.fallbackUsed());
        assertEquals("rule",output.strategyUsed());
        assertEquals("rule",service.setActiveStrategy("rule"));
    }
}
