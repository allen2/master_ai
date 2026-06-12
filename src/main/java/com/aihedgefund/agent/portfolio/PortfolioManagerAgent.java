package com.aihedgefund.agent.portfolio;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.orchestrator.AgentSignal;
import com.aihedgefund.orchestrator.AgentState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 组合管理 Agent（质量价值三重门槛筛选）
 * 替代 Python src/agents/portfolio_manager.py
 *
 * <p>本实现采用"必过门槛（veto gate）"逻辑，专门用于筛选
 * 「低估值 + 高护城河 + 强 DCF 现金流折现」的优质公司：</p>
 * <ul>
 *     <li>护城河组：评估持久竞争优势与负责任的管理层</li>
 *     <li>低估值组：评估安全边际与价格折让</li>
 *     <li>DCF 内在价值组：评估未来现金流折现能力</li>
 * </ul>
 *
 * <p>三组均为必过项：任一组缺失（未选相关分析师）、出现看空信号、
 * 或没有任何看多信号，该 ticker 即被淘汰为 hold；三组全部通过才给出 buy。</p>
 *
 * 输入：所有 analyst_signals + risk_metrics
 * 输出：每个 ticker 的交易决策（buy/hold + 数量 + 门槛明细）
 */
@Component
public class PortfolioManagerAgent extends BaseLlmAgent {

