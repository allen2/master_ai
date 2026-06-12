package com.aihedgefund.data.search;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serper.dev 搜索服务商实现（通过 API 返回 Google 搜索结果）。
 *
 * <p>接口：POST {base-url}/search，密钥置于请求头 X-API-KEY。</p>
 */
@Component
public class SerperSearchProvider implements WebSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(SerperSearchProvider.class);

    private static final String PROVIDER_NAME = "serper";

    private final WebClient webClient;

    private final String apiKey;

    public SerperSearchProvider(
            @Value("${web-search.serper.base-url:https://google.serper.dev}") String baseUrl,
            @Value("${web-search.serper.api-key:}") String apiKey) {
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
            log.warn("[WebSearch] Serper API Key 未配置，跳过搜索");
            return Mono.just("[web-search] 未配置 Serper API Key（web-search.serper.api-key），无法搜索。");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("q", query);
        body.put("num", maxResults);

        return webClient.post()
                .uri("/search")
                .header("X-API-KEY", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(resp -> parse(query, resp))
                .doOnError(e -> log.error("[WebSearch] Serper 调用失败: query={}, error={}", query, e.getMessage()));
    }

    /**
     * 解析 Serper 响应（organic 列表）为格式化文本。
     */
    private String parse(String query, String responseJson) {
        JSONObject root = JSON.parseObject(responseJson);
        JSONArray organic = root.getJSONArray("organic");

        List<SearchResultFormatter.Item> items = new ArrayList<>();
        if (organic != null) {
            for (int i = 0; i < organic.size(); i++) {
                JSONObject item = organic.getJSONObject(i);
                items.add(new SearchResultFormatter.Item(
                        item.getString("title"),
                        item.getString("link"),
                        item.getString("snippet")));
            }
        }
        return SearchResultFormatter.format(PROVIDER_NAME, query, null, items);
    }
}
