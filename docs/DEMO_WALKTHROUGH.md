# Five-minute portfolio demo walkthrough

1. **Open the dashboard** at http://localhost:5173. Point out five seeded agents, eight assigned orders and the empty queue.
2. **Trigger an event:** In the agent roster change Priya Sharma from BUSY to OFFLINE. Explain why `PATCH /agents/1/status` returns quickly and why work starts AFTER_COMMIT.
3. **Watch suggestions:** Wait for the automatic four-second refresh. Priya's orders appear as REASSIGNMENT_PENDING; the suggestion list shows AUTO RE-PLAN and a plain-English reason.
4. **Human checkpoint:** Click Accept and assign on one suggestion. Watch the order enter REASSIGNED and workload counters change; show that another suggestion remains until separately resolved.
5. **Demonstrate idempotency:** Choose OFFLINE again (no actual transition) or manually request the same order while a PENDING suggestion exists; there is no duplicate active suggestion.
6. **Strategy switch:** Choose Gemini AI in the routing panel. Without a key, trigger a manual recommendation on a different eligible order. The output shows AI fallback to rule-based, with explicit provenance.
7. **Engineering summary:** Show `RoutingStrategy`, `AgentStatusChangedListener`, transactional approval code, JUnit tests, and `ADR.md`.
8. **Close with limitations:** This is local H2 with in-process events and no authentication. For a production service, add PostgreSQL migrations, authentication, durable messaging/outbox and monitoring.

**Troubleshooting:** If no suggestions appear, ensure at least one AVAILABLE replacement exists, confirm the backend is running at localhost:8080, and inspect backend logs. OFFLINE recommendations only trigger for orders that were ASSIGNED to that agent.
