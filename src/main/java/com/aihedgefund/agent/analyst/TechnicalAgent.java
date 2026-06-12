package com.aihedgefund.agent.analyst;

import com.aihedgefund.agent.BaseAgent;
import com.aihedgefund.data.client.FinancialDatasetsClient;
import com.aihedgefund.orchestrator.AgentSignal;
import com.aihedgefund.orchestrator.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 技术面分析 Agent（规则型，无 LLM）
 * 替代 Python src/agents/technicals.py
 *
 * 信号策略（每个+1/-1，汇总判断）：
 *   1. MA5 > MA20（金叉）→ +1；否则 -1
 *   2. RSI(14) < 30（超卖）→ +1；> 70（超买）→ -1；否则 0
 *   3. 价格在 52 周高点 80% 以上 → +1；50% 以下 → -1
 */
@Component
public class TechnicalAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(TechnicalAgent.class);

    private final FinancialDatasetsClient client;

    public TechnicalAgent(FinancialDatasetsClient client) {
        this.client = client;
    }

    @Override
    public String getAgentId() { return "technical_analyst"; }

    @Override
    public String getDisplayName() { return "技术分析师"; }

    @Override
    public void analyze(AgentState state, List<String> tickers) {
        String endDate = state.getEndDate();
        String startDate = state.getStartDate();
        if (endDate == null) endDate = LocalDate.now().toString();
        if (startDate == null) startDate = LocalDate.now().minusMonths(12).toString();

        for (String ticker : tickers) {
            log.info("[{}] 分析技术面, ticker={}", getAgentId(), ticker);
            AgentSignal signal = analyzeOneTicker(ticker, startDate, endDate);
            state.putSignal(getAgentId(), ticker, signal);
            log.debug("[{}] ticker={} signal={}", getAgentId(), ticker, signal);
        }
    }

    private AgentSignal analyzeOneTicker(String ticker, String startDate, String endDate) {
        List<Map<String, Object>> prices = client.getPrices(ticker, startDate, endDate);

        if (prices == null || prices.size() < 20) {
            return AgentSignal.neutral(30, "价格数据不足（需要至少 20 条）");
        }

        List<Double> closes = extractCloses(prices);
        int score = 0;
        StringBuilder reasons = new StringBuilder();

        // 1. MA5 vs MA20
        double ma5 = average(closes, closes.size() - 5, closes.size());
        double ma20 = average(closes, closes.size() - 20, closes.size());
        if (ma5 > ma20) {
            score++;
            reasons.append(String.format("MA5(%.2f)>MA20(%.2f)金叉; ", ma5, ma20));
        } else {
            score--;
            reasons.append(String.format("MA5(%.2f)<MA20(%.2f)死叉; ", ma5, ma20));
        }

        // 2. RSI(14)
        if (closes.size() >= 15) {
            double rsi = calculateRsi(closes, 14);
            if (rsi < 30) {
                score++;
                reasons.append(String.format("RSI=%.1f超卖; ", rsi));
            } else if (rsi > 70) {
                score--;
                reasons.append(String.format("RSI=%.1f超买; ", rsi));
            } else {
                reasons.append(String.format("RSI=%.1f中性; ", rsi));
            }
        }

        // 3. 52 周位置
        double high52 = max(closes, Math.max(0, closes.size() - 252), closes.size());
        double low52 = min(closes, Math.max(0, closes.size() - 252), closes.size());
        double current = closes.get(closes.size() - 1);
        if (high52 > low52) {
            double position = (current - low52) / (high52 - low52);
            if (position >= 0.8) {
                score++;
                reasons.append(String.format("处于52周高点%.0f%%位置; ", position * 100));
            } else if (position <= 0.3) {
                score--;
                reasons.append(String.format("处于52周低点%.0f%%位置; ", position * 100));
            } else {
                reasons.append(String.format("处于52周中间%.0f%%位置; ", position * 100));
            }
        }

        String reasoning = reasons.toString();
        if (score >= 1) {
            return AgentSignal.bullish(Math.min(40 + score * 15, 80), "技术面偏多: " + reasoning);
        } else if (score <= -1) {
            return AgentSignal.bearish(Math.min(40 + Math.abs(score) * 15, 80), "技术面偏空: " + reasoning);
        } else {
            return AgentSignal.neutral(45, "技术面中性: " + reasoning);
        }
    }

    private List<Double> extractCloses(List<Map<String, Object>> prices) {
        List<Double> closes = new ArrayList<>();
        for (Map<String, Object> bar : prices) {
            Object close = bar.get("close");
            if (close instanceof Number) {
                closes.add(((Number) close).doubleValue());
            }
        }
        return closes;
    }

    private double average(List<Double> data, int from, int to) {
        double sum = 0;
        int count = 0;
        for (int i = from; i < to && i < data.size(); i++) {
            sum += data.get(i);
            count++;
        }
        return count > 0 ? sum / count : 0;
    }

    private double max(List<Double> data, int from, int to) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = from; i < to && i < data.size(); i++) {
            max = Math.max(max, data.get(i));
        }
        return max == Double.NEGATIVE_INFINITY ? 0 : max;
    }

    private double min(List<Double> data, int from, int to) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = from; i < to && i < data.size(); i++) {
            min = Math.min(min, data.get(i));
        }
        return min == Double.POSITIVE_INFINITY ? 0 : min;
    }

    private double calculateRsi(List<Double> closes, int period) {
        double avgGain = 0, avgLoss = 0;
        int start = closes.size() - period - 1;
        if (start < 0) return 50;

        for (int i = start; i < start + period; i++) {
            double change = closes.get(i + 1) - closes.get(i);
            if (change > 0) avgGain += change;
            else avgLoss += Math.abs(change);
        }
        avgGain /= period;
        avgLoss /= period;

        if (avgLoss == 0) return 100;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
}
