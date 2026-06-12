package com.aihedgefund.controller;

import com.aihedgefund.agent.research.IndustryBottleneckResearchAgent;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IndustryAnalysisController 集成测试
 * 验证 POST /industry-analysis/run SSE 接口的事件流响应格式和内容
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMvcAuthConfig.class)
class IndustryAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IndustryBottleneckResearchAgent researchAgent;

    /** 跳过金币扣减逻辑，避免测试依赖钱包数据库状态 */
    @MockBean
    private WalletService walletService;

    /** 跳过分析记录持久化逻辑，避免测试依赖数据库状态 */
    @MockBean
    private AnalysisRunService analysisRunService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * query 为空时应返回 422 参数校验失败
     */
    @Test
    void run_blankQuery_returns422() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("query", ""));

        mockMvc.perform(post("/industry-analysis/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * 正常场景：返回 start / activity / complete 事件，complete 中包含 Markdown 报告
     */
    @SuppressWarnings("unchecked")
    @Test
    void run_validQuery_streamsCompleteEventWithReport() throws Exception {
        String report = "# Section One: Bottleneck Breakdown\n...report body...";
        when(researchAgent.analyze(anyString(), any(), any(), any(Consumer.class)))
                .thenAnswer(invocation -> {
                    Consumer<String> callback = invocation.getArgument(3);
                    callback.accept("开始产业瓶颈反向拆解分析…");
                    return report;
                });

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "query", "分析AI产业，找到PE在100以内的标的"
        ));

        MvcResult asyncResult = mockMvc.perform(post("/industry-analysis/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(request().asyncStarted())
                .andReturn();

        asyncResult.getAsyncResult(5_000);

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:start")))
                .andExpect(content().string(containsString("event:activity")))
                .andExpect(content().string(containsString("event:complete")))
                .andExpect(content().string(containsString("Bottleneck Breakdown")));
    }

    /**
     * Agent 抛出异常时，应推送 error 事件而非直接返回 5xx
     */
    @Test
    void run_agentThrowsException_streamsErrorEvent() throws Exception {
        when(researchAgent.analyze(anyString(), any(), any(), any()))
                .thenThrow(new RuntimeException("LLM service unavailable"));

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "query", "分析能源行业"
        ));

        MvcResult asyncResult = mockMvc.perform(post("/industry-analysis/run")
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
}
