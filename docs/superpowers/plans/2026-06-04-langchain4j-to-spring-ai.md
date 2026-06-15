# LangChain4j → Spring AI 迁移实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 ai-hedge-fund-java 的 LLM 层从 LangChain4j 0.36.2 迁移到 Spring AI 1.0.0，保持所有功能不变。

**Architecture:** 只替换 `llm/` 包中的两个类和 `BaseLlmAgent` 中的 import，所有 Agent 类（investor/analyst/specialist）不需要任何改动。Spring AI 的 `ChatModel` 接口对应 LangChain4j 的 `ChatLanguageModel`，`ChatResponse` 对应 `Response<AiMessage>`。

**Tech Stack:** Spring AI 1.0.0（spring-ai-openai, spring-ai-anthropic, spring-ai-ollama），Spring Boot 3.2，Java 17

---

## 文件变更清单

| 文件 | 操作 | 原因 |
|------|------|------|
| `pom.xml` | 修改 | 移除 LangChain4j 4 个依赖，加入 Spring AI BOM + 3 个模型库 |
| `src/main/java/com/aihedgefund/llm/LlmClientFactory.java` | 重写 | 用 Spring AI API 替代 LangChain4j Builder |
| `src/main/java/com/aihedgefund/llm/StructuredOutputHelper.java` | 修改 | 替换消息类型和响应类型 |
| `src/main/java/com/aihedgefund/agent/BaseLlmAgent.java` | 修改 | import `ChatModel` 替代 `ChatLanguageModel` |
| `src/test/java/com/aihedgefund/llm/StructuredOutputHelperTest.java` | 重写 | mock 类型从 LangChain4j 换为 Spring AI |

**不需要改动：** 所有 Agent 类、Controller、Service、Mapper、其他测试。

---

## LangChain4j → Spring AI API 对照

| LangChain4j | Spring AI |
|-------------|-----------|
| `dev.langchain4j.model.chat.ChatLanguageModel` | `org.springframework.ai.chat.model.ChatModel` |
| `dev.langchain4j.data.message.SystemMessage.from(text)` | `new org.springframework.ai.chat.messages.SystemMessage(text)` |
| `dev.langchain4j.data.message.UserMessage.from(text)` | `new org.springframework.ai.chat.messages.UserMessage(text)` |
| `model.generate(List<ChatMessage>)` | `model.call(new Prompt(List<Message>))` |
| `Response<AiMessage>` | `org.springframework.ai.chat.model.ChatResponse` |
| `response.content().text()` | `response.getResult().getOutput().getContent()` |

---

## Task 1: 替换 pom.xml 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 删除 `<langchain4j.version>` property 并添加 `<spring-ai.version>`**

在 `<properties>` 块中，删除：
```xml
<langchain4j.version>0.36.2</langchain4j.version>
```
添加（与 `<java.version>17</java.version>` 并排）：
```xml
<spring-ai.version>1.0.0</spring-ai.version>
```

- [ ] **Step 2: 在 `<dependencies>` 前插入 `<dependencyManagement>`**

在 `<dependencies>` 开标签之前插入：
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

- [ ] **Step 3: 删除 4 个 LangChain4j 依赖块**

删除以下 4 个 `<dependency>` 块：
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-anthropic</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

- [ ] **Step 4: 添加 3 个 Spring AI 依赖（在 sqlite-jdbc 之后）**

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-anthropic</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama</artifactId>
</dependency>
```

- [ ] **Step 5: 验证依赖解析（只下载不编译）**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn dependency:resolve -q
```

预期：`BUILD SUCCESS`，无 `Could not resolve` 错误。

- [ ] **Step 6: Commit**

```bash
git add pom.xml
git commit -m "build: replace LangChain4j with Spring AI 1.0.0"
```

---

## Task 2: 重写 LlmClientFactory

**Files:**
- Modify: `src/main/java/com/aihedgefund/llm/LlmClientFactory.java`

- [ ] **Step 1: 用完整新内容替换文件**

