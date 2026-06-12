package com.aihedgefund.data.search;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tavily 搜索服务商实现（专为 AI Agent 设计的搜索 API）。
 *
 * <p>接口：POST {base-url}/search，参数与密钥置于请求体 api_key 字段。</p>
 */
@Component
public class TavilySearchProvider implements WebSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(TavilySearchProvider.class);

    private static final String PROVIDER_NAME = "tavily";

    private final WebClient webClient;

    private final String apiKey;

    public TavilySearchProvider(
            @Value("${web-search.tavily.base-url:https://api.tavily.com}") String baseUrl,
            @Value("${web-search.tavily.api-key:}") String apiKey) {
        this.webClient = WebClient.create(baseUrl);
        this.apiKey = apiKey;
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public Mono<String> search(String query, int maxResults) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[WebSearch] Tavily API Key 未配置，跳过搜索");
            return Mono.just("[web-search] 未配置 Tavily API Key（web-search.tavily.api-key），无法搜索。");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("api_key", apiKey);
        body.put("query", query);
        body.put("max_results", maxResults);
        body.put("search_depth", "basic");
        body.put("include_answer", true);

        return webClient.post()
                .uri("/search")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(resp -> parse(query, resp))
                .doOnError(e -> log.error("[WebSearch] Tavily 调用失败: query={}, error={}", query, e.getMessage()));
    }

    /**
     * 解析 Tavily 响应为格式化文本。
     */
    private String parse(String query, String responseJson) {
        JSONObject root = JSON.parseObject(responseJson);
        String answer = root.getString("answer");
        JSONArray results = root.getJSONArray("results");

        List<SearchResultFormatter.Item> items = new ArrayList<>();
        if (results != null) {
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                items.add(new SearchResultFormatter.Item(
                        item.getString("title"),
                        item.getString("url"),
                        item.getString("content")));
            }
        }
        return SearchResultFormatter.format(PROVIDER_NAME, query, answer, items);
    }
}
