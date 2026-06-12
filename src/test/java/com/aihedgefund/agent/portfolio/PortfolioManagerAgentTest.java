package com.aihedgefund.agent.portfolio;

import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.orchestrator.AgentSignal;
import com.aihedgefund.orchestrator.AgentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PortfolioManagerAgent 质量价值三重门槛逻辑单元测试。
 */
class PortfolioManagerAgentTest {

    private PortfolioManagerAgent agent;

    @BeforeEach
    void setUp() {
        LlmClientFactory mockFactory = Mockito.mock(LlmClientFactory.class);
        StructuredOutputHelper mockHelper = Mockito.mock(StructuredOutputHelper.class);
        agent = new PortfolioManagerAgent(mockFactory, mockHelper);
    }

    /** 三组门槛全部满足 → buy */
    @Test
    void analyze_allGatesPass_returnsBuy() {
        AgentState state = baseState();
        // 护城河组
        state.putSignal("warren_buffett", "AAPL", AgentSignal.bullish(80, "宽护城河"));
        state.putSignal("charlie_munger", "AAPL", AgentSignal.bullish(75, "高ROIC"));
        // 低估值组
        state.putSignal("ben_graham", "AAPL", AgentSignal.bullish(70, "安全边际充足"));
        // DCF 内在价值组
        state.putSignal("aswath_damodaran", "AAPL", AgentSignal.bullish(72, "低于内在价值"));

        agent.analyze(state, List.of("AAPL"));

        Map<String, Object> decision = decisionOf(state, "AAPL");
        assertThat(decision.get("action")).isEqualTo("buy");
        assertThat(decision.get("gate_passed")).isEqualTo(true);
        assertThat((int) decision.get("quantity")).isGreaterThan(0);
    }

    /** 护城河组出现看空 → 严格否决 → hold */
    @Test
    void analyze_bearishInGate_vetoesToHold() {
        AgentState state = baseState();
        state.putSignal("warren_buffett", "AAPL", AgentSignal.bullish(80, "宽护城河"));
        state.putSignal("charlie_munger", "AAPL", AgentSignal.bearish(60, "管理层存疑"));
        state.putSignal("ben_graham", "AAPL", AgentSignal.bullish(70, "安全边际充足"));
        state.putSignal("aswath_damodaran", "AAPL", AgentSignal.bullish(72, "低于内在价值"));

        agent.analyze(state, List.of("AAPL"));

        Map<String, Object> decision = decisionOf(state, "AAPL");
        assertThat(decision.get("action")).isEqualTo("hold");
        assertThat(decision.get("gate_passed")).isEqualTo(false);
        assertThat((int) decision.get("quantity")).isZero();
    }

    /** DCF 组无任何信号（未选相关分析师）→ 缺组淘汰 → hold */
    @Test
    void analyze_missingGroup_filteredToHold() {
        AgentState state = baseState();
        state.putSignal("warren_buffett", "AAPL", AgentSignal.bullish(80, "宽护城河"));
        state.putSignal("charlie_munger", "AAPL", AgentSignal.bullish(75, "高ROIC"));
        state.putSignal("ben_graham", "AAPL", AgentSignal.bullish(70, "安全边际充足"));
        // 故意不提供 DCF 组（aswath_damodaran / valuation_analyst）

        agent.analyze(state, List.of("AAPL"));

        Map<String, Object> decision = decisionOf(state, "AAPL");
        assertThat(decision.get("action")).isEqualTo("hold");
        assertThat(decision.get("gate_passed")).isEqualTo(false);
        @SuppressWarnings("unchecked")
        List<String> detail = (List<String>) decision.get("gate_detail");
        assertThat(detail).anyMatch(s -> s.contains("DCF内在价值组") && s.contains("缺组"));
    }

    /** 低估值组全中性、无看多 → 未确认 → hold */
    @Test
    void analyze_groupAllNeutral_notConfirmedToHold() {
        AgentState state = baseState();
        state.putSignal("warren_buffett", "AAPL", AgentSignal.bullish(80, "宽护城河"));
        state.putSignal("charlie_munger", "AAPL", AgentSignal.bullish(75, "高ROIC"));
        state.putSignal("ben_graham", "AAPL", AgentSignal.neutral(50, "估值中性"));
        state.putSignal("aswath_damodaran", "AAPL", AgentSignal.bullish(72, "低于内在价值"));

        agent.analyze(state, List.of("AAPL"));

        Map<String, Object> decision = decisionOf(state, "AAPL");
        assertThat(decision.get("action")).isEqualTo("hold");
        assertThat(decision.get("gate_passed")).isEqualTo(false);
    }

    /** valuation_analyst 同时满足低估值组与 DCF 组 */
    @Test
    void analyze_valuationAnalystCoversTwoGroups_returnsBuy() {
        AgentState state = baseState();
        state.putSignal("warren_buffett", "AAPL", AgentSignal.bullish(80, "宽护城河"));
        state.putSignal("charlie_munger", "AAPL", AgentSignal.bullish(75, "高ROIC"));
        // 仅靠 valuation_analyst 同时覆盖低估值组与 DCF 组
        state.putSignal("valuation_analyst", "AAPL", AgentSignal.bullish(68, "DCF 折价 25%"));

        agent.analyze(state, List.of("AAPL"));

        Map<String, Object> decision = decisionOf(state, "AAPL");
        assertThat(decision.get("action")).isEqualTo("buy");
        assertThat(decision.get("gate_passed")).isEqualTo(true);
    }

    @Test
    void getAgentId_returnsCorrectId() {
        assertThat(agent.getAgentId()).isEqualTo("portfolio_manager");
    }

    // ---- helpers ----

    private AgentState baseState() {
        AgentState state = new AgentState();
        state.putData("initial_cash", 100_000);
        return state;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decisionOf(AgentState state, String ticker) {
        Map<String, Object> decisions = (Map<String, Object>) state.getData("decisions");
        assertThat(decisions).containsKey(ticker);
        return (Map<String, Object>) decisions.get(ticker);
    }
}
