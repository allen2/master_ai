package com.aihedgefund.llm;

import com.aihedgefund.data.client.FinancialDatasetsClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 金融数据工具集（Spring AI @Tool）
 * 供 LLM Agent 主动调用获取所需数据，同时暴露为 MCP Server 工具。
 */
@Component
public class FinancialDataTools {

    private static final Logger log = LoggerFactory.getLogger(FinancialDataTools.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FinancialDatasetsClient client;

    public FinancialDataTools(FinancialDatasetsClient client) {
        this.client = client;
    }

    /**
     * 获取股票历史价格数据（日线 OHLCV）。
     *
     * @param ticker    股票代码，如 AAPL
     * @param startDate 开始日期，格式 yyyy-MM-dd
     * @param endDate   结束日期，格式 yyyy-MM-dd
     * @return JSON 数组字符串，每个元素为一天的 OHLCV 数据
     */
    @Tool(description = "获取海外股票历史价格数据（日线 OHLCV），返回 JSON 数组。用于技术分析、趋势判断、波动率计算。")
    public String getPrices(String ticker, String startDate, String endDate) {
        List<Map<String, Object>> data = client.getPrices(ticker, startDate, endDate);
        log.debug("[Tool] getPrices: ticker={}, {} ~ {} data {}", ticker, startDate, endDate,data);
        return toJson(data);
    }

    /**
     * 获取股票财务指标数据。
     *
     * @param ticker  股票代码，如 AAPL
     * @param endDate 截止日期，格式 yyyy-MM-dd
     * @param period  时间维度：ttm（滚动12月）或 annual（年度）
     * @param limit   返回条数，建议 1-5
     * @return JSON 数组字符串，包含 ROE、毛利率、净利率等指标
     */
    @Tool(description = "获取海外股票财务指标，包括 ROE、毛利率、净利率、市盈率、负债率等关键指标。"
            + "period 填 ttm（滚动12月）或 annual（年度），limit 填 1-5。")
    public String getFinancialMetrics(String ticker, String endDate, String period, int limit) {

        List<Map<String, Object>> data = client.getFinancialMetrics(ticker, endDate, period, limit);
        log.debug("[Tool] getFinancialMetrics: ticker={}, period={}, limit={} data={}", ticker, period, limit,data);
        return toJson(data);
    }

    /**
     * 获取股票内部人交易记录。
     *
     * @param ticker  股票代码，如 AAPL
     * @param endDate 截止日期，格式 yyyy-MM-dd
     * @param limit   返回条数，建议 10-50
     * @return JSON 数组字符串，包含高管买卖记录
     */
    @Tool(description = "获取海外股票内部人交易记录（高管买卖动向），反映公司内部人对未来的信心。limit 填 10-50。")
    public String getInsiderTrades(String ticker, String endDate, int limit) {

        List<Map<String, Object>> data = client.getInsiderTrades(ticker, endDate, limit);
        log.debug("[Tool] getInsiderTrades: ticker={}, limit={}, data={}", ticker, limit,data);
        return toJson(data);
    }

    /**
     * 获取财务明细行数据（营收、净利润、自由现金流等）。
     *
     * @param ticker    股票代码，如 AAPL
     * @param lineItems 逗号分隔的字段名，如 revenue,net_income,free_cash_flow
     * @param endDate   截止日期，格式 yyyy-MM-dd
     * @param period    时间维度：ttm 或 annual
     * @param limit     返回条数，建议 1-5
     * @return JSON 数组字符串，包含指定财务行数据
     */
    @Tool(description = "获取海外股票财务明细行数据（营收、净利润、自由现金流等）。"
            + "lineItems 为逗号分隔字段名如 revenue,net_income,free_cash_flow；"
            + "period 填 ttm 或 annual；limit 填 1-5。")
    public String getLineItems(String ticker, String lineItems, String endDate, String period, int limit) {
        log.debug("[Tool] getLineItems: ticker={}, items={}", ticker, lineItems);
        List<String> items = java.util.Arrays.stream(lineItems.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        List<Map<String, Object>> data = client.getLineItems(ticker, items, endDate, period, limit);
        return toJson(data);
    }

    /**
     * 将数据列表序列化为 JSON 字符串。
     * 若数据为空或序列化失败，返回空数组 "[]"。
     *
     * @param data 待序列化的数据列表
     * @return JSON 数组字符串
     */
    private String toJson(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("工具返回值序列化失败: {}", e.getMessage());
            return "[]";
        }
    }
}