```java
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
import org.springframework.stereotype.Component;

/**
 * LLM 客户端工厂（基于 Spring AI 1.0.0）
 * 支持 OpenAI / Anthropic / Groq / Ollama 四个 Provider
 * OpenAI Provider 通过 OPENAI_API_BASE 支持自定义端点（讯飞 MaaS 等）
 */
@Component
public class LlmClientFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmClientFactory.class);

    @Value("${openai.api-key:}")
    private String openAiKey;

    @Value("${openai.base-url:https://api.openai.com}")
    private String openAiBaseUrl;

    @Value("${anthropic.api-key:}")
    private String anthropicKey;

    @Value("${groq.api-key:}")
    private String groqKey;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    /**
     * 创建 ChatModel 实例
     *
     * @param modelName     模型名称（如 gpt-4o, claude-opus-4-8）
     * @param modelProvider Provider 名称（OpenAI / Anthropic / Groq / Ollama）
     * @param maxRetries    最大重试次数（由 StructuredOutputHelper 外层循环处理，此参数保留兼容签名）
     */
    public ChatModel create(String modelName, String modelProvider, int maxRetries) {
        log.info("创建 LLM 客户端, provider={}, model={}", modelProvider, modelName);
        return switch (modelProvider) {
            case "OpenAI" -> buildOpenAiModel(modelName, openAiBaseUrl,
                    openAiKey.isEmpty() ? "no-key" : openAiKey);
            case "Anthropic" -> buildAnthropicModel(modelName);
            case "Groq" -> buildOpenAiModel(modelName, "https://api.groq.com/openai/v1",
                    groqKey.isEmpty() ? "no-key" : groqKey);
            case "Ollama" -> buildOllamaModel(modelName);
            default -> {
                log.warn("不支持的 Provider: {}，回退到 OpenAI", modelProvider);
                yield buildOpenAiModel(modelName, openAiBaseUrl,
                        openAiKey.isEmpty() ? "no-key" : openAiKey);
            }
        };
    }

    public ChatModel createDefault() {
        return create("gpt-4o", "OpenAI", 3);
    }

    private OpenAiChatModel buildOpenAiModel(String modelName, String baseUrl, String apiKey) {
        OpenAiApi api = new OpenAiApi(baseUrl, apiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelName)
                .build();
        return new OpenAiChatModel(api, options);
    }

    private AnthropicChatModel buildAnthropicModel(String modelName) {
        AnthropicApi api = new AnthropicApi(
                anthropicKey.isEmpty() ? "no-key" : anthropicKey);
        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(modelName)
                .build();
        return new AnthropicChatModel(api, options);
    }

    private OllamaChatModel buildOllamaModel(String modelName) {
        OllamaApi api = new OllamaApi(ollamaBaseUrl);
        OllamaOptions options = OllamaOptions.builder()
                .model(modelName)
                .build();
        return new OllamaChatModel(api, options);
    }
}
```

- [ ] **Step 2: 编译验证**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn compile -pl . -am 2>&1 | Select-String "ERROR|BUILD" | Select-Object -Last 5
```

预期：`BUILD SUCCESS`（若某个 builder 方法名报错，如 `.model()` 不存在，改为 `.withModel()`）

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aihedgefund/llm/LlmClientFactory.java
git commit -m "feat: migrate LlmClientFactory to Spring AI ChatModel"
```

---

## Task 3: 更新 StructuredOutputHelper

**Files:**
- Modify: `src/main/java/com/aihedgefund/llm/StructuredOutputHelper.java`

- [ ] **Step 1: 替换 import 和方法体**

完整替换文件内容：

```java
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
                String content = response.getResult().getOutput().getContent();
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
```

- [ ] **Step 2: 编译验证**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn compile -q 2>&1 | Select-String "ERROR" | Select-Object -Last 5
```

预期：无输出（表示编译成功）

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aihedgefund/llm/StructuredOutputHelper.java
git commit -m "feat: migrate StructuredOutputHelper to Spring AI ChatModel/ChatResponse"
```

---

## Task 4: 更新 BaseLlmAgent

**Files:**
- Modify: `src/main/java/com/aihedgefund/agent/BaseLlmAgent.java`

- [ ] **Step 1: 替换 import 和字段类型**

完整替换文件内容：

