# ZipRun — AI Reassignment Engine

**Event-driven delivery dispatch with human-in-the-loop reassignment.**

Java 21 · Spring Boot 4 · Spring Data JPA · H2 · Optional Gemini · React 18 · Vite · GitHub Actions

A full-stack **engineering portfolio demonstration** of what happens when a delivery agent suddenly becomes unavailable: the system identifies stranded orders, recommends replacement agents asynchronously using interchangeable routing strategies, and waits for an operations user to approve any reassignment.

> This is a **local demonstration**, not a production dispatch service. There is no authentication on the example API, and the H2 dataset is intentionally reset on restart. Bind it to localhost only.

## The core workflow

```text
PATCH /agents/{id}/status  {"status":"OFFLINE"}
        |
  Database transaction commits
        |
  AFTER_COMMIT + @Async event listener
        |
  Find active orders assigned to offline agent
        |
  RecommendationService (one transaction / locked order)
        |
  Rule-based strategy or Gemini AI + rule fallback
        |
  Persist PENDING ReassignmentSuggestion
        |
  React dashboard polls /suggestions
        |
  Operator ACCEPTS or REJECTS
        |
  ACCEPT -> atomically reassign order + update workloads
```

## Features

- Agent roster with AVAILABLE, BUSY and OFFLINE statuses.
- Automatic, **post-commit asynchronous** re-planning when an assigned agent goes offline.
- Idempotent suggestion creation with per-order database row locking.
- Rule-based routing using least current workload with stable ties.
- Optional Gemini recommendation with validated JSON, agent eligibility check and deterministic fallback.
- Runtime strategy switching via `GET/PATCH /routing/strategy`, without restarting the server.
- Human approval/rejection; accepts update order assignment and workload counts within a transaction.
- REST APIs and a responsive React operations dashboard that refreshes every four seconds.
- Seeded example agents/orders, H2 local database, JUnit integration and unit tests, GitHub Actions CI.
- Separate [architecture decision record](ADR.md) and [engineering interview handbook](docs/ENGINEERING_INTERVIEW_HANDBOOK.md).

## Quick start (two terminals)

Requirements: **Java 21**, **Maven 3.9+**, **Node.js 20+**.

```bash
git clone https://github.com/5hu13ham/reassignment-engine.git
cd reassignment-engine
git switch feature/complete-reassignment-portfolio-2026-11-06
```

Terminal 1:

```bash
cd backend
mvn spring-boot:run
```

Terminal 2:

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. The API is at **http://localhost:8080**. No LLM key is needed: `routing.strategy=rule` is the default.

### Optional Gemini

Set `LLM_API_KEY` as an environment variable on the backend machine, then choose **Gemini AI** in the UI. The app uses the `gemini-flash-latest` model identifier by default; customize it with `LLM_MODEL`. If Gemini times out, returns invalid JSON, proposes an unavailable agent, or the key is absent, routing falls back to the least-load rule and marks the suggestion accordingly.

Do not put API keys in source files or commit them.

### Docker Compose alternative

```bash
docker compose up --build
```

Open the UI at http://localhost:5173. The H2 data is intentionally ephemeral. Ports are bound to **127.0.0.1**.

## 60-second walkthrough

1. Open the dashboard and note the available agent roster and assigned orders.
2. Set **Priya Sharma** to **OFFLINE** in the agent roster.
3. The status API responds without waiting on the AI; the background process finds her assigned orders and queues suggestions.
4. Within a refresh cycle, the suggestions appear with **AUTO RE-PLAN** badges and rule/AI rationale.
5. Click **Accept and assign** on one suggestion. Observe the order status change to **REASSIGNED** and agent workload counters update.
6. Switch to **Gemini AI** without restarting. Without a key, request a suggestion and observe the recorded **AI fallback**.
7. Repeat an OFFLINE status update. No duplicate pending suggestions are created for the same order.

More detailed steps: [docs/DEMO_WALKTHROUGH.md](docs/DEMO_WALKTHROUGH.md).

## REST API

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/agents` | Show agent roster |
| PATCH | `/agents/{id}/status` | Set AVAILABLE / BUSY / OFFLINE |
| GET | `/orders?status=ASSIGNED` | List/filter orders |
| POST | `/orders` | Create order pre-assigned to an online agent |
| POST | `/orders/{id}/suggest` | Manually request/reuse a recommendation |
| GET | `/suggestions?status=PENDING` | List suggestions awaiting ops |
| PATCH | `/suggestions/{id}` | Accept or reject suggestion |
| GET | `/routing/strategy` | Show selected strategy |
| PATCH | `/routing/strategy` | Switch to `rule` or `ai` at runtime |
| GET | `/actuator/health` | Health probe |

Examples:

```bash
curl http://localhost:8080/agents

curl -X PATCH http://localhost:8080/agents/1/status \
  -H 'Content-Type: application/json' -d '{"status":"OFFLINE"}'

curl http://localhost:8080/suggestions?status=PENDING

curl -X PATCH http://localhost:8080/suggestions/1 \
  -H 'Content-Type: application/json' -d '{"status":"ACCEPTED"}'

curl -X PATCH http://localhost:8080/routing/strategy \
  -H 'Content-Type: application/json' -d '{"strategy":"ai"}'
```

IDs change if orders/agents are added. Open the roster to confirm IDs before changing them.

## Quality checks

```bash
mvn -B -ntp -f backend/pom.xml clean verify
cd frontend && npm install && npm run build
```

GitHub Actions executes backend tests and compiles the frontend on branch pushes and pull requests.

## Design decisions and limitations

- **H2** keeps the demo zero-setup and disposable; the JPA repository abstraction permits a later PostgreSQL adapter and migrations.
- **In-process async events** are immediate and easy to understand, but they are not durable across process crashes and cannot replace Kafka/outbox/event queues.
- **Pessimistic order locking** prevents duplicate pending suggestions and conflicting approvals when all handlers use the repository methods. Locking order first ensures consistent business transitions.
- **AI confidence is not an SLA or an accuracy probability.** The rule-based strategy reports no numerical confidence; the AI suggestion remains subject to human approval.
- **Runtime strategy selection is in memory**, not shared between application replicas.
- **No authentication, capacity or zone-aware routing, durable worker retries, metrics, audit trail, or SLA monitoring.** These are clear future extensions, not claimed features.
- **Only AVAILABLE agents** can be recommended as replacements. BUSY is not treated as available capacity in this first sprint.

For rationale and tradeoffs, see [ADR.md](ADR.md).

**Built by Shubham Kumar** · Software engineering portfolio
