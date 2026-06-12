package com.aihedgefund.agent;

import com.aihedgefund.agent.analyst.TechnicalAgent;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class TechnicalAgentTest {

    private FinancialDatasetsClient mockClient;
    private TechnicalAgent agent;

    @BeforeEach
    void setUp() {
        mockClient = Mockito.mock(FinancialDatasetsClient.class);
        agent = new TechnicalAgent(mockClient);
    }

    /**
     * 构造锯齿形价格序列：整体趋势 trend，叠加小幅波动，模拟真实价格走势
     * 避免 RSI 因纯单调序列触发极值，导致趋势信号被抵消
     */
    private List<Map<String, Object>> buildPrices(int count, double startPrice, double trend) {
        List<Map<String, Object>> prices = new ArrayList<>();
        double price = startPrice;
        for (int i = 0; i < count; i++) {
            Map<String, Object> bar = new HashMap<>();
            // 每 3 天中：2 天顺趋势，1 天小幅回调，整体方向不变
            double step = (i % 3 == 2) ? -Math.abs(trend) * 0.3 : trend * 1.15;
            price = Math.max(1, price + step);
            bar.put("close", price);
            bar.put("time", "2024-01-" + String.format("%02d", (i % 28) + 1));
            prices.add(bar);
        }
        return prices;
    }

    @Test
    void analyze_risingTrend_returnsBullish() {
        // 上升趋势：MA5 > MA20
        when(mockClient.getPrices(eq("AAPL"), any(), any()))
                .thenReturn(buildPrices(30, 100, 2.0));

        AgentState state = new AgentState();
        state.putData("end_date", "2024-12-31");
        state.putData("start_date", "2024-01-01");
        agent.analyze(state, List.of("AAPL"));

        AgentSignal signal = state.getSignal("technical_analyst", "AAPL");
        assertThat(signal).isNotNull();
        assertThat(signal.getSignal()).isEqualTo("bullish");
    }

    @Test
    void analyze_fallingTrend_returnsBearish() {
        // 下降趋势：MA5 < MA20
        when(mockClient.getPrices(eq("MSFT"), any(), any()))
                .thenReturn(buildPrices(30, 200, -2.0));

        AgentState state = new AgentState();
        state.putData("end_date", "2024-12-31");
        state.putData("start_date", "2024-01-01");
        agent.analyze(state, List.of("MSFT"));

        AgentSignal signal = state.getSignal("technical_analyst", "MSFT");
        assertThat(signal.getSignal()).isEqualTo("bearish");
    }

    @Test
    void analyze_insufficientData_returnsNeutral() {
        when(mockClient.getPrices(any(), any(), any())).thenReturn(buildPrices(5, 100, 1.0));

        AgentState state = new AgentState();
        agent.analyze(state, List.of("X"));

        AgentSignal signal = state.getSignal("technical_analyst", "X");
        assertThat(signal.getSignal()).isEqualTo("neutral");
    }

    @Test
    void getAgentId_returnsCorrectId() {
        assertThat(agent.getAgentId()).isEqualTo("technical_analyst");
    }
}
