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
 * 情绪面分析 Agent — 基于内部人交易（规则型，无 LLM）
 * 替代 Python src/agents/sentiment.py 中的 insider trades 部分
 *
 * 逻辑：
 *   统计最近 90 天内部人买入 vs 卖出笔数
 *   buyRatio = buys / total
 *   > 0.7  → bullish
 *   < 0.3  → bearish
 *   否则   → neutral
 */
@Component
public class SentimentAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(SentimentAgent.class);

    private final FinancialDatasetsClient client;

    public SentimentAgent(FinancialDatasetsClient client) {
        this.client = client;
    }

    @Override
    public String getAgentId() { return "sentiment_analyst"; }

    @Override
    public String getDisplayName() { return "情绪分析师"; }

    @Override
    public void analyze(AgentState state, List<String> tickers) {
        String endDate = state.getEndDate();
        if (endDate == null) endDate = LocalDate.now().toString();

        for (String ticker : tickers) {
            log.info("[{}] 分析内部人情绪, ticker={}", getAgentId(), ticker);
            AgentSignal signal = analyzeOneTicker(ticker, endDate);
            state.putSignal(getAgentId(), ticker, signal);
            log.debug("[{}] ticker={} signal={}", getAgentId(), ticker, signal);
        }
    }

    private AgentSignal analyzeOneTicker(String ticker, String endDate) {
        List<Map<String, Object>> trades = client.getInsiderTrades(ticker, endDate, 50);

        if (trades == null || trades.isEmpty()) {
            return AgentSignal.neutral(30, "内部人交易数据不足");
        }

        int buys = 0, sells = 0;
        for (Map<String, Object> trade : trades) {
            String transactionType = toString(trade.get("transaction_type"));
            if (transactionType == null) continue;
            String type = transactionType.toLowerCase();
            if (type.contains("purchase") || type.contains("buy") || type.equals("p")) {
                buys++;
            } else if (type.contains("sale") || type.contains("sell") || type.equals("s")) {
                sells++;
            }
        }

        int total = buys + sells;
        if (total == 0) {
            return AgentSignal.neutral(30, "无有效内部人买卖记录");
        }

        double buyRatio = (double) buys / total;
        String reasoning = String.format("内部人买入%d笔/卖出%d笔，买入比=%.0f%%", buys, sells, buyRatio * 100);

        if (buyRatio >= 0.7) {
            return AgentSignal.bullish(60, reasoning + "，内部人看多");
        } else if (buyRatio <= 0.3) {
            return AgentSignal.bearish(60, reasoning + "，内部人看空");
        } else {
            return AgentSignal.neutral(50, reasoning + "，内部人情绪中性");
        }
    }

    private String toString(Object val) {
        return val != null ? val.toString() : null;
    }
}
