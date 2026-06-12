package com.aihedgefund.data.search;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 博查（Bocha）搜索服务商实现（国内可直达的中文搜索 API）。
 *
 * <p>接口：POST {base-url}/v1/web-search，密钥置于请求头 Authorization: Bearer。</p>
 */
@Component
public class BochaSearchProvider implements WebSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(BochaSearchProvider.class);

    private static final String PROVIDER_NAME = "bocha";

    private final WebClient webClient;

    private final String apiKey;

    public BochaSearchProvider(
            @Value("${web-search.bocha.base-url:https://api.bochaai.com}") String baseUrl,
            @Value("${web-search.bocha.api-key:}") String apiKey) {
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
            log.warn("[WebSearch] Bocha API Key 未配置，跳过搜索");
            return Mono.just("[web-search] 未配置 Bocha API Key（web-search.bocha.api-key），无法搜索。");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("count", maxResults);
        body.put("summary", true);

        return webClient.post()
                .uri("/v1/web-search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(resp -> parse(query, resp))
                .doOnError(e -> log.error("[WebSearch] Bocha 调用失败: query={}, error={}", query, e.getMessage()));
    }

    /**
     * 解析 Bocha 响应（data.webPages.value 列表）为格式化文本。
     */
    private String parse(String query, String responseJson) {
        JSONObject root = JSON.parseObject(responseJson);
        List<SearchResultFormatter.Item> items = new ArrayList<>();

        JSONObject data = root.getJSONObject("data");
        if (data != null) {
            JSONObject webPages = data.getJSONObject("webPages");
            if (webPages != null) {
                JSONArray value = webPages.getJSONArray("value");
                if (value != null) {
                    for (int i = 0; i < value.size(); i++) {
                        JSONObject item = value.getJSONObject(i);
                        // 优先使用 summary（更完整），无则退回 snippet
                        String snippet = item.getString("summary");
                        if (snippet == null || snippet.isBlank()) {
                            snippet = item.getString("snippet");
                        }
                        items.add(new SearchResultFormatter.Item(
                                item.getString("name"),
                                item.getString("url"),
                                snippet));
                    }
                }
            }
        }
        return SearchResultFormatter.format(PROVIDER_NAME, query, null, items);
    }
}
