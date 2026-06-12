package com.aihedgefund.agent.analyst;

import com.aihedgefund.agent.BaseAgent;
import com.aihedgefund.data.client.FinancialDatasetsClient;
import com.aihedgefund.orchestrator.AgentSignal;
import com.aihedgefund.orchestrator.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 成长性分析 Agent（规则型，无 LLM）
 * 替代 Python src/agents/growth_agent.py
 *
 * 逻辑：
 *   取最近 2 期财务指标，计算营收和净利润 YoY 增长率
 *   营收增长 > 20%  → +1；< 0 → -1
 *   净利润增长 > 20% → +1；< 0 → -1
 *   score 2 → bullish，0 → neutral，-2 → bearish
 */
@Component
public class GrowthAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(GrowthAgent.class);

    private final FinancialDatasetsClient client;

    public GrowthAgent(FinancialDatasetsClient client) {
        this.client = client;
    }

    @Override
    public String getAgentId() { return "growth_analyst"; }

    @Override
    public String getDisplayName() { return "成长分析师"; }

    @Override
    public void analyze(AgentState state, List<String> tickers) {
        String endDate = state.getEndDate();
        if (endDate == null) endDate = LocalDate.now().toString();

        for (String ticker : tickers) {
            log.info("[{}] 分析成长性, ticker={}", getAgentId(), ticker);
            AgentSignal signal = analyzeOneTicker(ticker, endDate);
            state.putSignal(getAgentId(), ticker, signal);
            log.debug("[{}] ticker={} signal={}", getAgentId(), ticker, signal);
        }
    }

    private AgentSignal analyzeOneTicker(String ticker, String endDate) {
        List<Map<String, Object>> metrics = client.getFinancialMetrics(ticker, endDate, "annual", 3);

        if (metrics == null || metrics.size() < 2) {
            return AgentSignal.neutral(30, "历史数据不足，无法计算成长率");
        }

        Map<String, Object> current = metrics.get(0);
        Map<String, Object> prior = metrics.get(1);

        int score = 0;
        StringBuilder reasons = new StringBuilder();

        // 营收增长
        double revenueGrowth = calcGrowth(current.get("revenue"), prior.get("revenue"));
        if (revenueGrowth > 0.20) {
            score++;
            reasons.append(String.format("营收增长%.1f%%>20%%; ", revenueGrowth * 100));
        } else if (revenueGrowth < 0) {
            score--;
            reasons.append(String.format("营收下滑%.1f%%; ", Math.abs(revenueGrowth * 100)));
        } else {
            reasons.append(String.format("营收增长%.1f%%一般; ", revenueGrowth * 100));
        }

        // 净利润增长
        double earningsGrowth = calcGrowth(current.get("net_income_margin"), prior.get("net_income_margin"));
        if (earningsGrowth > 0.20) {
            score++;
            reasons.append(String.format("净利增长%.1f%%>20%%", earningsGrowth * 100));
        } else if (earningsGrowth < 0) {
            score--;
            reasons.append(String.format("净利下滑%.1f%%", Math.abs(earningsGrowth * 100)));
        } else {
            reasons.append(String.format("净利增长%.1f%%一般", earningsGrowth * 100));
        }

        String reasoning = reasons.toString();
        if (score >= 1) {
            return AgentSignal.bullish(50 + score * 15, "成长性良好: " + reasoning);
        } else if (score <= -1) {
            return AgentSignal.bearish(50 + Math.abs(score) * 15, "成长性偏弱: " + reasoning);
        } else {
            return AgentSignal.neutral(45, "成长性中等: " + reasoning);
        }
    }

    private double calcGrowth(Object current, Object prior) {
        double cur = toDouble(current);
        double pri = toDouble(prior);
        if (pri == 0) return 0;
        return (cur - pri) / Math.abs(pri);
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0.0; }
    }
}
