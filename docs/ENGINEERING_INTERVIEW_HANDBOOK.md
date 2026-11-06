# Engineering Interview Handbook — ZipRun AI Reassignment Engine

**Purpose:** Learn how this project works from request to database, explain each decision confidently, and practice interview questions from the code itself.

**Architecture:** Java 21 · Spring Boot 4 · REST · JPA/H2 · Spring application events · `@Async` · Strategy pattern · optional Gemini · React.

## 1. The 60-second explanation

"I built an event-driven delivery reassignment platform. When an agent goes offline, Spring publishes a domain event during the database transaction. A post-commit asynchronous listener finds affected orders and calls a recommendation service. A pluggable strategy engine selects the least-loaded available agent or optionally asks Gemini, with validation and rule-based fallback. A suggested replacement goes into a human approval queue; only an accepted suggestion atomically updates the assigned agent, order state and workload counters. I used pessimistic locking and idempotency checks, unit/integration tests, and a React operations dashboard."

## 2. Follow the request: OFFLINE to reassigned

| Step | Source class | What happens | Concept to explain |
|---|---|---|---|
| 1 | `AgentController` | PATCH status input is mapped from JSON to a validated DTO. | REST, DTO, Bean Validation |
| 2 | `AgentService` | Within `@Transactional`, persist the new status and publish an event only on a real status transition. | Transaction boundary, change detection |
| 3 | `AgentStatusChangedEvent` | Immutable event record carries agent ID, previous status and current status. | Domain events, Java records |
| 4 | `AgentStatusChangedListener` | `@TransactionalEventListener(AFTER_COMMIT)` fires only after successful commit; `@Async` moves processing to bounded threads. | Event-driven architecture, async, consistency |
| 5 | `OrderService` | Finds ASSIGNED orders for the offline agent, producing a list of affected IDs. | Repository query, domain filtering |
| 6 | `RecommendationService` | Locks the order row, checks state and existing PENDING suggestion, marks it REASSIGNMENT_PENDING. | Pessimistic locking, idempotency, state machine |
| 7 | `RoutingService` | Reads selected strategy key and invokes rule or AI; AI failure falls back to rule. | Strategy Pattern, AtomicReference, fault tolerance |
| 8 | `AiRoutingStrategy` | Calls Gemini only if selected; JSON is validated and agent ID checked against eligible agents. | LLM adapter, output validation |
| 9 | `ReassignmentSuggestionRepository` | Persists a PENDING suggestion with trigger, reasoning, engine and fallback metadata. | Persistence, provenance, auditability |
| 10 | `App.tsx` | React polls pending suggestions and displays human-readable reasons. | Polling, hooks, UI state |
| 11 | `ReassignmentSuggestionService` | Operator approval locks the order, validates current state/availability and updates order + both workload counts. | ACID, consistency, authorization checkpoint |
| 12 | `Spring Data JPA` | Commits the multi-entity transition or rolls it back together on error. | Unit of Work, dirty checking |

## 3. Important code patterns to understand

### A. Why publish events inside a transaction but handle AFTER_COMMIT?

Bad flow: update an agent, immediately launch async routing, then database update rolls back. Now a suggestion references an OFFLINE state that never existed.

