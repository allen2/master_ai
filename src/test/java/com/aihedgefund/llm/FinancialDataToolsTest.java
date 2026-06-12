package com.aihedgefund.llm;

import com.aihedgefund.data.client.FinancialDatasetsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class FinancialDataToolsTest {

    private FinancialDatasetsClient mockClient;
    private FinancialDataTools tools;

    @BeforeEach
    void setUp() {
        mockClient = Mockito.mock(FinancialDatasetsClient.class);
        tools = new FinancialDataTools(mockClient);
    }

    @Test
    void getPrices_returnJsonArray() {
        when(mockClient.getPrices("AAPL", "2024-01-01", "2024-12-31"))
                .thenReturn(List.of(Map.of("close", 150.0, "time", "2024-01-02")));

        String result = tools.getPrices("AAPL", "2024-01-01", "2024-12-31");

        assertThat(result).startsWith("[");
        assertThat(result).contains("150.0");
    }

    @Test
    void getFinancialMetrics_returnJsonArray() {
        when(mockClient.getFinancialMetrics("AAPL", "2024-12-31", "ttm", 3))
                .thenReturn(List.of(Map.of("return_on_equity", 1.64, "net_margin", 0.27)));

        String result = tools.getFinancialMetrics("AAPL", "2024-12-31", "ttm", 3);

        assertThat(result).contains("return_on_equity");
        assertThat(result).contains("1.64");
    }

    @Test
    void getInsiderTrades_returnJsonArray() {
        when(mockClient.getInsiderTrades("AAPL", "2024-12-31", 20))
                .thenReturn(List.of(Map.of("transaction_type", "purchase", "shares", 1000)));

        String result = tools.getInsiderTrades("AAPL", "2024-12-31", 20);

        assertThat(result).contains("purchase");
    }

    @Test
    void getLineItems_parsesCommaSeparated_andCallsClient() {
        when(mockClient.getLineItems(eq("AAPL"), anyList(), eq("2024-12-31"), eq("annual"), eq(3)))
                .thenReturn(List.of(Map.of("revenue", 400_000_000_000L)));

        String result = tools.getLineItems("AAPL", "revenue,net_income", "2024-12-31", "annual", 3);

        assertThat(result).contains("revenue");
    }

    @Test
    void nullClientResponse_returnsEmptyJsonArray() {
        when(mockClient.getPrices(anyString(), anyString(), anyString())).thenReturn(null);

        String result = tools.getPrices("FAKE", "2024-01-01", "2024-12-31");

        assertThat(result).isEqualTo("[]");
    }
}
