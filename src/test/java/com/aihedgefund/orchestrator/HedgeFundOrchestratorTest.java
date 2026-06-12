package com.aihedgefund.orchestrator;

import com.aihedgefund.agent.BaseAgent;
import com.aihedgefund.agent.portfolio.PortfolioManagerAgent;
import com.aihedgefund.agent.risk.RiskManagerAgent;
import com.aihedgefund.data.client.FinancialDatasetsClient;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.model.req.HedgeFundRunReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class HedgeFundOrchestratorTest {

    private FinancialDatasetsClient mockClient;
    private RiskManagerAgent riskManager;
    private PortfolioManagerAgent portfolioManager;

    @BeforeEach
    void setUp() {
        mockClient = Mockito.mock(FinancialDatasetsClient.class);
        when(mockClient.getPrices(anyString(), anyString(), anyString())).thenReturn(List.of());
        when(mockClient.getFinancialMetrics(anyString(), anyString(), anyString(), anyInt())).thenReturn(List.of());
        when(mockClient.getInsiderTrades(anyString(), anyString(), anyInt())).thenReturn(List.of());

        riskManager = new RiskManagerAgent(mockClient);

        LlmClientFactory mockFactory = Mockito.mock(LlmClientFactory.class);
        StructuredOutputHelper mockHelper = Mockito.mock(StructuredOutputHelper.class);
        portfolioManager = new PortfolioManagerAgent(mockFactory, mockHelper);
    }

    @Test
    void run_withStubAgent_populatesAnalystSignals() {
        BaseAgent stubAgent = new BaseAgent() {
            @Override public String getAgentId() { return "stub_agent"; }
            @Override public String getDisplayName() { return "Stub Agent"; }
            @Override public void analyze(AgentState state, List<String> tickers) {
                tickers.forEach(t -> state.putSignal(getAgentId(), t, AgentSignal.bullish(75, "stub")));
            }
        };

        HedgeFundOrchestrator orchestrator = new HedgeFundOrchestrator(
                List.of(stubAgent), riskManager, portfolioManager, 5);

        HedgeFundRunReq req = new HedgeFundRunReq();
        req.setTickers(List.of("AAPL", "MSFT"));

        WorkflowResult result = orchestrator.run(req, null);

        assertThat(result.getAnalystSignals()).containsKey("stub_agent");
        assertThat(result.getAnalystSignals().get("stub_agent")).containsKeys("AAPL", "MSFT");
        assertThat(result.getAnalystSignals().get("stub_agent").get("AAPL").getSignal()).isEqualTo("bullish");
    }

    @Test
    void run_filterSelectedAnalysts_onlyRunsSelected() {
        BaseAgent agentA = new BaseAgent() {
            @Override public String getAgentId() { return "agent_a"; }
            @Override public String getDisplayName() { return "Agent A"; }
            @Override public void analyze(AgentState state, List<String> tickers) {
                tickers.forEach(t -> state.putSignal(getAgentId(), t, AgentSignal.bullish(50, "a")));
            }
        };
        BaseAgent agentB = new BaseAgent() {
            @Override public String getAgentId() { return "agent_b"; }
            @Override public String getDisplayName() { return "Agent B"; }
            @Override public void analyze(AgentState state, List<String> tickers) {
                tickers.forEach(t -> state.putSignal(getAgentId(), t, AgentSignal.bearish(50, "b")));
            }
        };

        HedgeFundOrchestrator orchestrator = new HedgeFundOrchestrator(
                List.of(agentA, agentB), riskManager, portfolioManager, 5);

        HedgeFundRunReq req = new HedgeFundRunReq();
        req.setTickers(List.of("AAPL"));
        req.setSelectedAnalysts(List.of("agent_a"));

        WorkflowResult result = orchestrator.run(req, null);

        assertThat(result.getAnalystSignals()).containsKey("agent_a");
        assertThat(result.getAnalystSignals()).doesNotContainKey("agent_b");
    }

    @Test
    void run_decisionsContainAllTickers() {
        BaseAgent bullAgent = new BaseAgent() {
            @Override public String getAgentId() { return "bull"; }
            @Override public String getDisplayName() { return "Bull Agent"; }
            @Override public void analyze(AgentState state, List<String> tickers) {
                tickers.forEach(t -> state.putSignal(getAgentId(), t, AgentSignal.bullish(80, "bull")));
            }
        };

        HedgeFundOrchestrator orchestrator = new HedgeFundOrchestrator(
                List.of(bullAgent), riskManager, portfolioManager, 5);

        HedgeFundRunReq req = new HedgeFundRunReq();
        req.setTickers(List.of("AAPL", "NVDA"));
        req.setInitialCash(100000.0);

        WorkflowResult result = orchestrator.run(req, null);

        assertThat(result.getDecisions()).containsKeys("AAPL", "NVDA");
        assertThat(result.getStatus()).isEqualTo("complete");
    }

    @Test
    void run_pushesSignalEventPerAgentImmediately() {
        BaseAgent stubAgent = new BaseAgent() {
            @Override public String getAgentId() { return "warren_buffett"; }
            @Override public String getDisplayName() { return "沃伦·巴菲特"; }
            @Override public void analyze(AgentState state, List<String> tickers) {
                tickers.forEach(t -> state.putSignal(getAgentId(), t, AgentSignal.bullish(88, "宽护城河")));
            }
        };

        HedgeFundOrchestrator orchestrator = new HedgeFundOrchestrator(
                List.of(stubAgent), riskManager, portfolioManager, 5);

        HedgeFundRunReq req = new HedgeFundRunReq();
        req.setTickers(List.of("AAPL"));

        CapturingSseEmitter emitter = new CapturingSseEmitter();
        orchestrator.run(req, emitter);

        String captured = emitter.captured();
        // 应推送了 signal 事件，且包含该 agent 的标识与信号内容
        assertThat(captured).contains("signal");
        assertThat(captured).contains("warren_buffett");
        assertThat(captured).contains("bullish");
    }

    /** 捕获型 SseEmitter：记录所有写入的事件数据片段，便于断言事件内容 */
    private static final class CapturingSseEmitter extends SseEmitter {

        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void send(SseEventBuilder builder) {
            // SseEventBuilder.build() 返回事件的各数据片段（含 "event:"、事件名、payload 等）
            builder.build().forEach(entry -> buffer.append(entry.getData()));
        }

        String captured() {
            return buffer.toString();
        }
    }
}
