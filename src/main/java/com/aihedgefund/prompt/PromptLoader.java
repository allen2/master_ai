package com.aihedgefund.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统提示词加载器。
 *
 * <p>将 Agent 的系统提示词从代码中外置到 classpath 下的 Markdown 文件
 * （{@code prompts/{name}.md}），由各 Agent 在 Bean 实例化（系统启动）时加载，
 * 加载结果按文件名缓存，避免重复读取。</p>
 */
@Component
public class PromptLoader {

    private static final Logger log = LoggerFactory.getLogger(PromptLoader.class);

    /** 提示词文件路径模板，相对 classpath */
    private static final String PROMPT_PATH_PATTERN = "prompts/%s.md";

    /** 提示词缓存：文件名 → 提示词内容 */
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    /**
     * 加载指定名称的系统提示词。
     *
     * @param promptName 提示词文件名（不含 .md 后缀），如 warren_buffett
     * @return 提示词内容（已去除首尾空白）
     * @throws IllegalStateException 文件不存在或读取失败时抛出
     */
    public String load(String promptName) {
        return promptCache.computeIfAbsent(promptName, this::readFromClasspath);
    }

    /**
     * 从 classpath 读取提示词文件内容。
     *
     * @param promptName 提示词文件名（不含 .md 后缀）
     * @return 提示词内容
     */
    private String readFromClasspath(String promptName) {
        String path = String.format(PROMPT_PATH_PATTERN, promptName);
        ClassPathResource resource = new ClassPathResource(path);

        if (!resource.exists()) {
            log.error("系统提示词文件不存在: classpath:{}", path);
            throw new IllegalStateException("系统提示词文件不存在: classpath:" + path);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            String content = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8).trim();
            log.info("加载系统提示词: classpath:{} ({} 字符)", path, content.length());
            return content;
        } catch (IOException e) {
            log.error("读取系统提示词文件失败: classpath:{}, error={}", path, e.getMessage());
            throw new IllegalStateException("读取系统提示词文件失败: classpath:" + path, e);
        }
    }
}
