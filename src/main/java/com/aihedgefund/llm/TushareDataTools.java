package com.aihedgefund.llm;

import com.aihedgefund.cache.ToolCallCacheService;
import com.aihedgefund.data.client.TushareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 金融数据工具集（Spring AI @Tool）
 * 供 LLM Agent 主动调用获取所需数据，同时暴露为 MCP Server 工具。
 *
 * <p>所有工具内置结果缓存（基于 {@link ToolCallCacheService}）：
 * 先用调用参数查缓存，命中则直接返回，未命中才请求 Tushare API，
 * 请求成功（结果非空）后写回缓存。该缓存对所有调用路径生效
 * （LLM 工具调用 / MCP 外部调用 / 直接 Java 调用）。</p>
 */
@Component
public class TushareDataTools {

    private static final Logger log = LoggerFactory.getLogger(TushareDataTools.class);

    /** 缓存命名空间前缀，避免与 LLM 工具调用层缓存键混淆 */
    private static final String CACHE_PREFIX = "tushare.";

    private final TushareClient client;

    private final ToolCallCacheService cacheService;

    public TushareDataTools(TushareClient client, ToolCallCacheService cacheService) {
        this.client = client;
        this.cacheService = cacheService;
    }

    // ==========================
    // 工具 1：获取日线行情
    // ==========================
    @Tool(description = "获取A股股票日线行情，返回开高低收、成交量等 OHLCV 数据。适合技术分析、价格趋势判断。")
    public String getDaily(
            @ToolParam(description = "股票TS代码，格式：6位数字.交易所后缀，如 000001.SZ（深圳）、600519.SH（上海）") String tsCode,
            @ToolParam(description = "起始日期，格式 YYYYMMDD，如 20240101") String startDate,
            @ToolParam(description = "截止日期，格式 YYYYMMDD，如 20241231") String endDate) {
        log.info("[Tool] getDaily: tsCode={}, startDate={}, endDate={}", tsCode, startDate, endDate);
        Map<String, Object> params = new HashMap<>();
        params.put("ts_code", tsCode);
        params.put("start_date", startDate);
        params.put("end_date", endDate);

        return callWithCache("daily", params);
    }

    // ==========================
    // 工具 2：获取财务指标（PE/PB/ROE）
    // ==========================
    @Tool(description = "获取A股股票财务指标，包含 PE、PB、ROE、净利润增长率、毛利率、每股收益等基本面量化指标。适合估值分析和盈利能力评估。")
    public String getFinaIndicator(
            @ToolParam(description = "股票TS代码，格式：6位数字.交易所后缀，如 000001.SZ（深圳）、600519.SH（上海）") String tsCode,
            @ToolParam(description = "报告期起始日期，格式 YYYYMMDD，如 20240101") String startDate,
            @ToolParam(description = "报告期截止日期，格式 YYYYMMDD，如 20241231") String endDate) {
        log.info("[Tool] getFinaIndicator: tsCode={}, startDate={}, endDate={}", tsCode, startDate, endDate);
        Map<String, Object> params = new HashMap<>();
        params.put("ts_code", tsCode);
        params.put("start_date", startDate);
        params.put("end_date", endDate);

        return callWithCache("fina_indicator", params);
    }

    // ==========================
    // 工具 3：获取利润表
    // ==========================
    @Tool(description = "获取A股股票利润表，包含营业收入、营业成本、净利润、毛利润等损益数据。适合营收增长分析和盈利质量评估。")
    public Mono<String> getIncome(
            @ToolParam(description = "股票TS代码，格式：6位数字.交易所后缀，如 000001.SZ（深圳）、600519.SH（上海）") String tsCode,
            @ToolParam(description = "报告期起始日期，格式 YYYYMMDD，如 20240101") String startDate,
            @ToolParam(description = "报告期截止日期，格式 YYYYMMDD，如 20241231") String endDate) {
        log.info("[Tool] getIncome: tsCode={}, startDate={}, endDate={}", tsCode, startDate, endDate);
        Map<String, Object> params = new HashMap<>();
        params.put("ts_code", tsCode);
        params.put("start_date", startDate);
        params.put("end_date", endDate);

        return callWithCacheReactive("income", params);
    }

    // ==========================
    // 工具 4：获取股票列表
    // ==========================
    @Tool(description = "获取A股全部上市股票列表，包含股票代码、名称、上市状态、所属行业、上市日期等基础信息。适合批量筛选标的或查找某只股票的TS代码。无需传入参数。")
    public String getStockBasic() {
        log.info("[Tool] getStockBasic: list_status=L");
        Map<String, Object> params = new HashMap<>();
        params.put("list_status", "L");
        params.put("exchange", "");

        return callWithCache("stock_basic", params);
    }

