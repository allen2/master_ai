package com.aihedgefund.controller;

import com.aihedgefund.model.req.HedgeFundRunReq;
import com.aihedgefund.orchestrator.AgentSignal;
import com.aihedgefund.orchestrator.HedgeFundOrchestrator;
import com.aihedgefund.orchestrator.WorkflowResult;
import com.aihedgefund.service.AnalysisRunService;
import com.aihedgefund.service.WalletService;
import com.aihedgefund.support.MockMvcAuthConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HedgeFundController 集成测试
 * 验证 POST /hedge-fund/run SSE 接口的事件流响应格式和内容
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMvcAuthConfig.class)
class HedgeFundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HedgeFundOrchestrator orchestrator;

    /** 跳过金币扣减逻辑，避免测试依赖钱包数据库状态 */
    @MockBean
    private WalletService walletService;

    /** 跳过分析记录持久化逻辑，避免测试依赖数据库状态 */
    @MockBean
    private AnalysisRunService analysisRunService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * tickers 为空时应返回 422 参数校验失败
     */
    @Test
    void run_missingTickers_returns422() throws Exception {
        mockMvc.perform(post("/hedge-fund/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * tickers 为空列表时应返回 422 参数校验失败
     */
    @Test
    void run_emptyTickers_returns422() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("tickers", List.of()));
        mockMvc.perform(post("/hedge-fund/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * 编排器抛出异常时，应推送 error 事件而非直接返回 5xx
     */
    @Test
    void run_orchestratorThrowsException_streamsErrorEvent() throws Exception {
        when(orchestrator.run(any(HedgeFundRunReq.class), any(SseEmitter.class)))
                .thenThrow(new RuntimeException("LLM service unavailable"));

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "tickers", List.of("AAPL"),
                "model_name", "gpt-4o",
                "model_provider", "OpenAI"
        ));

        MvcResult asyncResult = mockMvc.perform(post("/hedge-fund/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(request().asyncStarted())
                .andReturn();

        asyncResult.getAsyncResult(5_000);

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:error")))
                .andExpect(content().string(containsString("LLM service unavailable")));
    }

    /**
     * 多股票多分析师场景：complete 事件中应包含所有 ticker 和分析师的信号
     */
    @Test
    void run_multipleTickersAndAnalysts_allSignalsPresentInCompleteEvent() throws Exception {
        Map<String, Map<String, AgentSignal>> signals = Map.of(
                "warren_buffett_agent", Map.of(
                        "AAPL", AgentSignal.bullish(80, "Wonderful business"),
                        "MSFT", AgentSignal.bullish(85, "Durable competitive advantage")
                ),
                "fundamentals_agent", Map.of(
                        "AAPL", AgentSignal.neutral(50, "Fair valuation"),
                        "MSFT", AgentSignal.bullish(70, "Strong balance sheet")
                )
        );
        WorkflowResult mockResult = new WorkflowResult(signals, Map.of(), "complete");
        when(orchestrator.run(any(HedgeFundRunReq.class), any(SseEmitter.class))).thenReturn(mockResult);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "tickers", List.of("AAPL", "MSFT"),
                "model_name", "gpt-4o",
                "model_provider", "OpenAI"
        ));

        MvcResult asyncResult = mockMvc.perform(post("/hedge-fund/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(request().asyncStarted())
                .andReturn();

        asyncResult.getAsyncResult(5_000);

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("warren_buffett_agent")))
                .andExpect(content().string(containsString("fundamentals_agent")))
                .andExpect(content().string(containsString("AAPL")))
                .andExpect(content().string(containsString("MSFT")));
    }

    /**
     * 指定 selected_analysts 时，start 事件应正常推送（编排器过滤逻辑由编排器测试覆盖）
     */
    @Test
    void run_withSelectedAnalysts_returnsOk() throws Exception {
        WorkflowResult mockResult = new WorkflowResult(Map.of(), Map.of(), "complete");
        when(orchestrator.run(any(HedgeFundRunReq.class), any(SseEmitter.class))).thenReturn(mockResult);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "tickers", List.of("TSLA"),
                "model_name", "gpt-4o",
                "model_provider", "OpenAI",
                "selected_analysts", List.of("warren_buffett_agent", "fundamentals_agent")
        ));

        MvcResult asyncResult = mockMvc.perform(post("/hedge-fund/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(request().asyncStarted())
                .andReturn();

        asyncResult.getAsyncResult(5_000);

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:start")))
                .andExpect(content().string(containsString("event:complete")));
    }
}
