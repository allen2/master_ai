package com.aihedgefund.agent;

import com.aihedgefund.agent.analyst.GrowthAgent;
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

class GrowthAgentTest {

    private FinancialDatasetsClient mockClient;
    private GrowthAgent agent;

    @BeforeEach
    void setUp() {
        mockClient = Mockito.mock(FinancialDatasetsClient.class);
        agent = new GrowthAgent(mockClient);
    }

    @Test
    void analyze_highGrowth_returnsBullish() {
        Map<String, Object> current = new HashMap<>();
        current.put("revenue", 120_000_000_000.0);
        current.put("net_income_margin", 0.30);

        Map<String, Object> prior = new HashMap<>();
        prior.put("revenue", 90_000_000_000.0);  // +33% growth
        prior.put("net_income_margin", 0.22);      // +36% growth

        when(mockClient.getFinancialMetrics(anyString(), any(), eq("annual"), anyInt()))
                .thenReturn(List.of(current, prior));

        AgentState state = new AgentState();
        state.putData("end_date", "2024-12-31");
        agent.analyze(state, List.of("AAPL"));

        AgentSignal signal = state.getSignal("growth_analyst", "AAPL");
        assertThat(signal.getSignal()).isEqualTo("bullish");
    }

    @Test
    void analyze_negativeGrowth_returnsBearish() {
        Map<String, Object> current = new HashMap<>();
        current.put("revenue", 70_000_000_000.0);
        current.put("net_income_margin", 0.10);

        Map<String, Object> prior = new HashMap<>();
        prior.put("revenue", 100_000_000_000.0);  // -30% decline
        prior.put("net_income_margin", 0.18);       // -44% decline

        when(mockClient.getFinancialMetrics(anyString(), any(), eq("annual"), anyInt()))
                .thenReturn(List.of(current, prior));

        AgentState state = new AgentState();
        state.putData("end_date", "2024-12-31");
        agent.analyze(state, List.of("TSLA"));

        AgentSignal signal = state.getSignal("growth_analyst", "TSLA");
        assertThat(signal.getSignal()).isEqualTo("bearish");
    }

    @Test
    void analyze_insufficientHistory_returnsNeutral() {
        when(mockClient.getFinancialMetrics(anyString(), any(), any(), anyInt()))
                .thenReturn(List.of());

        AgentState state = new AgentState();
        agent.analyze(state, List.of("NEW"));

        AgentSignal signal = state.getSignal("growth_analyst", "NEW");
        assertThat(signal.getSignal()).isEqualTo("neutral");
    }
}
