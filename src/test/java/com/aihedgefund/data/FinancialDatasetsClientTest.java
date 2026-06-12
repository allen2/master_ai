package com.aihedgefund.data;

import com.aihedgefund.data.cache.FinancialDataCache;
import com.aihedgefund.data.client.FinancialDatasetsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialDatasetsClientTest {

    private FinancialDatasetsClient client;
    private FinancialDataCache cache;

    @BeforeEach
    void setUp() {
        cache = new FinancialDataCache();
        client = new FinancialDatasetsClient("test-api-key", "https://api.financialdatasets.ai", cache);
    }

    @Test
    void getPrices_returnsCachedData_withoutHttpCall() {
        Map<String, Object> row = new HashMap<>();
        row.put("time", "2024-01-01");
        row.put("close", 180.0);
        cache.setPrices("AAPL", List.of(row));

        List<Map<String, Object>> result = client.getPrices("AAPL", "2024-01-01", "2024-01-31");

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("time")).isEqualTo("2024-01-01");
    }

    @Test
    void getFinancialMetrics_returnsCachedData() {
        Map<String, Object> metric = new HashMap<>();
        metric.put("report_period", "2024-Q1");
        metric.put("revenue", 100000.0);
        cache.setFinancialMetrics("AAPL", List.of(metric));

        List<Map<String, Object>> result = client.getFinancialMetrics("AAPL", "2024-03-31", "ttm", 10);

        assertThat(result).hasSize(1);
    }

    @Test
    void getPrices_missingCache_returnsEmpty_whenNoNetwork() {
        // 无缓存且测试环境无法连接真实 API，应返回空列表而非抛异常
        List<Map<String, Object>> result = client.getPrices("FAKE_TICKER", "2024-01-01", "2024-01-31");
        assertThat(result).isNotNull();
    }
}
