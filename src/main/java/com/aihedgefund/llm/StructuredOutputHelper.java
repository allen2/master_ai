package com.aihedgefund.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 结构化 LLM 输出辅助（基于 Spring AI 1.0.0）
 * 带重试和默认值降级逻辑，处理模型输出不规范的情况
 */
@Component
public class StructuredOutputHelper {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputHelper.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*}[^{}]*)*}");

    /**
     * 调用 LLM 并将输出解析为指定类型，失败时返回 defaultFactory 的结果
     *
     * @param llm            语言模型
     * @param systemPrompt   系统提示
     * @param userPrompt     用户提示
     * @param responseType   期望的返回类型
     * @param defaultFactory 失败时的默认值工厂
     * @param maxRetries     最大重试次数
     */
    public <T> T call(ChatModel llm,
                      String systemPrompt,
                      String userPrompt,
                      Class<T> responseType,
                      Supplier<T> defaultFactory,
                      int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                log.debug("LLM 输入 (attempt {}/{}):\n[system]\n{}\n[user]\n{}",
                        attempt + 1, maxRetries, systemPrompt, userPrompt);

                Prompt prompt = new Prompt(List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userPrompt)
                ));
                ChatResponse response = llm.call(prompt);
                String content = response.getResult().getOutput().getText();
                log.debug("LLM 原始输出: {}", content);

                T result = parseJson(content, responseType);
                if (result != null) {
                    return result;
                }
                log.warn("JSON 解析失败 (attempt {}/{}), 原始输出: {}", attempt + 1, maxRetries, content);

            } catch (Exception e) {
                log.warn("LLM 调用失败 (attempt {}/{}): {}", attempt + 1, maxRetries, e.getMessage());
            }
        }

        log.error("全部 {} 次重试失败，使用默认值", maxRetries);
        return defaultFactory.get();
    }

    /**
     * 解析 LLM 文本输出为指定类型（工具调用模式专用，不发起 LLM 请求）
     *
     * @param content        LLM 返回的原始字符串
     * @param responseType   期望的目标类型
     * @param defaultFactory 解析失败时的默认值工厂
     */
    public <T> T parse(String content, Class<T> responseType, Supplier<T> defaultFactory) {
        if (content == null || content.isBlank()) {
            log.warn("LLM 响应为空，返回默认值");
            return defaultFactory.get();
        }
        T result = parseJson(content, responseType);
        if (result != null) {
            return result;
        }
        log.warn("工具调用模式响应 JSON 解析失败，原始响应: {}", content);
        return defaultFactory.get();
    }

    /**
     * 从 LLM 输出中提取 JSON 并反序列化
     */
    private <T> T parseJson(String content, Class<T> type) {
        if (content == null || content.isBlank()) return null;

        // 1. 直接解析整个内容
        try {
            return MAPPER.readValue(content.trim(), type);
        } catch (JsonProcessingException ignored) {}

        // 2. 提取 ```json ... ``` 代码块
        int jsonStart = content.indexOf("```json");
        if (jsonStart != -1) {
            String after = content.substring(jsonStart + 7);
            int jsonEnd = after.indexOf("```");
            if (jsonEnd != -1) {
                try {
                    return MAPPER.readValue(after.substring(0, jsonEnd).trim(), type);
                } catch (JsonProcessingException ignored) {}
            }
        }

        // 3. 用正则提取第一个 JSON 对象
        Matcher matcher = JSON_PATTERN.matcher(content);
        while (matcher.find()) {
            try {
                return MAPPER.readValue(matcher.group(), type);
            } catch (JsonProcessingException ignored) {}
        }

        return null;
    }
}
