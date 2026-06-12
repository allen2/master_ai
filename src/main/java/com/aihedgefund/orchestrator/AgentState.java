package com.aihedgefund.orchestrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 线程安全的共享状态容器，替代 Python LangGraph 的 AgentState TypedDict
 *
 * messages: 累积消息（对应 Annotated[Sequence[BaseMessage], operator.add]）
 * data:     共享数据字典（tickers, portfolio, dates 等）
 * analystSignals: agentId → ticker → AgentSignal（替代 analyst_signals dict）
 */
public class AgentState {

    private final List<String> messages = new CopyOnWriteArrayList<>();
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AgentSignal>> analystSignals = new ConcurrentHashMap<>();

    /** 活动进度监听器，用于把 agent 分析过程中的细粒度活动（调用工具/获取数据等）推送到外层（如 SSE） */
    private volatile ActivityListener activityListener;

    public AgentState() {}

    /**
     * agent 分析过程中的活动进度监听器。
     */
    @FunctionalInterface
    public interface ActivityListener {

        /**
         * @param agentId agent 标识
         * @param message 简短的活动描述（如「调用工具 getDaily 获取数据」）
         */
        void onActivity(String agentId, String message);
    }

    /** 设置活动监听器（由编排器接入 SSE 推送） */
    public void setActivityListener(ActivityListener listener) {
        this.activityListener = listener;
    }

    /**
     * 发布一条活动进度。监听器未设置或抛错时静默忽略，绝不影响分析主流程。
     *
     * @param agentId agent 标识
     * @param message 简短活动描述
     */
    public void publishActivity(String agentId, String message) {
        ActivityListener listener = this.activityListener;
        if (listener == null) {
            return;
        }
        try {
            listener.onActivity(agentId, message);
        } catch (Exception ignored) {
            // 进度推送失败不影响分析主流程
        }
    }

    // ---- messages ----
    public void addMessage(String message) {
        messages.add(message);
    }

    public List<String> getMessages() {
        return new ArrayList<>(messages);
    }

    // ---- data ----
    public void putData(String key, Object value) {
        data.put(key, value);
    }

    public Object getData(String key) {
        return data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Object val = data.get(key);
        return type.isInstance(val) ? type.cast(val) : null;
    }

    public Map<String, Object> getDataMap() {
        return new HashMap<>(data);
    }

    // ---- analystSignals ----
    public void putSignal(String agentId, String ticker, AgentSignal signal) {
        analystSignals.computeIfAbsent(agentId, k -> new ConcurrentHashMap<>())
                .put(ticker, signal);
    }

    public AgentSignal getSignal(String agentId, String ticker) {
        Map<String, AgentSignal> agentMap = analystSignals.get(agentId);
        return agentMap != null ? agentMap.get(ticker) : null;
    }

    public Map<String, Map<String, AgentSignal>> getAnalystSignals() {
        Map<String, Map<String, AgentSignal>> copy = new HashMap<>();
        analystSignals.forEach((agentId, tickerMap) ->
                copy.put(agentId, new HashMap<>(tickerMap)));
        return copy;
    }

    // ---- helpers ----
    @SuppressWarnings("unchecked")
    public List<String> getTickers() {
        Object val = data.get("tickers");
        return val instanceof List ? (List<String>) val : List.of();
    }

    public String getStartDate() {
        return (String) data.get("start_date");
    }

    public String getEndDate() {
        return (String) data.get("end_date");
    }

    public String getModelName() {
        return (String) data.get("model_name");
    }

    public String getModelProvider() {
        return (String) data.get("model_provider");
    }
}
