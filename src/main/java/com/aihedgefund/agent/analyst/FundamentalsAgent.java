package com.aihedgefund.agent.analyst;

import com.aihedgefund.agent.BaseAgent;
import com.aihedgefund.data.client.FinancialDatasetsClient;
import com.aihedgefund.orchestrator.AgentSignal;
import com.aihedgefund.orchestrator.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 基本面分析 Agent（规则型，无 LLM）
 * 替代 Python src/agents/fundamentals.py
 *
 * 评分逻辑：
 *   ROE > 15%           → +1
 *   Debt/Equity < 0.5   → +1
 *   Gross Margin > 40%  → +1
 *   Net Margin > 10%    → +1
 *
 * score 0-1 → bearish(40), 2 → neutral(50), 3-4 → bullish(score*20)
 */
@Component
public class FundamentalsAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(FundamentalsAgent.class);

    private final FinancialDatasetsClient client;

    public FundamentalsAgent(FinancialDatasetsClient client) {
        this.client = client;
    }

    @Override
    public String getAgentId() { return "fundamentals_analyst"; }

    @Override
    public String getDisplayName() { return "基本面分析师"; }

    @Override
    public void analyze(AgentState state, List<String> tickers) {
        String endDate = state.getEndDate();
        if (endDate == null) endDate = java.time.LocalDate.now().toString();

        for (String ticker : tickers) {
            log.info("[{}] 分析基本面, ticker={}", getAgentId(), ticker);
            AgentSignal signal = analyzeOneTicker(ticker, endDate);
            state.putSignal(getAgentId(), ticker, signal);
            log.debug("[{}] ticker={} signal={}", getAgentId(), ticker, signal);
        }
    }

    private AgentSignal analyzeOneTicker(String ticker, String endDate) {
        List<Map<String, Object>> metrics = client.getFinancialMetrics(ticker, endDate, "ttm", 5);

        if (metrics == null || metrics.isEmpty()) {
            return AgentSignal.neutral(30, "基本面数据不足，无法评分");
        }

        Map<String, Object> latest = metrics.get(0);
        int score = 0;
        StringBuilder reasons = new StringBuilder();

        // ROE > 15%
        double roe = toDouble(latest.get("return_on_equity"));
        if (roe > 0.15) {
            score++;
            reasons.append(String.format("ROE=%.1f%%>15%%; ", roe * 100));
        } else {
            reasons.append(String.format("ROE=%.1f%%较低; ", roe * 100));
        }

        // Debt/Equity < 0.5
        double debtToEquity = toDouble(latest.get("debt_to_equity"));
        if (debtToEquity >= 0 && debtToEquity < 0.5) {
            score++;
            reasons.append(String.format("D/E=%.2f<0.5健康; ", debtToEquity));
        } else if (debtToEquity >= 0.5) {
            reasons.append(String.format("D/E=%.2f偏高; ", debtToEquity));
        }

        // Gross Margin > 40%
        double grossMargin = toDouble(latest.get("gross_margin"));
        if (grossMargin > 0.40) {
            score++;
            reasons.append(String.format("毛利率=%.1f%%>40%%; ", grossMargin * 100));
        } else {
            reasons.append(String.format("毛利率=%.1f%%偏低; ", grossMargin * 100));
        }

        // Net Margin > 10%
        double netMargin = toDouble(latest.get("net_margin"));
        if (netMargin > 0.10) {
            score++;
            reasons.append(String.format("净利率=%.1f%%>10%%", netMargin * 100));
        } else {
            reasons.append(String.format("净利率=%.1f%%偏低", netMargin * 100));
        }

        String reasoning = reasons.toString();
        if (score >= 3) {
            return AgentSignal.bullish(Math.min(score * 20, 80), "基本面强劲: " + reasoning);
        } else if (score >= 2) {
            return AgentSignal.neutral(50, "基本面一般: " + reasoning);
        } else {
            return AgentSignal.bearish(40, "基本面偏弱: " + reasoning);
        }
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0.0; }
    }
}
