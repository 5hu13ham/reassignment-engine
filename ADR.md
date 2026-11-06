# Architecture Decision Records — ZipRun Reassignment Engine

Each entry uses **Context → Options → Decision → Tradeoffs**. These document the implemented local demo, rather than hypothetical cloud infrastructure.

## ADR-001 — Routing logic belongs behind a Strategy interface

**Context:** Both operator-triggered suggestions and automatic OFFLINE recovery need the same recommendation operation. We need rule-based routing today and AI routing optionally, with a future zone-aware algorithm.

**Options:** Put routing in the HTTP controller; use a large `if/else` inside one service; use a `RoutingStrategy` interface with separately registered components.

**Decision:** `RoutingStrategy.suggest(Order, List<Agent>, TriggerReason)` is implemented by `RuleBasedRoutingStrategy` and `AiRoutingStrategy`. `RoutingService` selects one using a registry.

**Tradeoffs:** More types and configuration than a simple switch statement. The separation makes algorithm testing and adding a third strategy much easier.

## ADR-002 — Runtime strategy selection uses AtomicReference

**Context:** An operations user should switch from a deterministic rule to AI at runtime without restarting the backend.

**Options:** A fixed property read during startup; query a database setting each call; use an `AtomicReference<String>` pointing at registered strategy keys.

**Decision:** Use an `AtomicReference` and expose `PATCH /routing/strategy`; read the key at invocation time, validate against the strategy registry.

**Tradeoffs:** Selection resets on restart, is not distributed to other replicas and the route has no authentication in this local demonstration. A multi-instance system would need centrally managed config and an authorized admin API.

## ADR-003 — Agent status changes create post-commit async work

**Context:** An OFFLINE event must not re-plan orders against a database change that later rolls back, and it must not block the PATCH request on an LLM.

**Options:** Re-plan synchronously in `AgentService`; poll the database on a timer; use `ApplicationEventPublisher` with `@TransactionalEventListener(AFTER_COMMIT)` and `@Async`.

**Decision:** Publish `AgentStatusChangedEvent` inside the status transaction. After commit, a bounded worker finds affected orders and invokes `RecommendationService` separately per order.

**Tradeoffs:** In-process events are **not durable**. If the app crashes after commit but before processing, event work may be lost. Production should use transactional outbox + durable queue or Kafka and an idempotent consumer. A bounded executor reduces but does not eliminate overload risks.

## ADR-004 — Suggestions are idempotent and approvals are transactional

**Context:** A repeated OFFLINE event or competing HTTP calls must not create two active suggestions, and two operators must not approve inconsistent replacements.

**Options:** Check before insert without locks; cache duplicate requests in memory; serialize by order database row and enforce transition checks.

**Decision:** `RecommendationService` obtains a pessimistic write lock on the order, checks for an existing PENDING suggestion, and persists only when absent. `ReassignmentSuggestionService` locks the order before changing its assignment, verifies the suggestion and proposed agent, updates workloads and resolves competing suggestions within one transaction.

**Tradeoffs:** Row locks reduce throughput and may deadlock if extended to more entities without a consistent lock ordering policy. Database locks are appropriate for this low-volume demo. A larger fleet needs performance testing, retryable transaction conflicts, and database constraints.

## ADR-005 — AI augments a deterministic, safe fallback

**Context:** LLM requests may be slow, unavailable or return invalid agent identifiers. A stranded order must not disappear just because Gemini is down.

**Options:** Require AI availability; quietly ignore AI exceptions; use rule-based routing when AI fails and explicitly label fallback provenance.

**Decision:** Default to rule-based routing without a key. Gemini is optional; validate JSON fields and recommended-agent membership in the eligible roster. `RoutingService` catches AI errors and returns a visibly marked rule-based result. Prompts distinguish manual selection from OFFLINE recovery.

**Tradeoffs:** The fallback is simple least-load, not zone- or SLA-optimal. AI confidence is subjective and should not be interpreted as a probability of successful delivery.

## ADR-006 — Human-in-the-loop approval is mandatory

**Context:** An automated suggestion is reversible, but actually changing an assigned delivery order affects live operations.

**Options:** Auto-assign whenever the model is confident; require a human acceptance step; approve automatically under configurable constraints.

**Decision:** Keep `ReassignmentSuggestion` in PENDING status until an operator explicitly ACCEPTS or REJECTS. ACCEPTED atomically changes the order and agent counts. REJECTED leaves the original assignment intact and, if the agent remains offline, the order still needs recovery.

**Tradeoffs:** Reassignment throughput depends on operators. This is intentional in the first iteration; automatic approval requires separately measured safety thresholds and business rules.

## ADR-007 — Local-first deployment, JPA, and transparent exclusions

**Context:** A portfolio reviewer should run the end-to-end flow quickly without signing up for a cloud provider.

**Options:** PostgreSQL + cloud infra; MongoDB; JPA with an ephemeral H2 database and optional future relational persistence.

**Decision:** Use H2, Spring Data JPA, seeded agents and orders, and a separate React dashboard. Automated tests exercise the core workflow.

**Tradeoffs:** Data resets on each restart. Authentication, audit logging, durable queues, SLA timers, capacity and geographic optimization are explicitly deferred rather than falsely presented as production-ready.
