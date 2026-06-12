package com.aihedgefund.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * 带缓存装饰的 ToolCallback 包装器
 *
 * <p>在 Spring AI 工具调用链中插入缓存层：
 *   1. 工具被 LLM 调用 → Spring AI 触发 call(toolInput)
 *   2. 本装饰器先查 ToolCallCacheService
 *   3. 命中 → 直接返回缓存结果，跳过外部 API 调用
 *   4. 未命中 → 委托原 ToolCallback 执行，结果写回缓存
 * </p>
 */
public class CachingToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(CachingToolCallback.class);

    private final ToolCallback delegate;
    private final ToolCallCacheService cacheService;

    /** 工具调用活动监听器，可为 null（无需推送进度时） */
    private final ToolActivityListener activityListener;

    public CachingToolCallback(ToolCallback delegate, ToolCallCacheService cacheService) {
        this(delegate, cacheService, null);
    }

    public CachingToolCallback(ToolCallback delegate, ToolCallCacheService cacheService,
            ToolActivityListener activityListener) {
        this.delegate = delegate;
        this.cacheService = cacheService;
        this.activityListener = activityListener;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        String toolName = delegate.getToolDefinition().name();

        // 1. 查缓存
        String cached = cacheService.get(toolName, toolInput);
        if (cached != null) {
            notifyActivity(toolName, true);
            notifyResult(toolName, cached);
            return cached;
        }

        // 2. 执行原调用（推送「正在调用工具」进度）
        notifyActivity(toolName, false);
        long start = System.currentTimeMillis();
        String result = delegate.call(toolInput);
        long elapsed = System.currentTimeMillis() - start;

        // 3. 写回缓存（结果非空才缓存，避免缓存错误/空响应）
        if (result != null && !result.isEmpty()) {
            cacheService.put(toolName, toolInput, result);
        }

        log.debug("工具执行完成: {} 耗时 {}ms, 结果长度 {}", toolName, elapsed,
                result != null ? result.length() : 0);
        notifyResult(toolName, result);
        return result;
    }

    /** 通知活动监听器（null 安全，异常静默） */
    private void notifyActivity(String toolName, boolean cacheHit) {
        if (activityListener == null) {
            return;
        }
        try {
            activityListener.onToolCall(toolName, cacheHit);
        } catch (Exception ignored) {
            // 进度推送失败不影响工具调用
        }
    }

    /** 通知活动监听器工具返回结果（null 安全，异常静默） */
    private void notifyResult(String toolName, String result) {
        if (activityListener == null) {
            return;
        }
        try {
            activityListener.onToolResult(toolName, result);
        } catch (Exception ignored) {
            // 进度推送失败不影响工具调用
        }
    }
}
