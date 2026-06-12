package com.aihedgefund.llm;

import com.aihedgefund.cache.ToolCallCacheService;
import com.aihedgefund.data.search.WebSearchProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网络搜索工具集（Spring AI @Tool）。
 *
 * <p>允许 LLM Agent 从互联网检索资料（新闻、研报、公司动态等），同时作为 MCP Server 工具暴露。
 * 底层支持多家搜索服务商（Tavily / Serper / Bocha），由配置 {@code web-search.provider} 选择激活其一。</p>
 *
 * <p>内置结果缓存（复用 {@link ToolCallCacheService}）：相同查询直接返回缓存，未命中才请求 API，
 * 请求成功（结果非空）后写回缓存。</p>
 */
@Component
public class WebSearchTools {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTools.class);

    /** 缓存命名空间前缀 */
    private static final String CACHE_PREFIX = "websearch.";

    /** 默认结果条数 */
    private static final int DEFAULT_MAX_RESULTS = 5;

    /** 结果条数上限，防止请求过大 */
    private static final int MAX_RESULTS_LIMIT = 10;

    private final WebSearchProvider activeProvider;

    private final ToolCallCacheService cacheService;

    public WebSearchTools(List<WebSearchProvider> providers,
            ToolCallCacheService cacheService,
            @Value("${web-search.provider:tavily}") String providerName) {
        this.cacheService = cacheService;
        this.activeProvider = resolveProvider(providers, providerName);
        log.info("网络搜索工具初始化: 激活服务商={}", activeProvider.name());
    }

    /**
     * 按配置名称选择激活的搜索服务商。
     *
     * @param providers    所有已注册的服务商
     * @param providerName 配置的服务商名称
     * @return 激活的服务商
     * @throws IllegalStateException 找不到对应服务商时抛出
     */
    private WebSearchProvider resolveProvider(List<WebSearchProvider> providers, String providerName) {
        return providers.stream()
                .filter(p -> p.name().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "未找到名为 [" + providerName + "] 的搜索服务商，可选: "
                                + providers.stream().map(WebSearchProvider::name).collect(Collectors.joining(", "))));
    }

    // ==========================
    // 工具：网络搜索
    // ==========================
    @Tool(description = "从互联网搜索实时资料，返回相关网页的标题、链接和摘要。适合查询公司最新新闻、行业动态、政策、研报观点等金融数据接口中没有的信息。")
    public String webSearch(
            @ToolParam(description = "搜索关键词或自然语言问题，如：贵州茅台 2024 年最新业绩 或 新能源汽车行业政策") String query,
            @ToolParam(required = false, description = "返回结果条数，默认 5，最大 10") Integer maxResults) {
        int count = normalizeMaxResults(maxResults);
        log.info("[Tool] webSearch: provider={}, query={}, maxResults={}", activeProvider.name(), query, count);

        Map<String, Object> params = Map.of(
                "provider", activeProvider.name(),
                "query", query,
                "max_results", count);
        String cacheKey = canonicalParams(params);

        String cached = readCache(cacheKey);
        if (cached != null) {
            log.info("[Tool] webSearch 命中缓存, query={}", query);
            return cached;
        }

        String result = activeProvider.search(query, count)
                .doOnError(e -> log.error("[Tool] webSearch 失败: query={}, error={}", query, e.getMessage()))
                .block();

        writeCache(cacheKey, result);
        log.debug("[Tool] webSearch 返回, query={}, length={}", query, result != null ? result.length() : 0);
        return result;
    }

    // ==========================
    // 缓存与参数处理
    // ==========================

    /**
     * 规范化结果条数：空或非正用默认值，超过上限取上限。
     */
    private int normalizeMaxResults(Integer maxResults) {
        if (maxResults == null || maxResults <= 0) {
            return DEFAULT_MAX_RESULTS;
        }
        return Math.min(maxResults, MAX_RESULTS_LIMIT);
    }

    /**
     * 读缓存。缓存异常时降级为未命中，绝不影响搜索。
     */
    private String readCache(String cacheKey) {
        try {
            return cacheService.get(CACHE_PREFIX + activeProvider.name(), cacheKey);
        } catch (Exception e) {
            log.warn("[Tool] webSearch 读缓存失败（降级为未命中）: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 写缓存。仅在结果非空时写入；缓存异常时仅告警，不影响返回。
     */
    private void writeCache(String cacheKey, String result) {
        if (result == null || result.isEmpty()) {
            return;
        }
        try {
            cacheService.put(CACHE_PREFIX + activeProvider.name(), cacheKey, result);
        } catch (Exception e) {
            log.warn("[Tool] webSearch 写缓存失败（忽略）: {}", e.getMessage());
        }
    }

    /**
     * 将参数序列化为确定性字符串（按 key 排序），保证相同查询得到相同缓存键。
     */
    private String canonicalParams(Map<String, Object> params) {
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }
}
