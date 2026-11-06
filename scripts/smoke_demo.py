#!/usr/bin/env python3
"""HTTP-level smoke test for local Docker Compose demo, no non-stdlib dependencies."""
import json
import time
from urllib.request import Request, urlopen
from urllib.error import HTTPError

API = "http://127.0.0.1:8080"
UI = "http://127.0.0.1:5173"

def call(url, method="GET", payload=None):
    data = None if payload is None else json.dumps(payload).encode()
    headers = {"Content-Type": "application/json"} if data is not None else {}
    with urlopen(Request(url, data=data, headers=headers, method=method), timeout=10) as response:
        raw = response.read()
        return json.loads(raw) if raw and "json" in response.headers.get("Content-Type", "").lower() else raw

def ready():
    for _ in range(65):
        try:
            health = call(API + "/actuator/health")
            if health.get("status") == "UP" and b"html" in call(UI).lower():
                return
        except Exception:
            time.sleep(3)
    raise AssertionError("Docker services did not become healthy")

ready()
agents = call(API + "/agents")
orders = call(API + "/orders")
assert len(agents) >= 5 and len(orders) >= 8, (len(agents), len(orders))
original = next(a for a in agents if a["name"] == "Priya Sharma")
assert original["status"] == "BUSY"
assigned = [o for o in orders if o["assignedAgentId"] == original["id"]]
assert assigned, "Expected seeded orders assigned to the first agent"

call(API + f"/agents/{original['id']}/status", "PATCH", {"status": "OFFLINE"})
suggestions = []
for _ in range(60):
    suggestions = call(API + "/suggestions?status=PENDING")
    if any(s["orderId"] == assigned[0]["id"] for s in suggestions):
        break
    time.sleep(1)
pending = next(s for s in suggestions if s["orderId"] == assigned[0]["id"])
assert pending["triggerReason"] == "AGENT_OFFLINE"
assert pending["strategyUsed"] == "rule"
assert pending["confidence"] is None
call(API + f"/agents/{original['id']}/status", "PATCH", {"status": "OFFLINE"})
call(API + f"/suggestions/{pending['id']}", "PATCH", {"status": "ACCEPTED"})
moved = next(o for o in call(API + "/orders") if o["id"] == assigned[0]["id"])
assert moved["status"] == "REASSIGNED"
assert moved["assignedAgentId"] != original["id"]

call(API + "/routing/strategy", "PATCH", {"strategy": "ai"})
result = call(API + "/orders/3/suggest", "POST")
assert result["fallbackUsed"] and result["strategyUsed"] == "rule"
print("Docker smoke passed: HTTP, seeded agents, async event, approval and Gemini fallback")