```java
package com.aihedgefund.agent;

import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.orchestrator.AgentSignal;
import com.aihedgefund.orchestrator.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;

/**
 * LLM-powered Agent 基类（基于 Spring AI 1.0.0）
 * 封装 LLM 调用、重试、降级逻辑，子类只需实现 buildAnalysisData() 和 getSystemPrompt()
 */
public abstract class BaseLlmAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(BaseLlmAgent.class);

    protected final LlmClientFactory llmFactory;
    protected final StructuredOutputHelper outputHelper;

    protected BaseLlmAgent(LlmClientFactory llmFactory, StructuredOutputHelper outputHelper) {
        this.llmFactory = llmFactory;
        this.outputHelper = outputHelper;
    }

    @Override
    public void analyze(AgentState state, List<String> tickers) {
        String modelName = state.getModelName() != null ? state.getModelName() : "gpt-4o";
        String modelProvider = state.getModelProvider() != null ? state.getModelProvider() : "OpenAI";
        ChatModel llm = llmFactory.create(modelName, modelProvider, 3);

        for (String ticker : tickers) {
            log.info("[{}] LLM 分析, ticker={}, model={}", getAgentId(), ticker, modelName);
            try {
                String analysisData = buildAnalysisData(state, ticker);
                String userPrompt = buildUserPrompt(ticker, analysisData);

                AgentSignal signal = outputHelper.call(
                        llm,
                        getSystemPrompt(),
                        userPrompt,
                        AgentSignal.class,
                        () -> AgentSignal.neutral(30, "LLM 调用失败，使用默认中性信号"),
                        3
                );
                state.putSignal(getAgentId(), ticker, signal);
                log.debug("[{}] ticker={} signal={}", getAgentId(), ticker, signal);

            } catch (Exception e) {
                log.error("[{}] 分析 {} 失败: {}", getAgentId(), ticker, e.getMessage());
                state.putSignal(getAgentId(), ticker,
                        AgentSignal.neutral(20, "分析失败: " + e.getMessage()));
            }
        }
    }

    /**
     * 子类实现：收集该 Agent 所需的财务数据并序列化为字符串
     */
    protected abstract String buildAnalysisData(AgentState state, String ticker);

    /**
     * 子类实现：Agent 专属系统提示词
     */
    protected abstract String getSystemPrompt();

    /**
     * 构造统一的用户提示词格式
     */
    protected String buildUserPrompt(String ticker, String analysisData) {
        return String.format(
                "分析股票 %s 的投资机会。\n\n财务数据：\n%s\n\n" +
                "请以 JSON 格式返回分析结果，包含以下字段：\n" +
                "- signal: \"bullish\" | \"bearish\" | \"neutral\"\n" +
                "- confidence: 0-100 的整数\n" +
                "- reasoning: 分析理由(中文描述）",
                ticker, analysisData
        );
    }
}
```

- [ ] **Step 2: 编译验证**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn compile -q 2>&1 | Select-String "ERROR" | Select-Object -Last 5
```

预期：无输出（编译成功）

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aihedgefund/agent/BaseLlmAgent.java
git commit -m "feat: update BaseLlmAgent to use Spring AI ChatModel"
```

---

## Task 5: 更新 StructuredOutputHelperTest

**Files:**
- Modify: `src/test/java/com/aihedgefund/llm/StructuredOutputHelperTest.java`

- [ ] **Step 1: 写失败态测试（新的 Spring AI mock 结构）**

完整替换文件内容：

