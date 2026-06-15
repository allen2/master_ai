package com.aihedgefund.controller;

import com.aihedgefund.auth.AuthUser;
import com.aihedgefund.auth.UserContext;
import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.model.req.HedgeFundRunReq;
import com.aihedgefund.orchestrator.AgentProfile;
import com.aihedgefund.orchestrator.AgentProfileRegistry;
import com.aihedgefund.orchestrator.HedgeFundOrchestrator;
import com.aihedgefund.orchestrator.WorkflowResult;
import com.aihedgefund.service.AnalysisRunService;
import com.aihedgefund.service.WalletService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 对冲基金运行接口（兼容 Python /hedge-fund/run SSE 路由）
 * Phase 2：接入真实规则型 Agent 编排，推送进度事件
 */
@RestController
@RequestMapping("/hedge-fund")
public class HedgeFundController {

    private static final Logger log = LoggerFactory.getLogger(HedgeFundController.class);

    private final HedgeFundOrchestrator orchestrator;
    private final AgentProfileRegistry profileRegistry;
    private final WalletService walletService;
    private final AnalysisRunService analysisRunService;

    /** SSE 请求最长等待时长（毫秒），默认 3000 秒 = 50 分钟，可通过 application.yml 覆盖 */
    @Value("${hedge-fund.sse-timeout-ms:3000000}")
    private long sseTimeoutMs;

    public HedgeFundController(HedgeFundOrchestrator orchestrator,
            AgentProfileRegistry profileRegistry, WalletService walletService,
            AnalysisRunService analysisRunService) {
        this.orchestrator = orchestrator;
        this.profileRegistry = profileRegistry;
        this.walletService = walletService;
        this.analysisRunService = analysisRunService;
    }

    /**
     * 获取所有分析师的五位一体结构化画像
     * 前端展示：分析师卡片 / 悬停提示
     */
    @GetMapping("/analysts")
    public List<AgentProfile> listAnalysts() {
        return profileRegistry.listAll();
    }

    @PostMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter run(@RequestBody @Valid HedgeFundRunReq req) {
        // 扣减金币（余额不足时抛出 BizException 402，直接返回 HTTP 400）
        AuthUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(401, "未登录");
        }
        walletService.deductOneForAnalysis(currentUser.getUserId());

        // 创建分析记录（RUNNING），完成或失败后回填结果，供「分析记录」页面查看历史
        Long runId = analysisRunService.createRunning(currentUser.getUserId(), req);

        log.info("收到对冲基金运行请求, tickers={}, userId={}, runId={}, SSE 超时={}ms",
                req.getTickers(), currentUser.getUserId(), runId, sseTimeoutMs);
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                // start 事件（包含待分析的分析师列表，前端用于立即初始化 UI）
                Map<String, Object> startData = new HashMap<>();
                startData.put("status", "started");
                startData.put("tickers", req.getTickers());
                List<String> selectedIds = req.getSelectedAnalysts();
                List<AgentProfile> agentProfiles;
                if (selectedIds == null || selectedIds.isEmpty()) {
                    agentProfiles = profileRegistry.listAll();
                } else {
                    agentProfiles = selectedIds.stream()
                            .map(id -> profileRegistry.getByAgentId(id).orElse(null))
                            .filter(p -> p != null)
                            .toList();
                }
                // 传递 {id, name} 映射，让前端初始化时直接显示中文名
                List<Map<String, String>> agentsList = agentProfiles.stream()
                        .map(p -> { Map<String, String> m = new LinkedHashMap<>(); m.put("id", p.getAgentId()); m.put("name", p.getDisplayName()); return m; })
                        .toList();
                startData.put("agents", agentsList);
                emitter.send(SseEmitter.event().name("start").data(startData));

                // 执行工作流（编排器内部推送 progress 事件）
                WorkflowResult result = orchestrator.run(req, emitter);

                // complete 事件（对应 Python CompleteEvent）
                Map<String, Object> completeData = new HashMap<>();
                completeData.put("analyst_signals", result.getAnalystSignals());
                completeData.put("decisions", result.getDecisions());
                completeData.put("status", result.getStatus());
                emitter.send(SseEmitter.event().name("complete").data(completeData));
                emitter.complete();

                analysisRunService.markComplete(runId, result);

            } catch (IOException e) {
                log.error("SSE 发送失败: {}", e.getMessage());
                analysisRunService.markError(runId, e.getMessage());
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("工作流执行失败: {}", e.getMessage(), e);
                analysisRunService.markError(runId, e.getMessage());
                try {
                    Map<String, Object> errData = new HashMap<>();
                    errData.put("message", e.getMessage());
                    emitter.send(SseEmitter.event().name("error").data(errData));
                    emitter.complete();
                } catch (IOException | IllegalStateException ex) {
                    // emitter 可能已超时或断开，直接 completeWithError
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }
}
