import { useCallback, useEffect, useState, type FormEvent } from "react";

type AgentStatus = "AVAILABLE" | "BUSY" | "OFFLINE";
type OrderStatus = "ASSIGNED" | "REASSIGNMENT_PENDING" | "REASSIGNED" | "DELIVERED";
type SuggestionStatus = "PENDING" | "ACCEPTED" | "REJECTED";

type Agent = { id: number; name: string; activeOrderCount: number; status: AgentStatus };
type Order = { id: number; description: string; assignedAgentId: number; status: OrderStatus };
type Suggestion = {
  id: number; orderId: number; recommendedAgentId: number; confidence: number | null;
  reasoning: string; status: SuggestionStatus; triggerReason: "INITIAL" | "AGENT_OFFLINE";
  strategyUsed: string; fallbackUsed: boolean;
};
type StrategyState = { activeStrategy: string };
type ApiError = { message?: string; error?: string };

const API = import.meta.env.VITE_API_BASE_URL || "";

async function request<T>(url: string, method = "GET", body?: object): Promise<T> {
  const response = await fetch(API + url, {
    method,
    headers: body ? { "Content-Type": "application/json" } : {},
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    let detail: ApiError = {};
    try { detail = await response.json() as ApiError; } catch { /* fallback below */ }
    throw new Error(detail.message || detail.error || "Request failed: HTTP " + response.status);
  }
  return await response.json() as T;
}
const orderLabel: Record<OrderStatus, string> = {
  ASSIGNED: "Assigned", REASSIGNMENT_PENDING: "Needs reassignment",
  REASSIGNED: "Reassigned", DELIVERED: "Delivered",
};

export default function App() {
  const [agents,setAgents] = useState<Agent[]>([]);
  const [orders,setOrders] = useState<Order[]>([]);
  const [suggestions,setSuggestions] = useState<Suggestion[]>([]);
  const [strategy,setStrategy] = useState("rule");
  const [busy,setBusy] = useState(false);
  const [error,setError] = useState("");
  const [message,setMessage] = useState("");
  const [description,setDescription] = useState("");
  const [assignedId,setAssignedId] = useState("");
  const [lastUpdated,setLastUpdated] = useState<Date | null>(null);

  const refresh = useCallback(async () => {
    try {
      const [allAgents, allOrders, allSuggestions, active] = await Promise.all([
        request<Agent[]>("/agents"),
        request<Order[]>("/orders"),
        request<Suggestion[]>("/suggestions?status=PENDING"),
        request<StrategyState>("/routing/strategy"),
      ]);
      setAgents(allAgents);setOrders(allOrders);setSuggestions(allSuggestions);
      setStrategy(active.activeStrategy);setLastUpdated(new Date());setError("");
    } catch(e) {setError(e instanceof Error ? e.message : "Unable to reach the API");}
  }, []);

  useEffect(() => {
    void refresh();
    const poll=window.setInterval(()=>{void refresh();},4000);
    return ()=>window.clearInterval(poll);
  },[refresh]);

  async function mutate(operation:()=>Promise<unknown>,success:string) {
    setBusy(true);setError("");
    try {
      await operation();
      setMessage(success);
      await refresh();
    } catch(e) {setError(e instanceof Error?e.message:"Operation failed");}
    finally {setBusy(false);}
  }

  function createOrder(event:FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!assignedId) return;
    void mutate(async()=>{
      await request("/orders","POST",{description,assignedAgentId:Number(assignedId)});
      setDescription("");
    },"Order created and assigned.");
  }

  const pendingOrders=orders.filter(o=>o.status==="REASSIGNMENT_PENDING");
  const available=agents.filter(a=>a.status==="AVAILABLE");
  const orderedAgents=[...agents].sort((a,b)=>a.id-b.id);
  const agentName=(id:number)=>agents.find(a=>a.id===id)?.name || "Agent #"+id;
  const orderName=(id:number)=>orders.find(o=>o.id===id)?.description || "Order #"+id;

  return <div className="app">
    <header className="top">
      <a href="/" className="brand"><span className="brand-square">Z</span>
        <span><strong>ZipRun</strong><small>REASSIGNMENT CONTROL CENTER</small></span></a>
      <div className="header-side"><span className="live-dot"/> LOCAL PORTFOLIO DEMO <span className="separator">•</span> JAVA 21 / REACT</div>
    </header>
    <main className="wrap">
      <section className="hero">
        <div><div className="eyebrow">OPS / INTELLIGENT DISPATCH</div>
          <h1>Keep deliveries <em>moving.</em></h1>
          <p>An event-driven dispatch demo. When an agent goes offline, the engine prepares a replacement recommendation. You approve the final move.</p>
        </div>
        <div className="workflow">
          <span>01 AGENT OFFLINE</span><b>→</b><span>02 AUTO RE-PLAN</span><b>→</b><span>03 HUMAN APPROVAL</span>
        </div>
      </section>
      {(error||message)&&<div className={error?"notice error":"notice success"} role="status">
        <span>{error||message}</span><button onClick={()=>{setError("");setMessage("");}} aria-label="Dismiss message">×</button>
      </div>}
      <section className="stats">
        <article><small>OPEN ORDERS</small><strong>{orders.filter(o=>o.status!=="DELIVERED").length}</strong><span>Dispatch workload</span></article>
        <article><small>AVAILABLE AGENTS</small><strong>{available.length}<i> / {agents.length}</i></strong><span>Ready for re-routing</span></article>
        <article className="attention"><small>PENDING APPROVALS</small><strong>{suggestions.length}</strong><span>Human checkpoint</span></article>
        <article><small>STRANDED ORDERS</small><strong>{pendingOrders.length}</strong><span>Awaiting reassignment</span></article>
      </section>
      <div className="main-grid">
        <div className="left-column">
          <section className="panel">
            <div className="panel-heading"><div><div className="eyebrow">EVENT SOURCE</div><h2>Agent roster</h2></div>
              <button className="outline" onClick={()=>void refresh()} disabled={busy}>↻ Refresh</button></div>
            <p className="panel-sub">Set a busy or available agent to offline to trigger the asynchronous recovery loop.</p>
            <div className="roster">
              {orderedAgents.map(agent=><div className="agent" key={agent.id}>
                <div className="avatar">{agent.name.split(" ").slice(0,2).map(s=>s[0]).join("")}</div>
                <div className="agent-main"><strong>{agent.name}</strong><small>#{agent.id} · {agent.activeOrderCount} active orders</small></div>
                <span className={"state state-"+agent.status.toLowerCase()}>{agent.status}</span>
                <select aria-label={"Change status for "+agent.name} disabled={busy} value={agent.status}
                  onChange={e=>void mutate(()=>request("/agents/"+agent.id+"/status","PATCH",{status:e.target.value}),
                    "Agent status updated. Offline re-planning runs in the background.")}>
                  <option value="AVAILABLE">Available</option><option value="BUSY">Busy</option><option value="OFFLINE">Offline</option>
                </select>
              </div>)}
              {!agents.length&&<div className="empty">No agents loaded. Check the backend connection.</div>}
            </div>
          </section>
          <section className="panel">
            <div className="panel-heading"><div><div className="eyebrow">CONTROLLED EXPERIMENT</div><h2>Routing strategy</h2></div>
              <span className="pill">{strategy.toUpperCase()}</span></div>
            <p className="panel-sub">Switch strategies at runtime. Without a Gemini API key, AI mode falls back automatically to the deterministic rule.</p>
            <div className="strategy-options">
              {["rule","ai"].map(option=><button key={option} disabled={busy}
                className={strategy===option?"selected":""}
                onClick={()=>void mutate(()=>request("/routing/strategy","PATCH",{strategy:option}),"Routing strategy switched.")}>
                {option==="rule"?"◇ Least-load rule":"✦ Gemini AI"}
              </button>)}
            </div>
          </section>
          <section className="panel">
            <div className="panel-heading"><div><div className="eyebrow">SIMULATION INPUT</div><h2>Create a delivery order</h2></div></div>
            <form className="order-form" onSubmit={createOrder}>
              <label>Description<input value={description} onChange={e=>setDescription(e.target.value)} required maxLength={500} placeholder="Example: Package from Indiranagar to HSR Layout"/></label>
              <label>Assigned agent<select value={assignedId} required onChange={e=>setAssignedId(e.target.value)}>
                <option value="">Select an online agent</option>
                {agents.filter(a=>a.status!=="OFFLINE").map(a=><option key={a.id} value={a.id}>{a.name} ({a.status})</option>)}
              </select></label>
              <button className="primary" disabled={busy}>Create order</button>
            </form>
          </section>
        </div>
        <div className="right-column">
          <section className="panel suggestion-panel">
            <div className="panel-heading"><div><div className="eyebrow">HUMAN-IN-THE-LOOP</div><h2>Recommendation queue</h2></div>
              <span className="count">{suggestions.length}</span></div>
            <p className="panel-sub">Suggestions never update delivery assignments until you accept them.</p>
            <div className="suggestions">
              {suggestions.map(s=><article className="suggestion" key={s.id}>
                <div className="suggestion-top"><span className={"trigger "+(s.triggerReason==="AGENT_OFFLINE"?"automatic":"manual")}>
                  {s.triggerReason==="AGENT_OFFLINE"?"⚡ AUTO RE-PLAN":"◇ MANUAL"}
                </span><span>#{s.id} · Order {s.orderId}</span></div>
                <h3>{orderName(s.orderId)}</h3>
                <p className="recommended">Recommended <strong>{agentName(s.recommendedAgentId)}</strong></p>
                <p className="reason">{s.reasoning}</p>
                <div className="meta"><span>{s.fallbackUsed?"↪ AI fallback":s.strategyUsed.toUpperCase()+" strategy"}</span>
                  <span>{s.confidence==null?"Rule-based · no probability":"AI estimate "+Math.round(s.confidence*100)+"%"}</span></div>
                <div className="buttons">
                  <button className="approve" disabled={busy} onClick={()=>void mutate(
                    ()=>request("/suggestions/"+s.id,"PATCH",{status:"ACCEPTED"}),"Reassignment approved and applied.")}>✓ Accept and assign</button>
                  <button className="reject" disabled={busy} onClick={()=>void mutate(
                    ()=>request("/suggestions/"+s.id,"PATCH",{status:"REJECTED"}),"Suggestion rejected.")}>Reject</button>
                </div>
              </article>)}
              {!suggestions.length&&<div className="empty state-empty"><div className="empty-icon">✓</div>
                <strong>All caught up</strong><p>No pending decisions. Set an assigned agent offline or request a manual suggestion from an order.</p></div>}
            </div>
          </section>
        </div>
      </div>
      <section className="panel orders-panel">
        <div className="panel-heading"><div><div className="eyebrow">DISPATCH OVERVIEW</div><h2>Orders</h2></div><span className="muted">{orders.length} total</span></div>
        <div className="table-wrap"><table><thead><tr><th>ORDER</th><th>DESCRIPTION</th><th>ASSIGNED AGENT</th><th>STATE</th><th>ACTION</th></tr></thead>
          <tbody>{orders.map(order=><tr key={order.id}>
            <td><b>#{order.id}</b></td><td>{order.description}</td>
            <td>{agentName(order.assignedAgentId)}</td>
            <td><span className={"order-state order-"+order.status.toLowerCase()}>{orderLabel[order.status]}</span></td>
            <td><button disabled={busy||order.status==="DELIVERED"||order.status==="REASSIGNED"||suggestions.some(s=>s.orderId===order.id)}
              onClick={()=>void mutate(()=>request("/orders/"+order.id+"/suggest","POST"),"Manual suggestion requested.")}>Suggest agent</button></td>
          </tr>)}</tbody></table>{!orders.length&&<p className="empty">No orders found.</p>}</div>
      </section>
      <footer><span>ENGINEERING PORTFOLIO DEMO · SHUBHAM KUMAR</span>
        <span>Auto-refresh: 4 seconds {lastUpdated?"· Last sync "+lastUpdated.toLocaleTimeString():""}</span>
        <span>Local demonstration only · No authentication</span></footer>
    </main>
  </div>;
}
