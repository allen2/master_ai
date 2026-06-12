package com.aihedgefund.data.client;

import com.aihedgefund.data.cache.FinancialDataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Financial Datasets API 客户端（替代 Python src/tools/api.py）
 * 支持内存缓存和 429 限速重试（线性退避：60s / 90s / 120s）
 */
@Component
public class FinancialDatasetsClient {

    private static final Logger log = LoggerFactory.getLogger(FinancialDatasetsClient.class);

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 60_000L;

    private final RestClient restClient;
    private final FinancialDataCache cache;

    public FinancialDatasetsClient(
            @Value("${financial-datasets.api-key:}") String apiKey,
            @Value("${financial-datasets.base-url:https://api.financialdatasets.ai}") String baseUrl,
            FinancialDataCache cache) {
        this.cache = cache;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-KEY", apiKey)
                .build();
        log.info("FinancialDatasetsClient 初始化, baseUrl={}", baseUrl);
    }

    /**
     * 获取历史价格（优先读缓存）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPrices(String ticker, String startDate, String endDate) {
        List<Map<String, Object>> cached = cache.getPrices(ticker);
        if (cached != null && !cached.isEmpty()) {
            log.debug("命中价格缓存, ticker={}", ticker);
            return cached;
        }

        log.info("获取价格数据, ticker={}, start={}, end={}", ticker, startDate, endDate);
        Map<String, Object> response = executeWithRetry(() ->
                restClient.get()
                        .uri("/prices/?ticker={t}&interval=day&interval_multiplier=1&start_date={s}&end_date={e}",
                                ticker, startDate, endDate)
                        .retrieve()
                        .body(Map.class));

        if (response == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> prices = (List<Map<String, Object>>) response.getOrDefault("prices", List.of());
        if (!prices.isEmpty()) {
            cache.setPrices(ticker, prices);
        }
        return prices;
    }

    /**
     * 获取财务指标（优先读缓存）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getFinancialMetrics(String ticker, String endDate, String period, int limit) {
        List<Map<String, Object>> cached = cache.getFinancialMetrics(ticker);
        if (cached != null && !cached.isEmpty()) {
            log.debug("命中财务指标缓存, ticker={}", ticker);
            return cached;
        }

        log.info("获取财务指标, ticker={}, period={}, limit={}", ticker, period, limit);
        Map<String, Object> response = executeWithRetry(() ->
                restClient.get()
                        .uri("/financial-metrics/?ticker={t}&report_period_lte={e}&limit={l}&period={p}",
                                ticker, endDate, limit, period)
                        .retrieve()
                        .body(Map.class));

        if (response == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> metrics = (List<Map<String, Object>>) response.getOrDefault("financial_metrics", List.of());
        if (!metrics.isEmpty()) {
            cache.setFinancialMetrics(ticker, metrics);
        }
        return metrics;
    }

    /**
     * 带限速重试的 HTTP 请求（替代 Python _make_api_request 的 429 处理）
     * 线性退避：60s / 90s / 120s
     */
    private <T> T executeWithRetry(ApiCall<T> call) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return call.execute();
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean isRateLimit = msg.contains("429") || msg.contains("Too Many Requests");

