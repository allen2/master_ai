package com.aihedgefund.orchestrator;

import com.aihedgefund.agent.BaseAgent;
import com.aihedgefund.agent.portfolio.PortfolioManagerAgent;
import com.aihedgefund.agent.risk.RiskManagerAgent;
import com.aihedgefund.model.req.HedgeFundRunReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 对冲基金工作流编排器（替代 Python LangGraph StateGraph）
 *
 * Phase 4 架构：
 *   [分析师 Agents — 并行 CompletableFuture] → RiskManager（串行）→ PortfolioManager（串行）
 */
@Component
public class HedgeFundOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(HedgeFundOrchestrator.class);

    private final List<BaseAgent> analystAgents;
    private final RiskManagerAgent riskManagerAgent;
    private final PortfolioManagerAgent portfolioManagerAgent;

    // 并行执行分析师 Agents 的线程池，大小 = 分析师数量（最多 21）
    private final ExecutorService agentExecutor;

    /** agentId → 显示名映射，用于活动/信号事件附带可读名称 */
    private final Map<String, String> agentDisplayNames;

    public HedgeFundOrchestrator(List<BaseAgent> analystAgents,
                                  RiskManagerAgent riskManagerAgent,
                                  PortfolioManagerAgent portfolioManagerAgent,
                                  @Value("${hedge-fund.agent-concurrency:5}") int agentConcurrency) {
        this.analystAgents = analystAgents.stream()
                .filter(a -> !(a instanceof RiskManagerAgent) && !(a instanceof PortfolioManagerAgent))
                .toList();
        this.riskManagerAgent = riskManagerAgent;
        this.portfolioManagerAgent = portfolioManagerAgent;

        Map<String, String> names = new HashMap<>();
        this.analystAgents.forEach(a -> names.put(a.getAgentId(), a.getDisplayName()));
        names.put(riskManagerAgent.getAgentId(), riskManagerAgent.getDisplayName());
        names.put(portfolioManagerAgent.getAgentId(), portfolioManagerAgent.getDisplayName());
        this.agentDisplayNames = names;

        int poolSize = Math.min(agentConcurrency, this.analystAgents.size());
        this.agentExecutor = Executors.newFixedThreadPool(poolSize);
        log.info("编排器初始化（并行模式），分析师 {} 个，LLM 并发线程数 {}",
                this.analystAgents.size(), poolSize);
    }

    public WorkflowResult run(HedgeFundRunReq req, SseEmitter emitter) {
        log.info("开始工作流（并行），tickers={}", req.getTickers());

        AgentState state = buildInitialState(req);
        // 接入活动进度监听器：agent 分析过程中的细粒度活动（调用工具/获取数据/推理中）即时推送到前端
        state.setActivityListener((agentId, message) -> sendActivity(emitter, agentId, message));
        List<BaseAgent> selectedAgents = selectAgents(req.getSelectedAnalysts());

        // ---- Phase 1：并行执行所有分析师 Agent ----
        // 对应 Python LangGraph 的并行 add_edge
        List<CompletableFuture<Void>> futures = selectedAgents.stream()
                .map(agent -> CompletableFuture.runAsync(() -> {
                    for (String ticker : req.getTickers()) {
                        sendProgress(emitter, agent.getDisplayName(), ticker, "analyzing");
                    }
                    try {
                        agent.analyze(state, req.getTickers());
                        // 该 agent 分析完成后立即推送其信号，前端即时展示（无需等待全部完成）
                        sendAgentSignal(emitter, agent, state, req.getTickers());
                        for (String ticker : req.getTickers()) {
                            sendProgress(emitter, agent.getDisplayName(), ticker, "done");
                        }
                    } catch (Exception e) {
                        log.error("[{}] 执行失败: {}", agent.getAgentId(), e.getMessage());
                        for (String ticker : req.getTickers()) {
                            sendProgress(emitter, agent.getDisplayName(), ticker, "error");
                        }
                    }
                }, agentExecutor))
                .toList();

        // 等待所有分析师完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("所有分析师完成，开始风险评估");

        // ---- Phase 2：风险管理（串行）----
        sendProgress(emitter, riskManagerAgent.getDisplayName(), "ALL", "analyzing");
        try {
            riskManagerAgent.analyze(state, req.getTickers());
            sendProgress(emitter, riskManagerAgent.getDisplayName(), "ALL", "done");
        } catch (Exception e) {
            log.error("[RiskManager] 执行失败: {}", e.getMessage());
            sendProgress(emitter, riskManagerAgent.getDisplayName(), "ALL", "error");
        }

        // ---- Phase 3：组合决策（串行）----
        sendProgress(emitter, portfolioManagerAgent.getDisplayName(), "ALL", "analyzing");
        try {
            portfolioManagerAgent.analyze(state, req.getTickers());
            sendProgress(emitter, portfolioManagerAgent.getDisplayName(), "ALL", "done");
        } catch (Exception e) {
            log.error("[PortfolioManager] 执行失败: {}", e.getMessage());
            sendProgress(emitter, portfolioManagerAgent.getDisplayName(), "ALL", "error");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> decisions = (Map<String, Object>) state.getData("decisions");

        WorkflowResult result = new WorkflowResult(
                state.getAnalystSignals(),
                decisions != null ? decisions : Map.of(),
                "complete"
        );
        log.info("工作流完成，analyst_signals={}", state.getAnalystSignals().size());
        return result;
    }

    private AgentState buildInitialState(HedgeFundRunReq req) {
        AgentState state = new AgentState();
        state.putData("tickers", req.getTickers());
        state.putData("start_date", req.getStartDate() != null
                ? req.getStartDate() : LocalDate.now().minusMonths(3).toString());
        state.putData("end_date", req.getEndDate() != null
                ? req.getEndDate() : LocalDate.now().toString());
        state.putData("model_name", req.getModelName());
        state.putData("model_provider", req.getModelProvider());
        state.putData("initial_cash", req.getInitialCash());
        state.addMessage("Make trading decisions based on the provided data.");
        return state;
    }

    private List<BaseAgent> selectAgents(List<String> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) return analystAgents;
        return analystAgents.stream()
                .filter(a -> selectedIds.contains(a.getAgentId()))
                .toList();
    }

    /** emitter 一旦失效即置 true，避免后续线程反复写死 emitter */
    private volatile boolean emitterDead = false;

    /** 串行化并发线程对同一 emitter 的写入，避免多 agent 同时推送时事件交错 */
    private final Object emitterLock = new Object();

    private void sendProgress(SseEmitter emitter, String agentName, String ticker, String status) {
        Map<String, String> event = new HashMap<>();
        event.put("agent", agentName);
        event.put("ticker", ticker);
        event.put("status", status);
        sendEvent(emitter, "progress", event);
    }

    /**
     * 推送单个 agent 分析完成后的信号，供前端即时展示。
     *
     * @param emitter SSE emitter
     * @param agent   完成分析的 agent
     * @param state   共享状态（读取该 agent 写入的信号）
     * @param tickers 股票列表
     */
    private void sendAgentSignal(SseEmitter emitter, BaseAgent agent, AgentState state, List<String> tickers) {
        if (emitter == null || emitterDead) {
            return;
        }
        Map<String, Object> signals = new HashMap<>();
        for (String ticker : tickers) {
            AgentSignal sig = state.getSignal(agent.getAgentId(), ticker);
            if (sig == null) {
                continue;
            }
            Map<String, Object> one = new HashMap<>();
            one.put("signal", sig.getSignal());
            one.put("confidence", sig.getConfidence());
            one.put("reasoning", sig.getReasoning());
            signals.put(ticker, one);
        }
        if (signals.isEmpty()) {
            return;
        }
        Map<String, Object> event = new HashMap<>();
        event.put("agent", agent.getDisplayName());
        event.put("agent_id", agent.getAgentId());
        event.put("signals", signals);
        sendEvent(emitter, "signal", event);
        log.info("[SSE] 推送 agent 信号: agent={}, tickers={}", agent.getAgentId(), signals.keySet());
    }

    /**
     * 推送 agent 分析过程中的活动进度（调用工具、获取数据、推理中等），供前端即时展示。
     *
     * @param emitter SSE emitter
     * @param agentId agent 标识
     * @param message 简短活动描述
     */
    private void sendActivity(SseEmitter emitter, String agentId, String message) {
        if (emitter == null || emitterDead) {
            return;
        }
        Map<String, Object> event = new HashMap<>();
        event.put("agent_id", agentId);
        event.put("agent", agentDisplayNames.getOrDefault(agentId, agentId));
        event.put("message", message);
        sendEvent(emitter, "activity", event);
    }

    /**
     * 线程安全地发送一个 SSE 事件；emitter 失效后置死并不再尝试。
     */
    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        if (emitter == null || emitterDead) {
            return;
        }
        synchronized (emitterLock) {
            if (emitterDead) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                emitterDead = true;
                log.warn("SSE 推送失败（IO）: {}", e.getMessage());
            } catch (IllegalStateException e) {
                // emitter 已超时/完成/客户端断开，标记死亡，后续不再尝试
                emitterDead = true;
                log.warn("SSE 推送失败（emitter 已关闭）: {}", e.getMessage());
            }
        }
    }
}
