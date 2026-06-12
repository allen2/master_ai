package com.aihedgefund.orchestrator;

import com.aihedgefund.agent.BaseAgent;
import com.aihedgefund.agent.portfolio.PortfolioManagerAgent;
import com.aihedgefund.agent.risk.RiskManagerAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.model.req.HedgeFundRunReq;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 质量价值三重门槛 — 编排器级功能测试（不依赖 LLM / 外部数据）。
 *
 * <p>用桩分析师 Agent 直接写入信号，串起真实 {@link HedgeFundOrchestrator}
 * 与真实 {@link PortfolioManagerAgent}，验证端到端的最终交易决策是否符合门槛规则。
 * RiskManagerAgent 被 mock 为 no-op，组合管理使用默认仓位占比。</p>
 */
class QualityValueGateFT {

    /** 桩分析师：固定向 state 写入指定方向的信号 */
    private static BaseAgent stubAgent(String agentId, String signal, int confidence) {
        return new BaseAgent() {
            @Override
            public void analyze(AgentState state, List<String> tickers) {
                for (String ticker : tickers) {
                    state.putSignal(agentId, ticker, new AgentSignal(signal, confidence, agentId));
                }
            }

            @Override
            public String getAgentId() {
                return agentId;
            }

            @Override
            public String getDisplayName() {
                return agentId;
            }
        };
    }

    private HedgeFundOrchestrator buildOrchestrator(List<BaseAgent> analysts) {
        RiskManagerAgent riskManager = Mockito.mock(RiskManagerAgent.class);
        PortfolioManagerAgent portfolioManager = new PortfolioManagerAgent(
                Mockito.mock(LlmClientFactory.class),
                Mockito.mock(StructuredOutputHelper.class));
        return new HedgeFundOrchestrator(analysts, riskManager, portfolioManager, 2);
    }

    private HedgeFundRunReq buildReq() {
        HedgeFundRunReq req = new HedgeFundRunReq();
        req.setTickers(List.of("AAPL"));
        req.setInitialCash(100_000.0);
        return req;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decisionOf(WorkflowResult result, String ticker) {
        Map<String, Object> decisions = result.getDecisions();
        assertThat(decisions).containsKey(ticker);
        return (Map<String, Object>) decisions.get(ticker);
    }

    /** 三组门槛均满足 → 端到端输出 buy */
    @Test
    void run_allGatesPass_decisionIsBuy() {
        List<BaseAgent> analysts = new ArrayList<>();
        analysts.add(stubAgent("warren_buffett", "bullish", 80));
        analysts.add(stubAgent("charlie_munger", "bullish", 75));
        analysts.add(stubAgent("ben_graham", "bullish", 70));
        analysts.add(stubAgent("aswath_damodaran", "bullish", 72));

        WorkflowResult result = buildOrchestrator(analysts).run(buildReq(), null);

        Map<String, Object> decision = decisionOf(result, "AAPL");
        assertThat(decision.get("action")).isEqualTo("buy");
        assertThat(decision.get("gate_passed")).isEqualTo(true);
    }

    /** DCF 组缺失（未选达摩达兰/估值分析师）→ 端到端输出 hold */
    @Test
    void run_missingDcfGroup_decisionIsHold() {
        List<BaseAgent> analysts = new ArrayList<>();
        analysts.add(stubAgent("warren_buffett", "bullish", 80));
        analysts.add(stubAgent("charlie_munger", "bullish", 75));
        analysts.add(stubAgent("ben_graham", "bullish", 70));

        WorkflowResult result = buildOrchestrator(analysts).run(buildReq(), null);

        Map<String, Object> decision = decisionOf(result, "AAPL");
        assertThat(decision.get("action")).isEqualTo("hold");
        assertThat(decision.get("gate_passed")).isEqualTo(false);
    }

    /** 低估值组出现看空 → 严格否决 → 端到端输出 hold */
    @Test
    void run_bearishInValueGroup_decisionIsHold() {
        List<BaseAgent> analysts = new ArrayList<>();
        analysts.add(stubAgent("warren_buffett", "bullish", 80));
        analysts.add(stubAgent("charlie_munger", "bullish", 75));
        analysts.add(stubAgent("ben_graham", "bullish", 70));
        analysts.add(stubAgent("michael_burry", "bearish", 65));
        analysts.add(stubAgent("aswath_damodaran", "bullish", 72));

        WorkflowResult result = buildOrchestrator(analysts).run(buildReq(), null);

        Map<String, Object> decision = decisionOf(result, "AAPL");
        assertThat(decision.get("action")).isEqualTo("hold");
        assertThat(decision.get("gate_passed")).isEqualTo(false);
    }
}