    // ==========================
    // 工具 5：获取指数行情
    // ==========================
    @Tool(description = "获取A股市场指数日线行情，包含开高低收、成交量等 OHLCV 数据。常用指数：沪深300（000300.SH）、上证指数（000001.SH）、创业板（399006.SZ）、中证500（000905.SH）。适合大盘趋势分析和指数对比。")
    public String getIndexDaily(
            @ToolParam(description = "指数TS代码，格式：6位数字.交易所后缀。例如：沪深300=000300.SH，上证指数=000001.SH，创业板指=399006.SZ，中证500=000905.SH") String tsCode,
            @ToolParam(description = "起始日期，格式 YYYYMMDD，如 20240101") String startDate,
            @ToolParam(description = "截止日期，格式 YYYYMMDD，如 20241231") String endDate) {
        log.info("[Tool] getIndexDaily: tsCode={}, startDate={}, endDate={}", tsCode, startDate, endDate);
        Map<String, Object> params = new HashMap<>();
        params.put("ts_code", tsCode);
        params.put("start_date", startDate);
        params.put("end_date", endDate);

        return callWithCache("index_daily", params);
    }

    // ==========================
    // 缓存通用逻辑
    // ==========================

    /**
     * 同步带缓存调用：先查缓存命中即返回，未命中调用 Tushare API 并写回缓存。
     *
     * @param apiName Tushare 接口名（如 daily、fina_indicator）
     * @param params  请求参数
     * @return 结果 JSON 字符串
     */
    private String callWithCache(String apiName, Map<String, Object> params) {
        String cacheKey = canonicalParams(params);
        String cached = readCache(apiName, cacheKey);
        if (cached != null) {
            log.info("[Tool] {} 命中缓存, params={}", apiName, cacheKey);
            return cached;
        }

        String result = client.api(apiName, params)
                .map(Object::toString)
                .doOnError(e -> log.error("[Tool] {} 失败: params={}, error={}", apiName, cacheKey, e.getMessage()))
                .block();

        writeCache(apiName, cacheKey, result);
        log.debug("[Tool] {} API 返回, length={}", apiName, result != null ? result.length() : 0);
        return result;
    }

    /**
     * 响应式带缓存调用：命中缓存返回 Mono.just(缓存)，未命中订阅 API 并在成功后写回缓存。
     *
     * @param apiName Tushare 接口名
     * @param params  请求参数
     * @return 结果 JSON 的 Mono
     */
    private Mono<String> callWithCacheReactive(String apiName, Map<String, Object> params) {
        String cacheKey = canonicalParams(params);
        String cached = readCache(apiName, cacheKey);
        if (cached != null) {
            log.info("[Tool] {} 命中缓存, params={}", apiName, cacheKey);
            return Mono.just(cached);
        }

        return client.api(apiName, params)
                .map(Object::toString)
                .doOnNext(result -> writeCache(apiName, cacheKey, result))
                .doOnError(e -> log.error("[Tool] {} 失败: params={}, error={}", apiName, cacheKey, e.getMessage()));
    }

    /**
     * 读缓存。缓存异常时降级为未命中，绝不影响数据获取。
     */
    private String readCache(String apiName, String cacheKey) {
        try {
            return cacheService.get(CACHE_PREFIX + apiName, cacheKey);
        } catch (Exception e) {
            log.warn("[Tool] {} 读缓存失败（降级为未命中）: {}", apiName, e.getMessage());
            return null;
        }
    }

    /**
     * 写缓存。仅在结果非空时写入；缓存异常时仅告警，不影响返回。
     */
    private void writeCache(String apiName, String cacheKey, String result) {
        if (result == null || result.isEmpty()) {
            return;
        }
        try {
            cacheService.put(CACHE_PREFIX + apiName, cacheKey, result);
        } catch (Exception e) {
            log.warn("[Tool] {} 写缓存失败（忽略）: {}", apiName, e.getMessage());
        }
    }

    /**
     * 将参数序列化为确定性字符串（按 key 排序），保证相同参数得到相同缓存键。
     *
     * @param params 请求参数
     * @return 形如 end_date=20241231&start_date=20240101&ts_code=000001.SZ 的串
     */
    private String canonicalParams(Map<String, Object> params) {
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

}