Correct flow: status update commits, *then* the listener starts. A Spring application event is **not** Kafka; it is in-process notification and may be lost on restart.

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Async("reassignmentExecutor")
public void onAgentStatusChanged(AgentStatusChangedEvent event) { ... }
```

**Interview follow-up:** "What if the application crashes between commit and event processing?" Answer: an in-process event is not durable; add a transactional outbox table + reliable publisher and an idempotent consumer.

### B. What does the Strategy pattern buy us?

```java
public interface RoutingStrategy {
    String strategyKey();
    RoutingResult suggest(Order order, List<Agent> candidates, TriggerReason trigger);
}
```

Different algorithms implement the same contract. `RoutingService` selects an implementation without the controller or listener knowing its internals. A future `ZoneAffinityStrategy` can be registered without modifying the use cases.

**SOLID:** Open/Closed Principle (new algorithms via implementations), Dependency Inversion (call interface, not a concrete AI client), Single Responsibility (routing separate from persistence).

### C. Why AtomicReference rather than a final String?

The chosen strategy can change *while the JVM is running*. `AtomicReference` provides a thread-safe, immediately visible pointer to the selection. Its value is **not persisted**, and each JVM would have its own copy.

### D. What is idempotency here?

If the same OFFLINE transition or two requests trigger a suggestion for the same order, **at most one PENDING suggestion** should exist. A query alone has a race:

```text
Thread A: no pending suggestion
Thread B: no pending suggestion
Thread A: INSERT pending
Thread B: INSERT pending  <-- duplicate
```

Our service first locks the order row with `PESSIMISTIC_WRITE`, then checks and inserts within a transaction. Concurrent handlers serialize on that order ID. Repeat requests return the existing suggestion.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select o from Order o join fetch o.assignedAgent where o.id = :id")
Optional<Order> findByIdForUpdate(@Param("id") Long id);
```

**Interview follow-up:** "Is this sufficient for huge scale?" Not by itself. Consider lock contention, transaction timeouts, unique business constraints, retries, optimistic locking and durable message deduplication.

### E. Why an approval transaction?

Approval changes **more than one entity**: the suggestion, the order's assigned agent/status, and the old/new agents' active-order counts. Either the entire set succeeds or it rolls back. Never mark a suggestion ACCEPTED before verifying that the recommended agent is still AVAILABLE.

**ACID:** Atomicity, Consistency, Isolation, Durability. H2 makes this convenient for a demo; the same design pattern applies to a relational production database with correct isolation/locking.

### F. What does the AI fallback do?

Only the **AI branch** is allowed to fail over. Rule-based routing remains deterministic, chooses AVAILABLE agents with minimum active count, and uses agent ID as a stable tie-breaker. Gemini output is untrusted until parsed and the chosen ID is verified against a server-generated eligible list.

A rule result uses `confidence=null` because determinism is **not** the same as a probabilistic success prediction. The `fallbackUsed` flag appears in the API/UI so ops can see why a recommendation was made.

### G. Why separate listener, service and repository?

- **Controller:** HTTP concerns and request DTOs.
- **Event listener:** asynchronous orchestration and fault isolation.
- **Service:** business invariants and transactional work.
- **Strategy:** recommendation algorithm.
- **Repository:** persistence abstraction.
- **Mapper:** controlled API response shape.

This separation lets tests isolate business logic and avoids hiding routing/persistence in controller methods.

## 4. Business state machines

**Agent:** AVAILABLE / BUSY / OFFLINE. Only AVAILABLE agents are chosen as replacement candidates.

**Order:** ASSIGNED → REASSIGNMENT_PENDING (after offline) → REASSIGNED (on approval) → DELIVERED (future endpoint). If no replacement exists, the stranded order remains REASSIGNMENT_PENDING for retry.

**Suggestion:** PENDING → ACCEPTED or REJECTED; a resolved suggestion cannot be accepted again. Rejecting an offline recovery leaves the order needing attention rather than silently marking it healthy.

## 5. Endpoints worth demonstrating

```http
PATCH /agents/1/status
Content-Type: application/json
{"status":"OFFLINE"}
```

```http
GET /suggestions?status=PENDING
```

```http
PATCH /suggestions/1
Content-Type: application/json
{"status":"ACCEPTED"}
```

```http
PATCH /routing/strategy
Content-Type: application/json
{"strategy":"ai"}
```

The frontend calls these same REST endpoints.

## 6. Interview question bank (answer each in 2–3 sentences)

**Q1. Difference between `@EventListener` and `@TransactionalEventListener`?** The first observes the published event according to normal application event dispatch. The second is bound to a transaction phase (AFTER_COMMIT here), preventing processing if that transaction rolls back.

