package com.aihedgefund.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

/**
 * LLM 客户端工厂（基于 Spring AI 1.0.0）
 * 支持 OpenAI / Anthropic / Groq / Ollama 四个 Provider
 *
 * 注意：Spring AI 的 OpenAiApi 默认追加 /v1/chat/completions，
 * 对于使用非标路径的兼容端点（如讯飞 /v2/chat/completions），
 * 需要通过 openai.completions-path 配置覆盖。
 */
@Component
public class LlmClientFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmClientFactory.class);

    @Value("${openai.api-key:}")
    private String openAiKey;

    @Value("${openai.base-url:https://api.openai.com}")
    private String openAiBaseUrl;

    /** Spring AI 默认值 /v1/chat/completions，讯飞 MaaS 等兼容端点需配置为 /v2/chat/completions */
    @Value("${openai.completions-path:/v1/chat/completions}")
    private String openAiCompletionsPath;

    @Value("${anthropic.api-key:}")
    private String anthropicKey;

    @Value("${groq.api-key:}")
    private String groqKey;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    /**
     * 创建 ChatModel 实例
     *
     * @param modelName     模型名称（如 gpt-4o, astron-code-latest）
     * @param modelProvider Provider 名称（OpenAI / Anthropic / Groq / Ollama）
     * @param maxRetries    最大重试次数（由 StructuredOutputHelper 外层循环处理，此参数保留兼容签名）
     */
    public ChatModel create(String modelName, String modelProvider, int maxRetries) {
        log.info("创建 LLM 客户端, provider={}, model={}", modelProvider, modelName);
        return switch (modelProvider) {
            case "OpenAI" -> buildOpenAiModel(modelName, openAiBaseUrl,
                    openAiKey.isEmpty() ? "no-key" : openAiKey, openAiCompletionsPath);
            case "Anthropic" -> buildAnthropicModel(modelName);
            case "Groq" -> buildOpenAiModel(modelName, "https://api.groq.com/openai/v1",
                    groqKey.isEmpty() ? "no-key" : groqKey, "/v1/chat/completions");
            case "Ollama" -> buildOllamaModel(modelName);
            default -> {
                log.warn("不支持的 Provider: {}，回退到 OpenAI", modelProvider);
                yield buildOpenAiModel(modelName, openAiBaseUrl,
                        openAiKey.isEmpty() ? "no-key" : openAiKey, openAiCompletionsPath);
            }
        };
    }


    private OpenAiChatModel buildOpenAiModel(String modelName, String baseUrl,
                                              String apiKey, String completionsPath) {
        log.debug("构建 OpenAI ChatModel, baseUrl={}, completionsPath={}, model={}",
                baseUrl, completionsPath, modelName);
        // !! 关键：用 BufferingClientHttpRequestFactory 缓冲响应体 !!
        //
        // 缓冲工厂会把响应体读入内存，每次 getBody() 都返回一个基于同一份字节数组的新流，
        // 因此拦截器可以完整读取响应体打日志，Spring AI 之后仍能再次读取并解析 tool_calls，
        // 工具调用循环不受影响。
        //
        // 对比无缓冲的 JdkClientHttpRequestFactory：readAllBytes() 会耗尽流，
        // 导致 Spring AI 读到空响应、感知不到工具调用。
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(new BufferingClientHttpRequestFactory(new JdkClientHttpRequestFactory()))
                .requestInterceptor((request, reqBody, execution) -> {
                    // ============ 给模型的输入（请求体，完整输出） ============
                    String reqBodyStr = reqBody != null
                            ? new String(reqBody, StandardCharsets.UTF_8) : "";
                    boolean hasToolDefs = reqBodyStr.contains("\"tools\"");
                    log.info("[LLM HTTP] >>> 请求 {} {}", request.getMethod(), request.getURI());
                    log.info("[LLM HTTP] >>> 模型输入 body ({} bytes, 含工具定义={}):\n{}",
                            reqBody != null ? reqBody.length : 0, hasToolDefs, reqBodyStr);

                    // --- 执行请求 ---
                    var response = execution.execute(request, reqBody);

                    // ============ 模型的响应（响应体，完整输出） ============
                    // 缓冲工厂保证此处 readAllBytes() 后 Spring AI 仍能再次读取
                    MediaType contentType = response.getHeaders().getContentType();
                    byte[] bodyBytes = response.getBody().readAllBytes();
                    String bodyStr = new String(bodyBytes, StandardCharsets.UTF_8);
                    log.info("[LLM HTTP] <<< 响应 status={}, contentType={}", response.getStatusCode(), contentType);
                    log.info("[LLM HTTP] <<< 模型响应 body ({} bytes):\n{}", bodyBytes.length, bodyStr);

                    // --- text/html 错误检测（认证失败或路径错误） ---
                    if (contentType != null && contentType.isCompatibleWith(MediaType.TEXT_HTML)) {
                        log.warn("[LLM HTTP] API 返回 text/html，可能是认证错误或路径配置错误");
                        throw new RuntimeException(
                                "API 返回 text/html（认证错误或路径配置错误，请检查 openai.base-url 和 openai.completions-path）");
                    }

                    // --- 工具调用观察 ---
                    if (bodyStr.contains("\"tool_calls\"")) {
                        log.info("[LLM HTTP] <<< 模型选择 [工具调用]");
                    } else if (hasToolDefs) {
                        log.info("[LLM HTTP] <<< 模型 [跳过工具] 直接文本回复");
                    }

                    // 缓冲响应体未被消耗，Spring AI 可正常解析 tool_calls，驱动工具调用循环
                    return response;
                });
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .completionsPath(completionsPath)
                .restClientBuilder(restClientBuilder)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelName)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    private AnthropicChatModel buildAnthropicModel(String modelName) {
        log.debug("构建 Anthropic ChatModel, model={}", modelName);
        AnthropicApi api = AnthropicApi.builder()
                .apiKey(anthropicKey.isEmpty() ? "no-key" : anthropicKey)
                .build();
        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(modelName)
                .build();
        return AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(options)
                .build();
    }

    private OllamaChatModel buildOllamaModel(String modelName) {
        log.debug("构建 Ollama ChatModel, baseUrl={}, model={}", ollamaBaseUrl, modelName);
        OllamaApi api = OllamaApi.builder()
                .baseUrl(ollamaBaseUrl)
                .build();
        OllamaOptions options = OllamaOptions.builder()
                .model(modelName)
                .build();
        return OllamaChatModel.builder()
                .ollamaApi(api)
                .defaultOptions(options)
                .build();
    }
}