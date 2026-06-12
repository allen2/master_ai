package com.aihedgefund.agent;

import com.aihedgefund.agent.analyst.SentimentAgent;
import com.aihedgefund.data.client.FinancialDatasetsClient;
import com.aihedgefund.orchestrator.AgentSignal;
import com.aihedgefund.orchestrator.AgentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SentimentAgentTest {

    private FinancialDatasetsClient mockClient;
    private SentimentAgent agent;

    @BeforeEach
    void setUp() {
        mockClient = Mockito.mock(FinancialDatasetsClient.class);
        agent = new SentimentAgent(mockClient);
    }

    private List<Map<String, Object>> buildTrades(int buys, int sells) {
        List<Map<String, Object>> trades = new ArrayList<>();
        for (int i = 0; i < buys; i++) {
            Map<String, Object> t = new HashMap<>();
            t.put("transaction_type", "Purchase");
            trades.add(t);
        }
        for (int i = 0; i < sells; i++) {
            Map<String, Object> t = new HashMap<>();
            t.put("transaction_type", "Sale");
            trades.add(t);
        }
        return trades;
    }

    @Test
    void analyze_mostlyBuys_returnsBullish() {
        when(mockClient.getInsiderTrades(eq("AAPL"), any(), anyInt()))
                .thenReturn(buildTrades(8, 2));

        AgentState state = new AgentState();
        state.putData("end_date", "2024-12-31");
        agent.analyze(state, List.of("AAPL"));

        AgentSignal signal = state.getSignal("sentiment_analyst", "AAPL");
        assertThat(signal.getSignal()).isEqualTo("bullish");
    }

    @Test
    void analyze_mostlySells_returnsBearish() {
        when(mockClient.getInsiderTrades(eq("TSLA"), any(), anyInt()))
                .thenReturn(buildTrades(1, 9));

        AgentState state = new AgentState();
        state.putData("end_date", "2024-12-31");
        agent.analyze(state, List.of("TSLA"));

        AgentSignal signal = state.getSignal("sentiment_analyst", "TSLA");
        assertThat(signal.getSignal()).isEqualTo("bearish");
    }

    @Test
    void analyze_noTrades_returnsNeutral() {
        when(mockClient.getInsiderTrades(any(), any(), anyInt())).thenReturn(List.of());

        AgentState state = new AgentState();
        agent.analyze(state, List.of("X"));

        AgentSignal signal = state.getSignal("sentiment_analyst", "X");
        assertThat(signal.getSignal()).isEqualTo("neutral");
    }
}