                if (isRateLimit && attempt < MAX_RETRIES) {
                    long waitMs = INITIAL_BACKOFF_MS + (30_000L * attempt);
                    log.warn("API 限速 (429)，第 {}/{} 次重试，等待 {}s", attempt + 1, MAX_RETRIES, waitMs / 1000);
                    try {
                        Thread.sleep(waitMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                } else {
                    log.error("API 调用失败 (attempt={}/{}): {}", attempt + 1, MAX_RETRIES + 1, e.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * 获取财务明细行（revenue, net_income, free_cash_flow 等）
     * <p>
     * 使用 /financials/ 端点，一次性获取利润表+资产负债表+现金流量表，
     * 然后根据 lineItems 筛选需要的字段。
     * 注意：free_cash_flow 在原始返回中为 null，
     * 需要用 operating_cash_flow - capital_expenditure 计算。
     * </p>
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getLineItems(String ticker, List<String> lineItems,
                                                   String endDate, String period, int limit) {
        List<Map<String, Object>> cached = cache.getLineItems(ticker);
        if (cached != null && !cached.isEmpty()) {
            log.debug("命中财务明细缓存, ticker={}", ticker);
            return cached;
        }

        log.info("获取财务明细, ticker={}, items={}", ticker, lineItems);
        Map<String, Object> response = executeWithRetry(() ->
                restClient.get()
                        .uri("/financials/?ticker={t}&period={p}&limit={l}",
                                ticker, period, limit)
                        .retrieve()
                        .body(Map.class));

        if (response == null) {
            return Collections.emptyList();
        }

        // 响应结构: {financials: {income_statements:[], balance_sheets:[], cash_flow_statements:[]}}
        Map<String, Object> financials = (Map<String, Object>) response.get("financials");
        if (financials == null) {
            log.warn("financials 响应中缺少 'financials' 字段, ticker={}", ticker);
            return Collections.emptyList();
        }

        List<Map<String, Object>> incomeStmts = (List<Map<String, Object>>) financials.getOrDefault("income_statements", List.of());
        List<Map<String, Object>> balanceStmts = (List<Map<String, Object>>) financials.getOrDefault("balance_sheets", List.of());
        List<Map<String, Object>> cashFlowStmts = (List<Map<String, Object>>) financials.getOrDefault("cash_flow_statements", List.of());

        // 合并为统一列表，每条记录带上来源标记
        List<Map<String, Object>> result = new java.util.ArrayList<>();

        for (Map<String, Object> stmt : incomeStmts) {
            Map<String, Object> row = new java.util.LinkedHashMap<>(stmt);
            row.put("_statement", "income");
            result.add(row);
        }
        for (Map<String, Object> stmt : balanceStmts) {
            Map<String, Object> row = new java.util.LinkedHashMap<>(stmt);
            row.put("_statement", "balance");
            result.add(row);
        }
        for (Map<String, Object> stmt : cashFlowStmts) {
            Map<String, Object> row = new java.util.LinkedHashMap<>(stmt);
            row.put("_statement", "cash_flow");
            result.add(row);
        }

        // 计算 free_cash_flow（原始数据中为 null）
        for (Map<String, Object> row : result) {
            if ("cash_flow".equals(row.get("_statement"))) {
                Object ocf = row.get("net_cash_flow_from_operations");
                Object capex = row.get("capital_expenditure");
                if (ocf instanceof Number && capex instanceof Number) {
                    row.put("free_cash_flow", ((Number) ocf).doubleValue() - ((Number) capex).doubleValue());
                }
            }
        }

        if (!result.isEmpty()) {
            cache.setLineItems(ticker, result);
        }
        return result;
    }

    /**
     * 获取内部人交易记录
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getInsiderTrades(String ticker, String endDate, int limit) {
        List<Map<String, Object>> cached = cache.getInsiderTrades(ticker);
        if (cached != null && !cached.isEmpty()) {
            log.debug("命中内部人交易缓存, ticker={}", ticker);
            return cached;
        }

        log.info("获取内部人交易, ticker={}", ticker);
        Map<String, Object> response = executeWithRetry(() ->
                restClient.get()
                        .uri("/insider-trades/?ticker={t}&filing_date_lte={e}&limit={l}",
                                ticker, endDate, limit)
                        .retrieve()
                        .body(Map.class));

        if (response == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> trades = (List<Map<String, Object>>) response.getOrDefault("insider_trades", List.of());
        if (!trades.isEmpty()) {
            cache.setInsiderTrades(ticker, trades);
        }
        return trades;
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T execute() throws Exception;
    }
}
