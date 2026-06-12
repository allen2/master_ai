package com.aihedgefund.controller;

import com.aihedgefund.agent.research.IndustryBottleneckResearchAgent;
import com.aihedgefund.auth.AuthUser;
import com.aihedgefund.auth.UserContext;
import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.model.req.IndustryAnalysisReq;
import com.aihedgefund.service.AnalysisRunService;
import com.aihedgefund.service.WalletService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 产业瓶颈分析接口：用户输入一句话产业/标的描述，使用「行业瓶颈反向拆解选股法」
 * 五步框架进行研究分析，以 SSE 流式返回分析进度和最终 Markdown 报告。
 */
@RestController
@RequestMapping("/industry-analysis")
public class IndustryAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(IndustryAnalysisController.class);

    private final IndustryBottleneckResearchAgent researchAgent;
    private final WalletService walletService;
    private final AnalysisRunService analysisRunService;

    /** SSE 请求最长等待时长（毫秒），默认 3000 秒 = 50 分钟，可通过 application.yml 覆盖 */
    @Value("${hedge-fund.sse-timeout-ms:3000000}")
    private long sseTimeoutMs;

    public IndustryAnalysisController(IndustryBottleneckResearchAgent researchAgent,
            WalletService walletService, AnalysisRunService analysisRunService) {
        this.researchAgent = researchAgent;
        this.walletService = walletService;
        this.analysisRunService = analysisRunService;
    }

    @PostMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter run(@RequestBody @Valid IndustryAnalysisReq req) {
        AuthUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(401, "未登录");
        }

        // 扣减金币（余额不足时抛出 BizException 402，直接返回 HTTP 400）
        walletService.deductOneForAnalysis(currentUser.getUserId());

        // 创建分析记录（RUNNING），完成或失败后回填结果，供「分析记录」页面查看历史
        Long runId = analysisRunService.createRunningIndustryAnalysis(
                currentUser.getUserId(), req.getQuery(), req.getModelName());

        log.info("收到产业瓶颈分析请求, query={}, userId={}, runId={}, SSE 超时={}ms",
                req.getQuery(), currentUser.getUserId(), runId, sseTimeoutMs);
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Map<String, Object> startData = new HashMap<>();
                startData.put("status", "started");
                startData.put("query", req.getQuery());
                emitter.send(SseEmitter.event().name("start").data(startData));

                String report = researchAgent.analyze(req.getQuery(), req.getModelName(), req.getModelProvider(),
                        message -> sendActivity(emitter, message));

                Map<String, Object> completeData = new HashMap<>();
                completeData.put("report", report);
                completeData.put("status", "completed");
                emitter.send(SseEmitter.event().name("complete").data(completeData));
                emitter.complete();

                analysisRunService.markCompleteIndustryAnalysis(runId, report);

            } catch (IOException e) {
                log.error("SSE 发送失败: {}", e.getMessage());
                analysisRunService.markError(runId, e.getMessage());
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("产业瓶颈分析失败: {}", e.getMessage(), e);
                analysisRunService.markError(runId, e.getMessage());
                try {
                    Map<String, Object> errData = new HashMap<>();
                    errData.put("message", e.getMessage());
                    emitter.send(SseEmitter.event().name("error").data(errData));
                    emitter.complete();
                } catch (IOException | IllegalStateException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    /** 推送分析活动进度事件，发送失败时静默忽略（不中断分析流程） */
    private void sendActivity(SseEmitter emitter, String message) {
        try {
            Map<String, Object> activityData = new HashMap<>();
            activityData.put("message", message);
            emitter.send(SseEmitter.event().name("activity").data(activityData));
        } catch (IOException | IllegalStateException e) {
            log.debug("活动进度推送失败（忽略）: {}", e.getMessage());
        }
    }
}