    private static final Logger log = LoggerFactory.getLogger(PortfolioManagerAgent.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ---- 信号方向常量（禁用魔法值）----
    private static final String SIGNAL_BULLISH = "bullish";
    private static final String SIGNAL_BEARISH = "bearish";

    // ---- 交易动作常量 ----
    private static final String ACTION_BUY = "buy";
    private static final String ACTION_HOLD = "hold";

    // ---- 默认值 ----
    private static final double DEFAULT_MAX_POSITION_PCT = 0.05;
    private static final double DEFAULT_INITIAL_CASH = 100_000;
    private static final double DEFAULT_CONFIDENCE = 50;

    /**
     * 质量价值三重门槛分组（agentId 集合），三组均为必过项。
     * 使用 LinkedHashMap 保证门槛明细输出顺序稳定。
     */
    private static final Map<String, Set<String>> GATE_GROUPS = buildGateGroups();

    private static Map<String, Set<String>> buildGateGroups() {
        Map<String, Set<String>> groups = new LinkedHashMap<>();
        // 护城河 + 负责任的管理层：巴菲特、芒格
        groups.put("护城河组", new LinkedHashSet<>(Arrays.asList(
                "warren_buffett", "charlie_munger")));
        // 低估值 / 安全边际：格雷厄姆、帕布莱、伯里、估值分析师
        groups.put("低估值组", new LinkedHashSet<>(Arrays.asList(
                "ben_graham", "mohnish_pabrai", "michael_burry", "valuation_analyst")));
        // DCF 内在价值：达摩达兰、估值分析师
        groups.put("DCF内在价值组", new LinkedHashSet<>(Arrays.asList(
                "aswath_damodaran", "valuation_analyst")));
        return Collections.unmodifiableMap(groups);
    }

    public PortfolioManagerAgent(LlmClientFactory llmFactory, StructuredOutputHelper outputHelper) {
        super(llmFactory, outputHelper);
    }

    @Override public String getAgentId() { return "portfolio_manager"; }
    @Override public String getDisplayName() { return "Portfolio Manager"; }

    @Override
    public void analyze(AgentState state, List<String> tickers) {
        log.info("[{}] 生成组合决策（质量价值三重门槛）, tickers={}", getAgentId(), tickers);
        Map<String, Object> decisions = makeDecisions(state, tickers);
        state.putData("decisions", decisions);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> makeDecisions(AgentState state, List<String> tickers) {
        Map<String, Object> decisions = new LinkedHashMap<>();
        Map<String, Map<String, AgentSignal>> signals = state.getAnalystSignals();
        Map<String, Map<String, Object>> riskMetrics =
                (Map<String, Map<String, Object>>) state.getData("risk_metrics");

        double initialCash = toDouble(state.getData("initial_cash"));
        if (initialCash == 0) {
            initialCash = DEFAULT_INITIAL_CASH;
        }

        for (String ticker : tickers) {
            // 统计各方向信号（用于决策置信度与透明度展示）
            SignalStats stats = countSignals(signals, ticker);

            // 评估质量价值三重门槛
            GateOutcome gate = evaluateGate(signals, ticker);

            double maxPositionPct = resolveMaxPositionPct(riskMetrics, ticker);
            double avgConfidence = stats.count > 0
                    ? (double) stats.totalConfidence / stats.count
                    : DEFAULT_CONFIDENCE;

            String action;
            int quantity;
            if (gate.passed) {
                // 三组全部通过 → 买入候选
                action = ACTION_BUY;
                quantity = Math.max(1, (int) (initialCash * maxPositionPct / 100));
            } else {
                // 任一门槛未过 → 淘汰，不持仓
                action = ACTION_HOLD;
                quantity = 0;
            }

            String gateSummary = String.join("；", gate.details);
            String reasoning = gate.passed
                    ? "通过质量价值三重门槛 → buy。" + gateSummary
                    : "未通过质量价值门槛 → hold。" + gateSummary;

            Map<String, Object> decision = new LinkedHashMap<>();
            decision.put("action", action);
            decision.put("quantity", quantity);
            decision.put("confidence", (int) avgConfidence);
            decision.put("gate_passed", gate.passed);
            decision.put("gate_detail", gate.details);
            decision.put("bull_count", stats.bullCount);
            decision.put("bear_count", stats.bearCount);
            decision.put("reasoning", reasoning);
            decisions.put(ticker, decision);

            log.info("[{}] ticker={} → action={}, gate_passed={}, bull={}, bear={}, 明细={}",
                    getAgentId(), ticker, action, gate.passed, stats.bullCount, stats.bearCount, gateSummary);
        }

        return decisions;
    }

    /**
     * 统计某 ticker 在所有分析师中的信号分布。
     *
     * @param signals 全部分析师信号 agentId → ticker → signal
     * @param ticker  目标股票代码
     * @return 信号统计结果
     */
    private SignalStats countSignals(Map<String, Map<String, AgentSignal>> signals, String ticker) {
        SignalStats stats = new SignalStats();
        for (Map<String, AgentSignal> agentMap : signals.values()) {
            AgentSignal sig = agentMap.get(ticker);
            if (sig == null) {
                continue;
            }
            stats.count++;
            stats.totalConfidence += sig.getConfidence();
            if (SIGNAL_BULLISH.equals(sig.getSignal())) {
                stats.bullCount++;
            } else if (SIGNAL_BEARISH.equals(sig.getSignal())) {
                stats.bearCount++;
            } else {
                stats.neutralCount++;
            }
        }
        return stats;
    }

    /**
     * 评估质量价值三重门槛（护城河 / 低估值 / DCF），三组均为必过项。
     *
     * <p>每组判定规则（严格否决）：</p>
     * <ul>
     *     <li>缺组（未选相关分析师，组内无任何信号）→ 不过</li>
     *     <li>组内出现任一看空信号 → 否决，不过</li>
     *     <li>组内无任何看多信号（全中性）→ 未确认，不过</li>
     *     <li>组内有看多且无看空 → 通过</li>
     * </ul>
     *
     * @param signals 全部分析师信号
     * @param ticker  目标股票代码
     * @return 门槛评估结果（是否全过 + 各组明细）
     */
    private GateOutcome evaluateGate(Map<String, Map<String, AgentSignal>> signals, String ticker) {
        List<String> details = new ArrayList<>();
        boolean passed = true;

        for (Map.Entry<String, Set<String>> group : GATE_GROUPS.entrySet()) {
            String groupName = group.getKey();
            int bull = 0;
            int bear = 0;
            int neutral = 0;
            String vetoer = null;

            for (String agentId : group.getValue()) {
                Map<String, AgentSignal> agentMap = signals.get(agentId);
                AgentSignal sig = agentMap == null ? null : agentMap.get(ticker);
                if (sig == null) {
                    continue;
                }
                if (SIGNAL_BULLISH.equals(sig.getSignal())) {
                    bull++;
                } else if (SIGNAL_BEARISH.equals(sig.getSignal())) {
                    bear++;
                    if (vetoer == null) {
                        vetoer = agentId;
                    }
                } else {
                    neutral++;
                }
            }

            int present = bull + bear + neutral;
            String status;
            if (present == 0) {
                status = "缺组(未选相关分析师)";
                passed = false;
            } else if (bear > 0) {
                status = String.format("否决(%s看空)", vetoer);
                passed = false;
            } else if (bull == 0) {
                status = "未确认(无看多信号)";
                passed = false;
            } else {
                status = String.format("通过(%d多/%d中)", bull, neutral);
            }
            details.add(groupName + ": " + status);
        }

        return new GateOutcome(passed, details);
    }

    /**
     * 解析某 ticker 的最大仓位占比，无风险指标时使用默认值。
     */
    private double resolveMaxPositionPct(Map<String, Map<String, Object>> riskMetrics, String ticker) {
        if (riskMetrics != null && riskMetrics.containsKey(ticker)) {
            return toDouble(riskMetrics.get(ticker).get("max_position_pct"));
        }
        return DEFAULT_MAX_POSITION_PCT;
    }

    @Override
    protected String getSystemPrompt() { return ""; } // 不使用 LLM 调用路径

    private double toDouble(Object val) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(val));
        } catch (Exception e) {
            return 0;
        }
    }

    /** 某 ticker 的信号方向统计 */
    private static final class SignalStats {
        private int bullCount;
        private int bearCount;
        private int neutralCount;
        private int totalConfidence;
        private int count;
    }

    /** 门槛评估结果：是否三组全过 + 各组明细文本 */
    private static final class GateOutcome {
        private final boolean passed;
        private final List<String> details;

        private GateOutcome(boolean passed, List<String> details) {
            this.passed = passed;
            this.details = details;
        }
    }
}
