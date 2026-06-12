package com.aihedgefund.data.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 金融数据内存缓存（替代 Python src/data/cache.py）
 * 按 ticker 缓存价格、财务指标等数据，避免重复 API 调用
 */
@Component
public class FinancialDataCache {

    private static final Logger log = LoggerFactory.getLogger(FinancialDataCache.class);

    private final Map<String, List<Map<String, Object>>> pricesCache = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> financialMetricsCache = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> lineItemsCache = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> insiderTradesCache = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> companyNewsCache = new ConcurrentHashMap<>();

    public List<Map<String, Object>> getPrices(String ticker) {
        return pricesCache.get(ticker);
    }

    public void setPrices(String ticker, List<Map<String, Object>> data) {
        pricesCache.put(ticker, mergeData(pricesCache.get(ticker), data, "time"));
        log.debug("缓存价格数据, ticker={}, 条数={}", ticker, pricesCache.get(ticker).size());
    }

    public List<Map<String, Object>> getFinancialMetrics(String ticker) {
        return financialMetricsCache.get(ticker);
    }

    public void setFinancialMetrics(String ticker, List<Map<String, Object>> data) {
        financialMetricsCache.put(ticker, mergeData(financialMetricsCache.get(ticker), data, "report_period"));
    }

    public List<Map<String, Object>> getLineItems(String ticker) {
        return lineItemsCache.get(ticker);
    }

    public void setLineItems(String ticker, List<Map<String, Object>> data) {
        lineItemsCache.put(ticker, mergeData(lineItemsCache.get(ticker), data, "report_period"));
    }

    public List<Map<String, Object>> getInsiderTrades(String ticker) {
        return insiderTradesCache.get(ticker);
    }

    public void setInsiderTrades(String ticker, List<Map<String, Object>> data) {
        insiderTradesCache.put(ticker, mergeData(insiderTradesCache.get(ticker), data, "filing_date"));
    }

    public List<Map<String, Object>> getCompanyNews(String ticker) {
        return companyNewsCache.get(ticker);
    }

    public void setCompanyNews(String ticker, List<Map<String, Object>> data) {
        companyNewsCache.put(ticker, mergeData(companyNewsCache.get(ticker), data, "date"));
    }

    /**
     * 合并数据，按 keyField 去重，避免重复条目
     */
    private List<Map<String, Object>> mergeData(List<Map<String, Object>> existing,
                                                  List<Map<String, Object>> newData,
                                                  String keyField) {
        if (existing == null || existing.isEmpty()) {
            return new ArrayList<>(newData);
        }
        Set<Object> existingKeys = new HashSet<>();
        for (Map<String, Object> item : existing) {
            existingKeys.add(item.get(keyField));
        }
        List<Map<String, Object>> merged = new ArrayList<>(existing);
        for (Map<String, Object> item : newData) {
            if (!existingKeys.contains(item.get(keyField))) {
                merged.add(item);
            }
        }
        return merged;
    }
}
