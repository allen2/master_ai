package com.aihedgefund.agent.research;

import com.aihedgefund.cache.CachingToolCallback;
import com.aihedgefund.cache.ToolActivityListener;
import com.aihedgefund.cache.ToolCallCacheService;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.prompt.PromptLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 热门行业逆向对立面价值标的挖掘 Agent。
 *
 * <p>根据用户输入的一句话热门行业/分析需求，使用「通用热门行业逆向对立面价值标的挖掘」五步框架，
 * 调用大模型 + 网络搜索等工具完成研究，输出自由格式的 Markdown 分析报告。</p>
 *
 * <p>与 {@code BaseLlmAgent} 不同：本 Agent 输出自由格式 Markdown 报告而非
 * {@code AgentSignal} JSON，因此不复用其结构化输出解析逻辑，但复用工具收集/缓存/心跳模式。</p>
 */
@Component
public class ContrarianSectorResearchAgent {

    private static final Logger log = LoggerFactory.getLogger(ContrarianSectorResearchAgent.class);

    /** 系统提示词文件名（对应 prompts/contrarian_sector_research.md） */
    private static final String PROMPT_NAME = "contrarian_sector_research";

    /** 心跳调度线程池：大模型推理耗时较长时定期推送「仍在分析中」的活动消息 */
    private static final ScheduledExecutorService HEARTBEAT_SCHEDULER = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "contrarian-research-heartbeat");
        t.setDaemon(true);
        return t;
    });

    /** 心跳推送间隔（秒） */
    private static final int HEARTBEAT_INTERVAL_SECONDS = 10;

    /** 工具结果预览的最大字符数 */
    private static final int RESULT_PREVIEW_MAX_LENGTH = 120;

    private final LlmClientFactory llmFactory;
    private final String systemPrompt;

    /** 注入所有 ToolCallbackProvider：网络搜索 + 财务数据等工具 */
    @Autowired(required = false)
    private List<ToolCallbackProvider> toolCallbackProviders;

    /** 工具调用缓存服务：相同参数的工具调用直接返回缓存结果 */
    @Autowired
    private ToolCallCacheService cacheService;

    public ContrarianSectorResearchAgent(LlmClientFactory llmFactory, PromptLoader promptLoader) {
        this.llmFactory = llmFactory;
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * 对一句话热门行业/分析需求执行五步逆向对立面价值标的挖掘分析。
     *
     * @param query         用户输入的一句话热门行业/对立面分析需求及筛选条件
     * @param modelName     大模型名称
     * @param modelProvider 大模型提供方
     * @param activityCallback 分析过程中的活动进度回调（用于 SSE 推送），可为 null
     * @return Markdown 格式的五段式分析报告
     */
    public String analyze(String query, String modelName, String modelProvider, Consumer<String> activityCallback) {
        String resolvedModelName = modelName != null ? modelName : "astron-code-latest";
        String resolvedModelProvider = modelProvider != null ? modelProvider : "OpenAI";
        ChatModel chatModel = llmFactory.create(resolvedModelName, resolvedModelProvider, 3);

        ToolCallback[] allCallbacks = collectToolCallbacks(activityCallback);
        String toolsDescription = buildToolsDescription(allCallbacks);

        log.debug("[contrarian-sector-research] 可用工具 {} 个: {}", allCallbacks.length,
                Arrays.stream(allCallbacks)
                        .map(c -> c.getToolDefinition().name())
                        .collect(Collectors.joining(", ")));

        ToolCallingChatOptions toolOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(allCallbacks)
                .internalToolExecutionEnabled(true)
                .build();

        log.info("[contrarian-sector-research] 开始分析, query={}, model={}, 工具数={}",
                query, resolvedModelName, allCallbacks.length);

        pushActivity(activityCallback, "开始热门行业逆向对立面分析…");

        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(buildUserPrompt(query, toolsDescription))
                ),
                toolOptions
        );

        pushActivity(activityCallback, "调用大模型推理中…");
        ChatResponse response = callWithHeartbeat(prompt, chatModel, activityCallback);
        String report = response.getResult().getOutput().getText();

        log.debug("[contrarian-sector-research] LLM 最终响应: {}", report);
        return report;
    }

    /**
     * 调用大模型并在等待期间定期推送心跳活动，避免前端在大模型响应较慢时长时间无感知。
     */
    private ChatResponse callWithHeartbeat(Prompt prompt, ChatModel chatModel, Consumer<String> activityCallback) {
        AtomicInteger tick = new AtomicInteger(0);
        ScheduledFuture<?> heartbeat = HEARTBEAT_SCHEDULER.scheduleAtFixedRate(() -> {
            int seconds = tick.incrementAndGet() * HEARTBEAT_INTERVAL_SECONDS;
            pushActivity(activityCallback, MessageFormat.format("大模型正在埋头苦算…（已等待 {0} 秒）", seconds));
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        try {
            return chatModel.call(prompt);
        } finally {
            heartbeat.cancel(false);
        }
    }

    /**
     * 收集所有注册的 ToolCallback（本地工具 + MCP 外部工具），并包裹缓存层与活动监听器。
     */
    private ToolCallback[] collectToolCallbacks(Consumer<String> activityCallback) {
        List<ToolCallback> list = new ArrayList<>();
        ToolActivityListener activity = new ToolActivityListener() {
            @Override
            public void onToolCall(String toolName, boolean cacheHit) {
                pushActivity(activityCallback, cacheHit ? ("命中缓存：" + toolName) : ("调用工具 " + toolName + " 获取数据…"));
            }

            @Override
            public void onToolResult(String toolName, String result) {
                String preview = truncateForPreview(result);
                if (preview != null) {
                    pushActivity(activityCallback, "工具 " + toolName + " 返回：" + preview);
                }
            }
        };
        if (toolCallbackProviders != null) {
            for (ToolCallbackProvider provider : toolCallbackProviders) {
                ToolCallback[] callbacks = provider.getToolCallbacks();
                if (callbacks != null) {
                    for (ToolCallback cb : callbacks) {
                        list.add(new CachingToolCallback(cb, cacheService, activity));
                    }
                }
            }
        }
        return list.toArray(new ToolCallback[0]);
    }

    /** 截取工具返回结果的前一小段，去除换行后用于前端进度展示，避免长 JSON/数组刷屏。 */
    private String truncateForPreview(String result) {
        if (result == null || result.isBlank()) {
            return null;
        }
        String oneLine = result.replaceAll("\\s+", " ").trim();
        if (oneLine.length() <= RESULT_PREVIEW_MAX_LENGTH) {
            return oneLine;
        }
        return oneLine.substring(0, RESULT_PREVIEW_MAX_LENGTH) + "…";
    }

    /** 生成工具描述文本，嵌入 userPrompt 让 LLM 知道可以调用哪些工具 */
    private String buildToolsDescription(ToolCallback[] callbacks) {
        if (callbacks.length == 0) {
            return "  (暂无可用数据工具)";
        }
        return Arrays.stream(callbacks)
                .map(cb -> String.format("  - %s: %s",
                        cb.getToolDefinition().name(),
                        cb.getToolDefinition().description()))
                .collect(Collectors.joining("\n"));
    }

    /** 构造用户提示词，传递用户的一句话分析需求和可用工具列表 */
    private String buildUserPrompt(String query, String toolsDescription) {
        return String.format(
                "请按照系统提示词中的「通用热门行业逆向对立面价值标的挖掘」五步框架，分析以下需求：\n\n%s\n\n" +
                "可用工具列表：\n%s\n\n" +
                "请充分调用工具获取真实、最新的公开信息后，按一→五的顺序输出 Markdown 格式的完整分析报告。",
                query, toolsDescription
        );
    }

    /** 活动回调空安全通知 */
    private void pushActivity(Consumer<String> activityCallback, String message) {
        if (activityCallback == null) {
            return;
        }
        try {
            activityCallback.accept(message);
        } catch (Exception ignored) {
            // 进度推送失败不影响分析流程
        }
    }
}
