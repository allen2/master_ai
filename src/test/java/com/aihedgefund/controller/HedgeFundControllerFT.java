package com.aihedgefund.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HedgeFundController 端对端功能测试（无 mock）
 * 启动完整 Spring Boot 应用，通过真实 HTTP 请求验证 SSE 事件流
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HedgeFundControllerFT {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 真实 HTTP 请求验证：SSE 流中必须包含 start 和 complete 事件，
     * 并打印完整响应内容到控制台（LLM 调用失败时降级为中性信号）
     */
    @Test
    void run_validRequest_streamsStartAndCompleteEvents() throws Exception {
        // 只选一个分析师以减少 LLM 调用次数
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "tickers", List.of("AAPL"),
                "start_date", "2024-01-01",
                "end_date", "2024-03-01",
                "model_name", "astron-code-latest",
                "model_provider", "OpenAI",
                "initial_cash", 100000.0,
                "selected_analysts", List.of("fundamentals_agent")
        ));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/hedge-fund/run"))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(120))
                .build();

        System.out.println("\n=== SSE Response Body ===");

        AtomicBoolean hasStart = new AtomicBoolean(false);
        AtomicBoolean hasComplete = new AtomicBoolean(false);
        StringBuilder fullBody = new StringBuilder();

        HttpResponse<Stream<String>> response = client.send(request,
                HttpResponse.BodyHandlers.ofLines());

        response.body().forEach(line -> {
            System.out.println(line);
            fullBody.append(line).append("\n");
            if (line.equals("event:start")) hasStart.set(true);
            if (line.equals("event:complete")) hasComplete.set(true);
        });

        System.out.println("=========================\n");

        assertEquals(200, response.statusCode(), "HTTP 状态码应为 200");
        assertTrue(hasStart.get(), "SSE 流中应包含 event:start");
        assertTrue(hasComplete.get(), "SSE 流中应包含 event:complete");
        assertTrue(fullBody.toString().contains("\"status\":\"started\""),
                "start 事件 data 应包含 status=started");
        assertTrue(fullBody.toString().contains("\"status\":\"complete\""),
                "complete 事件 data 应包含 status=complete");
    }
}
