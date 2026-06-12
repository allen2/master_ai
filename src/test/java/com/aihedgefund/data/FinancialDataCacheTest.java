package com.aihedgefund.data;

import com.aihedgefund.data.cache.FinancialDataCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialDataCacheTest {

    private FinancialDataCache cache;

    @BeforeEach
    void setUp() {
        cache = new FinancialDataCache();
    }

    @Test
    void getPrices_missCacheReturnsNull() {
        assertThat(cache.getPrices("AAPL")).isNull();
    }

    @Test
    void setPrices_thenGetReturnsData() {
        Map<String, Object> row = new HashMap<>();
        row.put("time", "2024-01-01");
        row.put("close", 180.0);
        cache.setPrices("AAPL", List.of(row));
        assertThat(cache.getPrices("AAPL")).hasSize(1);
    }

    @Test
    void setPrices_mergesNoDuplicates() {
        Map<String, Object> row1 = new HashMap<>();
        row1.put("time", "2024-01-01");
        row1.put("close", 180.0);
        cache.setPrices("AAPL", List.of(row1));

        Map<String, Object> rowDup = new HashMap<>();
        rowDup.put("time", "2024-01-01");
        rowDup.put("close", 180.0);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("time", "2024-01-02");
        row2.put("close", 182.0);
        cache.setPrices("AAPL", List.of(rowDup, row2));

        assertThat(cache.getPrices("AAPL")).hasSize(2);
    }

    @Test
    void differentTickers_isolatedCaches() {
        Map<String, Object> rowA = new HashMap<>();
        rowA.put("time", "2024-01-01");
        cache.setPrices("AAPL", List.of(rowA));
        assertThat(cache.getPrices("MSFT")).isNull();
    }

    @Test
    void getFinancialMetrics_cacheAndRetrieve() {
        Map<String, Object> metric = new HashMap<>();
        metric.put("report_period", "2024-Q1");
        metric.put("revenue", 100000.0);
        cache.setFinancialMetrics("AAPL", List.of(metric));
        assertThat(cache.getFinancialMetrics("AAPL")).hasSize(1);
        assertThat(cache.getFinancialMetrics("AAPL").get(0).get("report_period")).isEqualTo("2024-Q1");
    }
}
