package com.aihedgefund.agent;

import com.aihedgefund.orchestrator.AgentState;

import java.util.List;

/**
 * 所有 Agent 的基础接口
 * 对应 Python 中每个 agent_func(state: AgentState) 函数
 */
public interface BaseAgent {

    /**
     * 分析指定股票列表，将信号写入 state.analystSignals
     *
     * @param state   共享状态（线程安全）
     * @param tickers 股票代码列表
     */
    void analyze(AgentState state, List<String> tickers);

    /**
     * Agent 唯一标识，用于 analystSignals 的 key
     * 对应 Python 中的 agent_id 参数
     */
    String getAgentId();

    /**
     * 展示名称（用于 SSE 进度事件）
     */
    String getDisplayName();
}