```java
package com.aihedgefund.llm;

import com.aihedgefund.orchestrator.AgentSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class StructuredOutputHelperTest {

    private StructuredOutputHelper helper;
    private ChatModel mockLlm;

    @BeforeEach
    void setUp() {
        helper = new StructuredOutputHelper();
        mockLlm = Mockito.mock(ChatModel.class);
    }

    private ChatResponse mockResponse(String text) {
        AssistantMessage msg = new AssistantMessage(text);
        return new ChatResponse(List.of(new Generation(msg)));
    }

    @Test
    void call_validJson_returnsSignal() {
        String json = "{\"signal\":\"bullish\",\"confidence\":75,\"reasoning\":\"strong fundamentals\"}";
        when(mockLlm.call(any(Prompt.class))).thenReturn(mockResponse(json));

        AgentSignal result = helper.call(mockLlm, "system", "user",
                AgentSignal.class, () -> AgentSignal.neutral(30, "default"), 3);

        assertThat(result.getSignal()).isEqualTo("bullish");
        assertThat(result.getConfidence()).isEqualTo(75);
        assertThat(result.getReasoning()).isEqualTo("strong fundamentals");
    }

    @Test
    void call_jsonInCodeBlock_extractsCorrectly() {
        String response = "Here is the analysis:\n```json\n{\"signal\":\"bearish\",\"confidence\":60,\"reasoning\":\"high debt\"}\n```";
        when(mockLlm.call(any(Prompt.class))).thenReturn(mockResponse(response));

        AgentSignal result = helper.call(mockLlm, "system", "user",
                AgentSignal.class, () -> AgentSignal.neutral(30, "default"), 3);

        assertThat(result.getSignal()).isEqualTo("bearish");
    }

    @Test
    void call_invalidJson_returnsDefault() {
        when(mockLlm.call(any(Prompt.class)))
                .thenReturn(mockResponse("Sorry, I cannot analyze this."));

        AgentSignal defaultSignal = AgentSignal.neutral(25, "fallback");
        AgentSignal result = helper.call(mockLlm, "system", "user",
                AgentSignal.class, () -> defaultSignal, 1);

        assertThat(result.getSignal()).isEqualTo("neutral");
        assertThat(result.getReasoning()).isEqualTo("fallback");
    }

    @Test
    void call_embeddedJson_extractsCorrectly() {
        String response = "Analysis result: {\"signal\":\"neutral\",\"confidence\":50,\"reasoning\":\"mixed signals\"} based on the data.";
        when(mockLlm.call(any(Prompt.class))).thenReturn(mockResponse(response));

        AgentSignal result = helper.call(mockLlm, "system", "user",
                AgentSignal.class, () -> AgentSignal.neutral(30, "default"), 1);

        assertThat(result.getSignal()).isEqualTo("neutral");
    }

    @Test
    void call_llmThrowsException_retriesAndReturnsDefault() {
        when(mockLlm.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("LLM service unavailable"));

        AgentSignal defaultSignal = AgentSignal.neutral(10, "error fallback");
        AgentSignal result = helper.call(mockLlm, "system", "user",
                AgentSignal.class, () -> defaultSignal, 2);

        assertThat(result.getReasoning()).isEqualTo("error fallback");
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn test -Dtest=StructuredOutputHelperTest 2>&1 | Select-String "Tests run|FAIL|ERROR|BUILD" | Select-Object -Last 5
```

预期：`Tests run: 5, Failures: 0, Errors: 0`，`BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/aihedgefund/llm/StructuredOutputHelperTest.java
git commit -m "test: update StructuredOutputHelperTest for Spring AI ChatModel"
```

---

## Task 6: 全量测试验证

**Files:** 无文件改动，仅验证

- [ ] **Step 1: 运行所有测试**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn test -DskipFrontend=true 2>&1 | Select-String "Tests run|FAIL|ERROR|BUILD SUCCESS" | Select-Object -Last 10
```

预期：所有已有测试通过，`BUILD SUCCESS`。

> **注意：** `HedgeFundOrchestratorTest` 中的 `mockHelper = Mockito.mock(StructuredOutputHelper.class)` 在方法签名改变（`ChatLanguageModel` → `ChatModel`）后仍然有效，因为 `StructuredOutputHelper` 整个类被 mock，Mockito 会自动适配新签名。

- [ ] **Step 2: 如有测试失败，检查以下已知风险点**

若 `HedgeFundControllerTest` 失败：无关 Spring AI，检查是否有其他 mock 设置问题。

若编译报 `cannot find symbol: method model(String)` for `OpenAiChatOptions.builder()`：  
将 Task 2 中所有 `.model(modelName)` 改为 `.withModel(modelName)`（部分 Spring AI 版本使用 with 前缀）：
```java
OpenAiChatOptions options = OpenAiChatOptions.builder()
        .withModel(modelName)   // 改这里
        .build();
```

若 `AnthropicChatModel` 构造函数报错（参数不匹配）：
```java
// 尝试三参数构造
AnthropicChatModel model = new AnthropicChatModel(api, options, 
    RetryUtils.DEFAULT_RETRY_TEMPLATE);
```

- [ ] **Step 3: 验证应用启动**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn spring-boot:run 2>&1 | Select-String "Started|ERROR" | Select-Object -First 5
```

预期：`Started AiHedgeFundApplication in X seconds`，无 `ERROR`。
