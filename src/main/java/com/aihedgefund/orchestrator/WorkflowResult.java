package com.aihedgefund.orchestrator;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * 工作流执行结果，对应 Python run_hedge_fund() 的返回值
 */
public class WorkflowResult {

    @JsonProperty("analyst_signals")
    private Map<String, Map<String, AgentSignal>> analystSignals;

    private Map<String, Object> decisions;

    private String status;

    public WorkflowResult() {}

    public WorkflowResult(Map<String, Map<String, AgentSignal>> analystSignals,
                          Map<String, Object> decisions,
                          String status) {
        this.analystSignals = analystSignals;
        this.decisions = decisions;
        this.status = status;
    }

    public static WorkflowResult partial(Map<String, Map<String, AgentSignal>> analystSignals) {
        return new WorkflowResult(analystSignals, Map.of(), "partial");
    }

    public Map<String, Map<String, AgentSignal>> getAnalystSignals() { return analystSignals; }
    public void setAnalystSignals(Map<String, Map<String, AgentSignal>> analystSignals) {
        this.analystSignals = analystSignals;
    }
    public Map<String, Object> getDecisions() { return decisions; }
    public void setDecisions(Map<String, Object> decisions) { this.decisions = decisions; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
