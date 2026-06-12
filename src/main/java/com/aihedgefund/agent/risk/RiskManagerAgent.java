package com.aihedgefund.agent.risk;

import com.aihedgefund.agent.BaseAgent;
import com.aihedgefund.data.client.FinancialDatasetsClient;
import com.aihedgefund.orchestrator.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 风险管理 Agent（规则型）
 * 替代 Python src/agents/risk_manager.py
 *
 * 计算每个 ticker 的：
 *   - 价格波动率（过去 30 天日收益率标准差）
 *   - analyst_signals 的信号一致性
 *   - 建议仓位限制（max_position_pct）
 */
@Component
public class RiskManagerAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(RiskManagerAgent.class);
    private final FinancialDatasetsClient client;

    public RiskManagerAgent(FinancialDatasetsClient client) {
        this.client = client;
    }

    @Override public String getAgentId() { return "risk_management_agent"; }
    @Override public String getDisplayName() { return "Risk Manager"; }

    @Override
    public void analyze(AgentState state, List<String> tickers) {
        Map<String, Map<String, Object>> riskMetrics = new HashMap<>();

        for (String ticker : tickers) {
            log.info("[{}] 计算风险指标, ticker={}", getAgentId(), ticker);
            Map<String, Object> metrics = computeRisk(state, ticker);
            riskMetrics.put(ticker, metrics);
        }

        state.putData("risk_metrics", riskMetrics);
        log.info("[{}] 风险评估完成，覆盖 {} 个 ticker", getAgentId(), tickers.size());
    }

    private Map<String, Object> computeRisk(AgentState state, String ticker) {
        String endDate = state.getEndDate() != null ? state.getEndDate() : "2024-12-31";
        String startDate = state.getStartDate() != null ? state.getStartDate() : "2024-01-01";

        List<Map<String, Object>> prices = client.getPrices(ticker, startDate, endDate);

        double volatility = 0.0;
        if (prices != null && prices.size() >= 2) {
            List<Double> returns = new ArrayList<>();
            for (int i = 1; i < prices.size(); i++) {
                double prev = toDouble(prices.get(i - 1).get("close"));
                double cur = toDouble(prices.get(i).get("close"));
                if (prev > 0) returns.add((cur - prev) / prev);
            }
            volatility = stdDev(returns);
        }

        // 信号一致性：统计看多 vs 看空的 agent 数量
        Map<String, Map<String, com.aihedgefund.orchestrator.AgentSignal>> signals = state.getAnalystSignals();
        int bullCount = 0, bearCount = 0;
        for (Map<String, com.aihedgefund.orchestrator.AgentSignal> agentSignals : signals.values()) {
            com.aihedgefund.orchestrator.AgentSignal sig = agentSignals.get(ticker);
            if (sig != null) {
                if ("bullish".equals(sig.getSignal())) bullCount++;
                else if ("bearish".equals(sig.getSignal())) bearCount++;
            }
        }

        // 高波动或信号分歧 → 降低仓位
        double maxPositionPct = 0.10; // 默认最多 10%
        if (volatility > 0.03) maxPositionPct = 0.05; // 高波动降低到 5%
        if (Math.abs(bullCount - bearCount) <= 1) maxPositionPct *= 0.5; // 信号分歧减半

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("volatility_30d", Math.round(volatility * 10000.0) / 100.0); // 百分比
        result.put("bull_count", bullCount);
        result.put("bear_count", bearCount);
        result.put("max_position_pct", maxPositionPct);
        return result;
    }

    private double stdDev(List<Double> values) {
        if (values.isEmpty()) return 0;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        return Math.sqrt(variance);
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return 0; }
    }
}
