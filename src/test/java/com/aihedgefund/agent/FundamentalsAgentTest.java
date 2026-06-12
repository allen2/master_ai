package com.aihedgefund.agent;

import com.aihedgefund.agent.analyst.FundamentalsAgent;
import com.aihedgefund.data.client.FinancialDatasetsClient;
import com.aihedgefund.orchestrator.AgentSignal;
import com.aihedgefund.orchestrator.AgentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class FundamentalsAgentTest {

    private FinancialDatasetsClient mockClient;
    private FundamentalsAgent agent;

    @BeforeEach
    void setUp() {
        mockClient = Mockito.mock(FinancialDatasetsClient.class);
        agent = new FundamentalsAgent(mockClient);
    }

    @Test
    void analyze_strongFundamentals_returnsBullish() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("return_on_equity", 0.22);   // > 15% ✓
        metrics.put("debt_to_equity", 0.3);       // < 0.5 ✓
        metrics.put("gross_margin", 0.55);         // > 40% ✓
        metrics.put("net_margin", 0.18);           // > 10% ✓
        when(mockClient.getFinancialMetrics(eq("AAPL"), any(), any(), anyInt()))
                .thenReturn(List.of(metrics));

        AgentState state = new AgentState();
        state.putData("end_date", "2024-12-31");
        agent.analyze(state, List.of("AAPL"));

        AgentSignal signal = state.getSignal("fundamentals_analyst", "AAPL");
        assertThat(signal).isNotNull();
        assertThat(signal.getSignal()).isEqualTo("bullish");
        assertThat(signal.getConfidence()).isGreaterThan(60);
    }

    @Test
    void analyze_weakFundamentals_returnsBearish() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("return_on_equity", 0.05);   // < 15% ✗
        metrics.put("debt_to_equity", 1.5);       // > 0.5 ✗
        metrics.put("gross_margin", 0.20);         // < 40% ✗
        metrics.put("net_margin", 0.02);           // < 10% ✗
        when(mockClient.getFinancialMetrics(eq("TSLA"), any(), any(), anyInt()))
                .thenReturn(List.of(metrics));

        AgentState state = new AgentState();
        state.putData("end_date", "2024-12-31");
        agent.analyze(state, List.of("TSLA"));

        AgentSignal signal = state.getSignal("fundamentals_analyst", "TSLA");
        assertThat(signal.getSignal()).isEqualTo("bearish");
    }

    @Test
    void analyze_noData_returnsNeutral() {
        when(mockClient.getFinancialMetrics(anyString(), any(), any(), anyInt()))
                .thenReturn(List.of());

        AgentState state = new AgentState();
        agent.analyze(state, List.of("UNKNOWN"));

        AgentSignal signal = state.getSignal("fundamentals_analyst", "UNKNOWN");
        assertThat(signal.getSignal()).isEqualTo("neutral");
    }

    @Test
    void getAgentId_returnsCorrectId() {
        assertThat(agent.getAgentId()).isEqualTo("fundamentals_analyst");
    }
}
