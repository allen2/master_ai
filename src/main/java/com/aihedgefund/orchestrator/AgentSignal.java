package com.aihedgefund.orchestrator;

/**
 * 单个 Agent 对单个 Ticker 的分析信号
 * 替代 Python 中的 WarrenBuffettSignal / FundamentalsSignal 等 Pydantic 模型
 */
public class AgentSignal {

    private String signal;      // bullish / bearish / neutral
    private int confidence;     // 0-100
    private String reasoning;

    public AgentSignal() {}

    public AgentSignal(String signal, int confidence, String reasoning) {
        this.signal = signal;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }

    public static AgentSignal bullish(int confidence, String reasoning) {
        return new AgentSignal("bullish", confidence, reasoning);
    }

    public static AgentSignal bearish(int confidence, String reasoning) {
        return new AgentSignal("bearish", confidence, reasoning);
    }

    public static AgentSignal neutral(int confidence, String reasoning) {
        return new AgentSignal("neutral", confidence, reasoning);
    }

    public String getSignal() { return signal; }
    public void setSignal(String signal) { this.signal = signal; }
    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    @Override
    public String toString() {
        return "AgentSignal{signal='" + signal + "', confidence=" + confidence + "}";
    }
}
