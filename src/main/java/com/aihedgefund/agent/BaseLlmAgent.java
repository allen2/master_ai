package com.aihedgefund.agent;

import com.aihedgefund.cache.CachingToolCallback;
import com.aihedgefund.cache.ToolActivityListener;
import com.aihedgefund.cache.ToolCallCacheService;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.orchestrator.AgentSignal;
import com.aihedgefund.orchestrator.AgentState;
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

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * LLM-powered Agent 基类（Spring AI Tool Calling 模式）
 *
 * 工具调用流程：
 *   1. 把工具定义（ToolCallback[]）随 Prompt 发给模型
 *   2. 模型返回工具调用请求
 *   3. Spring AI 的 internalToolExecutionEnabled=true 自动执行工具，把结果回传模型
 *   4. 循环直到模型返回最终文本响应
 *
 * 前提：LlmClientFactory 的 HTTP 拦截器不能消耗响应体（readAllBytes），
 * 否则 Spring AI 无法读取响应来感知工具调用，工具循环无法启动。
 */
public abstract class BaseLlmAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(BaseLlmAgent.class);

    /** LLM 调用心跳调度线程池：用于在大模型推理耗时较长时，定期向前端推送「仍在分析中」的活动消息 */
    private static final ScheduledExecutorService HEARTBEAT_SCHEDULER = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "llm-heartbeat");
        t.setDaemon(true);
        return t;
    });

    /** 心跳推送间隔（秒） */
    private static final int HEARTBEAT_INTERVAL_SECONDS = 10;

    /**
     * 心跳趣味文案：大模型推理耗时较长时按顺序循环展示，避免「大模型推理中…」单调重复。
     * {0}=ticker，{1}=已等待秒数
     */
    private static final String[] HEARTBEAT_MESSAGES = {
            "{0}：大模型正在埋头苦算…（已等待 {1} 秒）",
            "{0}：正在翻阅财报和历史数据…（已等待 {1} 秒）",
            "{0}：多空观点交锋中，让子弹再飞一会…（已等待 {1} 秒）",
            "{0}：AI 分析师陷入沉思…（已等待 {1} 秒）",
            "{0}：正在和巴菲特、芒格连线讨论…（已等待 {1} 秒）",
            "{0}：推理引擎全速运转中…（已等待 {1} 秒）",
            "{0}：正在权衡安全边际与成长性…（已等待 {1} 秒）",
            "{0}：再等等，结论马上出炉…（已等待 {1} 秒）"
    };

    protected final LlmClientFactory llmFactory;
    protected final StructuredOutputHelper outputHelper;

    /** 注入所有 ToolCallbackProvider：本地 FinancialDataTools + 外部 MCP 服务器 */
    @Autowired(required = false)
    private List<ToolCallbackProvider> toolCallbackProviders;

    /** 工具调用缓存服务：对相同参数的工具调用直接返回缓存结果 */
    @Autowired
    private ToolCallCacheService cacheService;

    protected BaseLlmAgent(LlmClientFactory llmFactory, StructuredOutputHelper outputHelper) {
        this.llmFactory = llmFactory;
        this.outputHelper = outputHelper;
    }

    @Override
    public void analyze(AgentState state, List<String> tickers) {
        String modelName = state.getModelName() != null ? state.getModelName() : "astron-code-latest";
        String modelProvider = state.getModelProvider() != null ? state.getModelProvider() : "OpenAI";
        ChatModel chatModel = llmFactory.create(modelName, modelProvider, 3);

        // 收集所有注册的 ToolCallback（本地 + MCP），并绑定活动进度推送
        ToolCallback[] allCallbacks = collectToolCallbacks(state);

        // 动态生成工具描述，嵌入 userPrompt 辅助模型决策调用哪些工具
        String toolsDescription = buildToolsDescription(allCallbacks);

        log.debug("[{}] 可用工具 {} 个: {}",
                getAgentId(), allCallbacks.length,
                Arrays.stream(allCallbacks)
                        .map(c -> c.getToolDefinition().name())
                        .collect(Collectors.joining(", ")));

        // 构建工具选项：
        //   toolCallbacks    → 向模型声明工具定义（函数签名 + 描述），Spring AI 自动序列化为 OpenAI tools 字段
        //   internalToolExecutionEnabled=true → Spring AI 负责完整工具循环：
        //       模型请求工具 → Spring AI 执行 ToolCallback.call() → 结果回传模型 → 直到最终文本响应
        //
        // 注意：此选项依赖 HTTP 响应体未被拦截器消耗（见 LlmClientFactory 拦截器注释）
        ToolCallingChatOptions toolOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(allCallbacks)
                .internalToolExecutionEnabled(true)
                .build();

        for (String ticker : tickers) {
            log.info("[{}] 工具调用分析, ticker={}, model={}, 工具数={}",
                    getAgentId(), ticker, modelName, allCallbacks.length);
            try {
                String endDate = state.getEndDate() != null ? state.getEndDate()
                        : LocalDate.now().toString();

                state.publishActivity(getAgentId(), ticker + "：开始分析，准备数据…");

                Prompt prompt = new Prompt(
                        List.of(
                                new SystemMessage(getSystemPrompt()),
                                new UserMessage(buildUserPrompt(ticker, endDate, toolsDescription))
                        ),
                        toolOptions
                );

                //   chatModel.call() 配合 internalToolExecutionEnabled=true：
                //   Spring AI 在内部处理完整的工具调用循环后才返回最终 ChatResponse
                state.publishActivity(getAgentId(), ticker + "：调用大模型推理中…");
                ChatResponse response = callWithHeartbeat(state, ticker, prompt, chatModel);
                String rawResponse = response.getResult().getOutput().getText();

                log.debug("[{}] ticker={} LLM 最终响应: {}", getAgentId(), ticker, rawResponse);

                AgentSignal signal = outputHelper.parse(rawResponse, AgentSignal.class,
                        () -> AgentSignal.neutral(30, "LLM 响应解析失败，使用默认中性信号"));
                state.putSignal(getAgentId(), ticker, signal);
                log.info("[{}] ticker={} → {}", getAgentId(), ticker, signal);

            } catch (Exception e) {
                log.error("[{}] 分析 {} 失败: {}", getAgentId(), ticker, e.getMessage(), e);
                state.putSignal(getAgentId(), ticker,
                        AgentSignal.neutral(20, "分析失败: " + e.getMessage()));
            }
        }
    }

    /**
     * 调用大模型并在等待期间定期推送心跳活动，避免前端在大模型响应较慢时长时间无感知。
     *
     * @param state     共享状态（用于推送活动进度）
     * @param ticker    当前分析的股票代码
     * @param prompt    完整的 Prompt（含工具选项）
     * @param chatModel 大模型客户端
     * @return 大模型最终响应
     */
    private ChatResponse callWithHeartbeat(AgentState state, String ticker, Prompt prompt, ChatModel chatModel) {
        AtomicInteger tick = new AtomicInteger(0);
        ScheduledFuture<?> heartbeat = HEARTBEAT_SCHEDULER.scheduleAtFixedRate(() -> {
            int count = tick.incrementAndGet();
            int seconds = count * HEARTBEAT_INTERVAL_SECONDS;
            String template = HEARTBEAT_MESSAGES[(count - 1) % HEARTBEAT_MESSAGES.length];
            state.publishActivity(getAgentId(), MessageFormat.format(template, ticker, seconds));
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        try {
            return chatModel.call(prompt);
        } finally {
            heartbeat.cancel(false);
        }
    }

    /**
     * 收集所有注册的 ToolCallback（本地工具 + MCP 外部工具），并包裹缓存层。
     * 同时绑定活动监听器：工具被调用时通过 state 推送「调用工具/命中缓存」进度。
     *
     * @param state 共享状态（承载活动监听器）
     */
    private ToolCallback[] collectToolCallbacks(AgentState state) {
        List<ToolCallback> list = new ArrayList<>();
        // 工具调用活动 → 转为 state 的活动进度（前端可即时展示）
        ToolActivityListener activity = new ToolActivityListener() {
            @Override
            public void onToolCall(String toolName, boolean cacheHit) {
                state.publishActivity(getAgentId(),
                        cacheHit ? ("命中缓存：" + toolName) : ("调用工具 " + toolName + " 获取数据…"));
            }

            @Override
            public void onToolResult(String toolName, String result) {
                String preview = truncateForPreview(result);
                if (preview != null) {
                    state.publishActivity(getAgentId(), "工具 " + toolName + " 返回：" + preview);
                }
            }
        };
        if (toolCallbackProviders != null) {
            for (ToolCallbackProvider provider : toolCallbackProviders) {
                ToolCallback[] callbacks = provider.getToolCallbacks();
                if (callbacks != null) {
                    for (ToolCallback cb : callbacks) {
                        // 包裹缓存装饰器：相同参数直接返回缓存，跳过外部 API 调用
                        list.add(new CachingToolCallback(cb, cacheService, activity));
                    }
                }
            }
        }
        return list.toArray(new ToolCallback[0]);
    }

    /** 工具结果预览的最大字符数 */
    private static final int RESULT_PREVIEW_MAX_LENGTH = 120;

    /**
     * 截取工具返回结果的前一小段，去除换行后用于前端进度展示，避免长 JSON/数组刷屏。
     *
     * @param result 工具原始返回结果
     * @return 截断后的预览文本；结果为空时返回 null（不展示）
     */
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

    /**
     * 生成工具描述文本，嵌入 userPrompt 让 LLM 知道可以调用哪些工具
     */
    private String buildToolsDescription(ToolCallback[] callbacks) {
        if (callbacks.length == 0) return "  (暂无可用数据工具)";
        return Arrays.stream(callbacks)
                .map(cb -> String.format("  - %s: %s",
                        cb.getToolDefinition().name(),
                        cb.getToolDefinition().description()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 子类实现：Agent 专属系统提示词（投资风格和分析框架）
     */
    protected abstract String getSystemPrompt();

    /**
     * 构造用户提示词，告知 LLM 分析目标和可用工具。子类可覆盖实现专属风格。
     */
    protected String buildUserPrompt(String ticker, String endDate, String toolsDescription) {
        return String.format(
                "请分析股票 %s 的投资机会（参考日期：%s）。\n\n" +
                "可用工具列表：\n%s\n\n" +
                "请根据你的投资风格选择合适的工具获取数据，完成分析后以 JSON 格式返回：\n" +
                "{\n" +
                "  \"signal\": \"bullish\" | \"bearish\" | \"neutral\",\n" +
                "  \"confidence\": <0-100的整数>,\n" +
                "  \"reasoning\": \"<详细分析理由>\"\n" +
                "}",
                ticker, endDate, toolsDescription
        );
    }
}
