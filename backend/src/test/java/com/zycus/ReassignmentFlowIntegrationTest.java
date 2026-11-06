package com.zycus;

import com.zycus.dto.*;
import com.zycus.entity.*;
import com.zycus.repository.*;
import com.zycus.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReassignmentFlowIntegrationTest {
    @Autowired private AgentService agentService;
    @Autowired private OrderService orderService;
    @Autowired private RecommendationService recommendationService;
    @Autowired private ReassignmentSuggestionService suggestionService;
    @Autowired private AgentRepository agents;
    @Autowired private OrderRepository orders;
    @Autowired private ReassignmentSuggestionRepository suggestions;
    @Autowired private RoutingService routing;

    @Test
    void offlineEventQueuesSuggestionAndApprovalReassignsOrder() throws Exception {
        Agent original=new Agent();
        original.setName("Assigned test agent");
        original.setStatus(AgentStatus.AVAILABLE);
        original.setActiveOrderCount(0);
        original=agents.save(original);
        Long orderId=orderService.createOrder(new CreateOrderRequest("Demo package",original.getId())).id();

        agentService.updateAgentStatus(original.getId(),new UpdateAgentStatusRequest(AgentStatus.OFFLINE));
        ReassignmentSuggestion pending=awaitPending(orderId);
        assertEquals(TriggerReason.AGENT_OFFLINE,pending.getTriggerReason());
        assertEquals(OrderStatus.REASSIGNMENT_PENDING,orders.findById(orderId).orElseThrow().getStatus());
        assertNotEquals(original.getId(),pending.getRecommendedAgent().getId());

        suggestionService.updateSuggestionStatus(pending.getId(),
                new UpdateSuggestionStatusRequest(SuggestionStatus.ACCEPTED));
        Order moved=orders.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.REASSIGNED,moved.getStatus());
        assertEquals(pending.getRecommendedAgent().getId(),moved.getAssignedAgent().getId());
        assertEquals(0,agents.findById(original.getId()).orElseThrow().getActiveOrderCount());

        assertThrows(RuntimeException.class,()->suggestionService.updateSuggestionStatus(
                pending.getId(),new UpdateSuggestionStatusRequest(SuggestionStatus.ACCEPTED)));
    }

    @Test
    void repeatedOfflineAndManualSuggestionsDoNotDuplicatePendingApproval() throws Exception {
        Agent original=new Agent();
        original.setName("Duplicate check");
        original.setStatus(AgentStatus.AVAILABLE);
        original.setActiveOrderCount(0);
        original=agents.save(original);
        Long id=original.getId();
        Long orderId=orderService.createOrder(new CreateOrderRequest("Test order",id)).id();
        agentService.updateAgentStatus(id,new UpdateAgentStatusRequest(AgentStatus.OFFLINE));
        ReassignmentSuggestion first=awaitPending(orderId);
        agentService.updateAgentStatus(id,new UpdateAgentStatusRequest(AgentStatus.OFFLINE));
        ReassignmentSuggestion second=recommendationService.generateRecommendation(orderId,TriggerReason.INITIAL);
        assertEquals(first.getId(),second.getId());
        assertEquals(1,suggestions.findByOrderIdAndStatus(orderId,SuggestionStatus.PENDING).size());
        suggestionService.updateSuggestionStatus(first.getId(),
                new UpdateSuggestionStatusRequest(SuggestionStatus.REJECTED));
        assertEquals(OrderStatus.REASSIGNMENT_PENDING,orders.findById(orderId).orElseThrow().getStatus());
    }

    @Test
    void runtimeSwitchToAiFallsBackWithoutKey() {
        Agent original=new Agent();
        original.setName("Strategy source");
        original.setStatus(AgentStatus.AVAILABLE);
        original.setActiveOrderCount(0);
        original=agents.save(original);
        Long id=orderService.createOrder(new CreateOrderRequest("AI fallback demo",original.getId())).id();
        routing.setActiveStrategy("ai");
        ReassignmentSuggestion result=recommendationService.generateRecommendation(id,TriggerReason.INITIAL);
        assertEquals("rule",result.getStrategyUsed());
        assertTrue(result.isFallbackUsed());
        routing.setActiveStrategy("rule");
    }

    private ReassignmentSuggestion awaitPending(Long orderId) throws Exception {
        for(int i=0;i<100;i++) {
            var found=suggestions.findFirstByOrderIdAndStatus(orderId,SuggestionStatus.PENDING);
            if(found.isPresent()) return found.get();
            Thread.sleep(100);
        }
        fail("Asynchronous offline suggestion was not generated in 10 seconds");
        return null;
    }
}