**Q2. What happens if async processing throws?** The listener catches errors per affected order so one failure doesn't stop other orders. For a production platform, a durable retry queue and DLQ would be required.

**Q3. What if Gemini returns a non-existent agent?** `AiRoutingStrategy` checks the recommendation against the current eligible agents and raises an exception. `RoutingService` catches this and produces a rule-based recommendation.

**Q4. How do you avoid duplicate suggestions?** Acquire a pessimistic row lock per order, check the PENDING suggestion inside the transaction, and return it if present.

**Q5. Why not assign immediately when an agent goes offline?** The business process explicitly requires an operations checkpoint. Recommendations have operational side effects only after human approval.

**Q6. What would Kafka add?** Durable events, consumer groups, replay/retries and decoupling across services. This demonstration uses in-process Spring events, not Kafka, so it doesn't claim those guarantees.

**Q7. What is the cost of row locking?** Lower throughput under high contention, possible deadlocks and transaction timeouts. Short transactions and consistent lock ordering are essential.

**Q8. What changes to support thousands of agents?** Paginated queries, agent capacity/zone indexes, asynchronous durable workers, metrics, a database constraint on active suggestions, and load testing.

**Q9. How is configuration switched without restart?** A validated REST call updates the `AtomicReference<String>` strategy key. The selection is thread-safe but resets on process restart.

**Q10. Why is an LLM confidence score not guaranteed accuracy?** It is generated by the model and isn't calibrated against delivery outcomes. The final selection remains a human decision.

**Q11. What if the selected agent goes OFFLINE while ops are approving?** The approval service rechecks agent status under a database lock and rejects the stale recommendation.

**Q12. Why H2 instead of PostgreSQL today?** Zero setup and a repeatable synthetic demo. A real system would use PostgreSQL plus migrations and appropriate transaction isolation.

**Q13. What's the difference between optimistic and pessimistic locking?** Optimistic locking detects concurrent updates at commit (often via `@Version`); pessimistic locking blocks conflicting transactions while work is in progress.

**Q14. How does React show async suggestions?** It polls the REST API every four seconds, updates component state and shows actions and errors. Server-Sent Events/WebSockets are optional extensions when real-time scale matters.

**Q15. What is the main security limitation?** The demo API has no user authentication or ops-role authorization, so it must remain local. Before any public deployment, add authentication, authorization, audit events, rate limiting and strict CORS/CSRF policies.

## 7. Known non-goals — answer honestly

This repository does **not** implement Kafka, production-grade outbox, audit trail, user identity management, multi-region deployment, SLA prediction, route maps, rate limiting, zone-aware dispatch, or real delivery tracking. These are deliberate follow-ups, not hidden features.

## 8. One-line summary of every major change

| Change | Why | Keyword |
|---|---|---|
| Consolidated duplicate event types | Fixes missing listener delivery | Event contract |
| Added AFTER_COMMIT async handler | Only re-plan committed OFFLINE events, off request thread | Transactional events |
| Added bounded executor | Prevent unlimited background threads | Backpressure |
| Fixed recommendation service invocation | Connects event to proper trigger reason | Type safety |
| Added per-order lock and pending check | Prevent duplicate suggestions | Idempotency |
| Implemented approval updating 3 entities | Makes reassignments real and atomic | ACID |
| Updated active order counters | Keeps workload-based routing meaningful | Data consistency |
| Added manual suggestion route | Both HTTP and events use one routing contract | Reuse |
| Added runtime strategy endpoint | Toggle rule/AI without restart | AtomicReference |
| Validated Gemini output | Never accept hallucinated agent IDs/confidence | Trust boundaries |
| Made API key optional and fallback visible | Reliable offline demo | Fault tolerance |
| Added React ops board and polling | Visualizes the human checkpoint | Hooks, REST |
| Added H2 seed dataset and tests | Repeatable interview demonstration | Integration testing |
| Added CI and Docker | Reproducible local setup | CI/CD |
| Added ADR and this handbook | Explain tradeoffs in interviews | Architecture ownership |
