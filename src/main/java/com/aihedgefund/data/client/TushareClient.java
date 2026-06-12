package com.aihedgefund.data.client;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class TushareClient {

    private final WebClient webClient;

    @Value("${tushare.token:}")
    private String token;

    public TushareClient() {
        this.webClient = WebClient.create("https://api.tushare.pro");
    }

    // 通用调用方法
    public Mono<JSONObject> api(String apiName, Map<String, Object> params) {
        Map<String, Object> body = new HashMap<>();
        body.put("api_name", apiName);
        body.put("token", token);
        body.put("params", params);

        return webClient.post()
                .uri("/")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(JSON::parseObject);
    }
}