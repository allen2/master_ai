# Spring AI Tool Calling + MCP Server 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 ai-hedge-fund-java 实现 Spring AI Tool Calling，让 LLM Agent 主动调用金融数据工具获取所需数据，同时将工具暴露为标准 MCP Server 供外部客户端使用。

**Architecture:** 新建 `FinancialDataTools`（`@Component` + `@Tool`）封装 4 个金融 API，通过 `@Autowired` 注入 `BaseLlmAgent`；`BaseLlmAgent.analyze()` 改用 `ChatClient.prompt().tools(financialDataTools)` 多轮工具调用；同时添加 `spring-ai-mcp-server-spring-boot-starter` 并注册 `ToolCallbackProvider` bean，使外部 MCP 客户端（Claude Desktop、Cursor 等）可连接这些工具。

**Tech Stack:** Spring AI 1.0.0（spring-ai-mcp-server-spring-boot-starter），`ChatClient`，`@Tool`，`MethodToolCallbackProvider`，Jackson

---

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/main/java/com/aihedgefund/llm/FinancialDataTools.java` | **新建** | 4 个 @Tool 方法封装 FinancialDatasetsClient |
| `src/main/java/com/aihedgefund/llm/StructuredOutputHelper.java` | 修改 | 新增 `parse(String, Class, Supplier)` 公共方法 |
| `src/main/java/com/aihedgefund/agent/BaseLlmAgent.java` | 重写 | @Autowired tools，ChatClient 工具调用，删除 buildAnalysisData() 抽象方法 |
| 13 个 investor agents + 2 specialist agents | 修改 | 删除 FinancialDatasetsClient 字段、构造参数、buildAnalysisData() |
| `src/main/java/com/aihedgefund/agent/portfolio/PortfolioManagerAgent.java` | 修改 | 删除空的 buildAnalysisData() |
| `pom.xml` | 修改 | 新增 spring-ai-mcp-server-spring-boot-starter |
| `src/main/java/com/aihedgefund/config/McpConfiguration.java` | **新建** | 注册 ToolCallbackProvider bean |
| `src/main/resources/application.yml` | 修改 | 添加 spring.ai.mcp.server 配置 |
| `src/test/java/com/aihedgefund/llm/FinancialDataToolsTest.java` | **新建** | 4 个工具方法单元测试 |
| `src/test/java/com/aihedgefund/llm/StructuredOutputHelperTest.java` | 修改 | 新增 parse() 方法测试 |

---

## Task 1: 新建 FinancialDataTools + 测试

**Files:**
- Create: `src/main/java/com/aihedgefund/llm/FinancialDataTools.java`
- Create: `src/test/java/com/aihedgefund/llm/FinancialDataToolsTest.java`

- [ ] **Step 1: 创建测试文件（失败态）**

```java
// src/test/java/com/aihedgefund/llm/FinancialDataToolsTest.java
package com.aihedgefund.llm;

import com.aihedgefund.data.client.FinancialDatasetsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class FinancialDataToolsTest {

    private FinancialDatasetsClient mockClient;
    private FinancialDataTools tools;

    @BeforeEach
    void setUp() {
        mockClient = Mockito.mock(FinancialDatasetsClient.class);
        tools = new FinancialDataTools(mockClient);
    }

    @Test
    void getPrices_returnJsonArray() {
        when(mockClient.getPrices("AAPL", "2024-01-01", "2024-12-31"))
                .thenReturn(List.of(Map.of("close", 150.0, "time", "2024-01-02")));

        String result = tools.getPrices("AAPL", "2024-01-01", "2024-12-31");

        assertThat(result).startsWith("[");
        assertThat(result).contains("150.0");
    }

    @Test
    void getFinancialMetrics_returnJsonArray() {
        when(mockClient.getFinancialMetrics("AAPL", "2024-12-31", "ttm", 3))
                .thenReturn(List.of(Map.of("return_on_equity", 1.64, "net_margin", 0.27)));

        String result = tools.getFinancialMetrics("AAPL", "2024-12-31", "ttm", 3);

        assertThat(result).contains("return_on_equity");
        assertThat(result).contains("1.64");
    }

    @Test
    void getInsiderTrades_returnJsonArray() {
        when(mockClient.getInsiderTrades("AAPL", "2024-12-31", 20))
                .thenReturn(List.of(Map.of("transaction_type", "purchase", "shares", 1000)));

        String result = tools.getInsiderTrades("AAPL", "2024-12-31", 20);

        assertThat(result).contains("purchase");
    }

    @Test
    void getLineItems_parsesCommaSeparated_andCallsClient() {
        when(mockClient.getLineItems(eq("AAPL"), anyList(), eq("2024-12-31"), eq("annual"), eq(3)))
                .thenReturn(List.of(Map.of("revenue", 400_000_000_000L)));

        String result = tools.getLineItems("AAPL", "revenue,net_income", "2024-12-31", "annual", 3);

        assertThat(result).contains("revenue");
    }

    @Test
    void nullClientResponse_returnsEmptyJsonArray() {
        when(mockClient.getPrices(anyString(), anyString(), anyString())).thenReturn(null);

        String result = tools.getPrices("FAKE", "2024-01-01", "2024-12-31");

        assertThat(result).isEqualTo("[]");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "d:\workspace\ai-hedge\ai-hedge-fund-java"
mvn test -Dtest=FinancialDataToolsTest 2>&1 | Select-String "ERROR|FAIL|BUILD" | Select-Object -Last 5
```

预期：`FAIL` — `Cannot find class FinancialDataTools`

- [ ] **Step 3: 创建 FinancialDataTools.java**

```java
// src/main/java/com/aihedgefund/llm/FinancialDataTools.java
package com.aihedgefund.llm;

import com.aihedgefund.data.client.FinancialDatasetsClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 金融数据工具集（Spring AI @Tool）
 * 供 LLM Agent 主动调用获取所需数据，同时暴露为 MCP Server 工具。
 */
@Component
public class FinancialDataTools {

    private static final Logger log = LoggerFactory.getLogger(FinancialDataTools.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FinancialDatasetsClient client;

    public FinancialDataTools(FinancialDatasetsClient client) {
        this.client = client;
    }

    @Tool(description = "获取股票历史价格数据（日线 OHLCV），返回 JSON 数组。用于技术分析、趋势判断、波动率计算。")
    public String getPrices(String ticker, String startDate, String endDate) {
        log.debug("[Tool] getPrices: ticker={}, {} ~ {}", ticker, startDate, endDate);
        List<Map<String, Object>> data = client.getPrices(ticker, startDate, endDate);
        return toJson(data);
    }

    @Tool(description = "获取股票财务指标，包括 ROE、毛利率、净利率、市盈率、负债率等关键指标。"
            + "period 填 ttm（滚动12月）或 annual（年度），limit 填 1-5。")
    public String getFinancialMetrics(String ticker, String endDate, String period, int limit) {
        log.debug("[Tool] getFinancialMetrics: ticker={}, period={}, limit={}", ticker, period, limit);
        List<Map<String, Object>> data = client.getFinancialMetrics(ticker, endDate, period, limit);
        return toJson(data);
    }

    @Tool(description = "获取股票内部人交易记录（高管买卖动向），反映公司内部人对未来的信心。limit 填 10-50。")
    public String getInsiderTrades(String ticker, String endDate, int limit) {
        log.debug("[Tool] getInsiderTrades: ticker={}, limit={}", ticker, limit);
        List<Map<String, Object>> data = client.getInsiderTrades(ticker, endDate, limit);
        return toJson(data);
    }

    @Tool(description = "获取财务明细行数据（营收、净利润、自由现金流等）。"
            + "lineItems 为逗号分隔字段名如 revenue,net_income,free_cash_flow；"
            + "period 填 ttm 或 annual；limit 填 1-5。")
    public String getLineItems(String ticker, String lineItems, String endDate, String period, int limit) {
        log.debug("[Tool] getLineItems: ticker={}, items={}", ticker, lineItems);
        List<String> items = List.of(lineItems.split(","));
        List<Map<String, Object>> data = client.getLineItems(ticker, items, endDate, period, limit);
        return toJson(data);
    }

    private String toJson(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("工具返回值序列化失败: {}", e.getMessage());
            return "[]";
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "d:\workspace\ai-hedge\ai-hedge-fund-java"
mvn test -Dtest=FinancialDataToolsTest 2>&1 | Select-String "Tests run|BUILD" | Select-Object -Last 3
```

预期：`Tests run: 5, Failures: 0`，`BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aihedgefund/llm/FinancialDataTools.java \
        src/test/java/com/aihedgefund/llm/FinancialDataToolsTest.java
git commit -m "feat: add FinancialDataTools with @Tool methods for LLM tool calling"
```

---

## Task 2: 扩展 StructuredOutputHelper + 重写 BaseLlmAgent

**Files:**
- Modify: `src/main/java/com/aihedgefund/llm/StructuredOutputHelper.java`
- Modify: `src/main/java/com/aihedgefund/agent/BaseLlmAgent.java`
- Modify: `src/test/java/com/aihedgefund/llm/StructuredOutputHelperTest.java`

- [ ] **Step 1: 在 StructuredOutputHelperTest 追加 parse() 测试（写在已有测试文件末尾，在最后一个 `}` 前）**

在 `src/test/java/com/aihedgefund/llm/StructuredOutputHelperTest.java` 的最后一个 `}` 前添加：

```java
    @Test
    void parse_validJson_returnsSignal() {
        String json = "{\"signal\":\"bullish\",\"confidence\":80,\"reasoning\":\"工具调用模式\"}";

        AgentSignal result = helper.parse(json, AgentSignal.class,
                () -> AgentSignal.neutral(30, "default"));

        assertThat(result.getSignal()).isEqualTo("bullish");
        assertThat(result.getConfidence()).isEqualTo(80);
    }

    @Test
    void parse_blankContent_returnsDefault() {
        AgentSignal defaultSig = AgentSignal.neutral(10, "no content");

        AgentSignal result = helper.parse("", AgentSignal.class, () -> defaultSig);

        assertThat(result.getReasoning()).isEqualTo("no content");
    }
```

- [ ] **Step 2: 运行新增测试确认失败**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "d:\workspace\ai-hedge\ai-hedge-fund-java"
mvn test -Dtest=StructuredOutputHelperTest 2>&1 | Select-String "Tests run|FAIL|BUILD" | Select-Object -Last 3
```

预期：`FAIL` — `parse` 方法不存在

- [ ] **Step 3: 在 StructuredOutputHelper 中添加 parse() 方法（在 call() 方法之后，parseJson() 之前插入）**

在 `src/main/java/com/aihedgefund/llm/StructuredOutputHelper.java` 的 `call()` 方法结束的 `}` 之后、`parseJson()` 方法之前插入：

```java
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
```

- [ ] **Step 4: 运行 StructuredOutputHelperTest 全部测试通过**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "d:\workspace\ai-hedge\ai-hedge-fund-java"
mvn test -Dtest=StructuredOutputHelperTest 2>&1 | Select-String "Tests run|BUILD" | Select-Object -Last 3
```

预期：`Tests run: 7, Failures: 0`，`BUILD SUCCESS`

- [ ] **Step 5: 重写 BaseLlmAgent.java**

完整替换文件内容：

```java
package com.aihedgefund.agent;

import com.aihedgefund.llm.FinancialDataTools;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.orchestrator.AgentSignal;
import com.aihedgefund.orchestrator.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

/**
 * LLM-powered Agent 基类（Spring AI Tool Calling 模式）
 *
 * 每次分析时创建 ChatClient 并注入 FinancialDataTools，
 * LLM 主动决定调用哪些工具获取数据，多轮推理后输出最终 signal JSON。
 */
public abstract class BaseLlmAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(BaseLlmAgent.class);

    protected final LlmClientFactory llmFactory;
    protected final StructuredOutputHelper outputHelper;

    @Autowired
    private FinancialDataTools financialDataTools;

    protected BaseLlmAgent(LlmClientFactory llmFactory, StructuredOutputHelper outputHelper) {
        this.llmFactory = llmFactory;
        this.outputHelper = outputHelper;
    }

    @Override
    public void analyze(AgentState state, List<String> tickers) {
        String modelName = state.getModelName() != null ? state.getModelName() : "astron-code-latest";
        String modelProvider = state.getModelProvider() != null ? state.getModelProvider() : "OpenAI";
        ChatModel chatModel = llmFactory.create(modelName, modelProvider, 3);
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        for (String ticker : tickers) {
            log.info("[{}] LLM 分析（工具调用模式）, ticker={}, model={}", getAgentId(), ticker, modelName);
            try {
                String endDate = state.getEndDate() != null ? state.getEndDate()
                        : LocalDate.now().toString();
                String userPrompt = buildUserPrompt(ticker, endDate);

                String rawResponse = chatClient.prompt()
                        .system(getSystemPrompt())
                        .user(userPrompt)
                        .tools(financialDataTools)
                        .call()
                        .content();

                log.debug("[{}] ticker={} LLM 原始响应: {}", getAgentId(), ticker, rawResponse);

                AgentSignal signal = outputHelper.parse(rawResponse, AgentSignal.class,
                        () -> AgentSignal.neutral(30, "LLM 调用失败，使用默认中性信号"));
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
     * 子类实现：Agent 专属系统提示词（说明投资风格和分析框架）
     */
    protected abstract String getSystemPrompt();

    /**
     * 构造用户提示词，告知 LLM 分析目标和可用工具
     */
    protected String buildUserPrompt(String ticker, String endDate) {
        return String.format(
                "请分析股票 %s 的投资机会（参考日期：%s）。\n\n" +
                "你可以调用以下工具获取需要的数据：\n" +
                "- getFinancialMetrics：财务指标（ROE、毛利率、净利率等）\n" +
                "- getPrices：历史价格走势\n" +
                "- getInsiderTrades：内部人买卖记录\n" +
                "- getLineItems：财务明细（营收、净利润、FCF 等）\n\n" +
                "根据你的投资风格选择需要的数据，完成分析后以 JSON 格式返回：\n" +
                "- signal: \"bullish\" | \"bearish\" | \"neutral\"\n" +
                "- confidence: 0-100 的整数\n" +
                "- reasoning: 分析理由（中文）",
                ticker, endDate
        );
    }
}
```

- [ ] **Step 6: 编译验证**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "d:\workspace\ai-hedge\ai-hedge-fund-java"
mvn compile -q 2>&1 | Select-String "ERROR" | Select-Object -Last 5
```

预期：无输出（编译成功）

> **注意**：若 `org.springframework.ai.chat.client.ChatClient` 找不到，检查 `spring-ai-openai` 是否包含了 `ChatClient`。Spring AI 1.0.0 中 `ChatClient` 在 `spring-ai-core` 模块，已通过 BOM 传递依赖。若仍缺失，在 pom.xml 显式添加 `spring-ai-core`。

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/aihedgefund/llm/StructuredOutputHelper.java \
        src/main/java/com/aihedgefund/agent/BaseLlmAgent.java \
        src/test/java/com/aihedgefund/llm/StructuredOutputHelperTest.java
git commit -m "feat: add StructuredOutputHelper.parse() and refactor BaseLlmAgent to use ChatClient tool calling"
```

---

## Task 3: 清理 15 个 LLM Agent + PortfolioManagerAgent

**Files:**
- Modify: 所有继承 `BaseLlmAgent` 的 agent 文件（见下方列表）

每个 agent 需要做 **相同的 3 处改动**：
1. 删除 `private static final ObjectMapper MAPPER = new ObjectMapper();`（如有）
2. 删除 `private final FinancialDatasetsClient client;` 字段
3. 从构造函数参数中删除 `FinancialDatasetsClient c`，删除 `this.client = c;`
4. 删除整个 `buildAnalysisData()` 方法

- [ ] **Step 1: 修改 WarrenBuffettAgent（示范完整版本）**

完整替换为：

```java
package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import org.springframework.stereotype.Component;

/** 沃伦·巴菲特 Agent — 寻找具有护城河的优质企业，以合理价格买入 */
@Component
public class WarrenBuffettAgent extends BaseLlmAgent {

    public WarrenBuffettAgent(LlmClientFactory llmFactory, StructuredOutputHelper outputHelper) {
        super(llmFactory, outputHelper);
    }

    @Override public String getAgentId() { return "warren_buffett"; }
    @Override public String getDisplayName() { return "沃伦·巴菲特"; }

    @Override
    protected String getSystemPrompt() {
        return """
                你是沃伦·巴菲特，奥马哈先知。请用你的价值投资原则分析股票：
                1. 关注具有持久竞争优势（护城河）的企业
                2. 寻找持续盈利增长且股本回报率高（>15%）的公司
                3. 偏好低负债、强自由现金流的企业
                4. 以合理价格买入——即使是优质企业也绝不高价追涨
                5. 以长期持有为目标（10年以上）

                请返回 JSON 格式：signal（bullish/bearish/neutral）、confidence（0-100）、reasoning（中文）。
                """;
    }
}
```

- [ ] **Step 2: 同样模式修改其余 12 个 investor agent**

对以下每个文件应用相同的改动（删除 MAPPER、client 字段、FinancialDatasetsClient 构造参数、buildAnalysisData 方法）：

- `BenGrahamAgent.java` — 构造：`(LlmClientFactory f, StructuredOutputHelper h)`，其余保留
- `BillAckmanAgent.java`
- `CathieWoodAgent.java`
- `CharlieMungerAgent.java`
- `MichaelBurryAgent.java`
- `MohnishPabraiAgent.java`
- `NassimTalebAgent.java`
- `PeterLynchAgent.java`
- `PhilFisherAgent.java`
- `RakeshJhunjhunwalaAgent.java`
- `StanleyDruckenmillerAgent.java`
- `AswathDamodaranAgent.java`

每个文件修改后的结构（以 BenGrahamAgent 为例）：

```java
package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import org.springframework.stereotype.Component;

/** 本杰明·格雷厄姆 — 价值投资之父，寻找安全边际 */
@Component
public class BenGrahamAgent extends BaseLlmAgent {

    public BenGrahamAgent(LlmClientFactory f, StructuredOutputHelper h) {
        super(f, h);
    }

    @Override public String getAgentId() { return "ben_graham"; }
    @Override public String getDisplayName() { return "本杰明·格雷厄姆"; }

    @Override
    protected String getSystemPrompt() {
        return """
                你是本杰明·格雷厄姆，价值投资之父。请以安全边际原则分析股票：
                1. 寻找低于内在价值的股票（市盈率<15，市净率<1.5）
                2. 要求强健的资产负债表：流动比率>2，长期负债低
                3. 10年以上持续盈利记录
                4. 优先考虑有分红历史的公司
                请返回 JSON 格式：signal（bullish/bearish/neutral）、confidence（0-100）、reasoning（中文）。
                """;
    }
}
```

> 其余 11 个 investor agent 应用相同结构：仅保留 `package`、`import`、`@Component`、类名、2 参构造器调用 `super(f, h)`、3 个重写方法。所有已存在的 `getSystemPrompt()` 内容保持不变。

- [ ] **Step 3: 修改 2 个 specialist agent**

`NewsSentimentAgent.java` 完整替换：

```java
package com.aihedgefund.agent.specialist;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import org.springframework.stereotype.Component;

/** 新闻情绪专项 Agent */
@Component
public class NewsSentimentAgent extends BaseLlmAgent {

    public NewsSentimentAgent(LlmClientFactory f, StructuredOutputHelper h) {
        super(f, h);
    }

    @Override public String getAgentId() { return "news_sentiment_analyst"; }
    @Override public String getDisplayName() { return "新闻情绪分析师"; }

    @Override
    protected String getSystemPrompt() {
        return """
                你是一位新闻情绪分析师。请分析近期新闻和内部人交易数据以评估市场情绪：
                1. 利好新闻（业绩超预期、新产品发布、战略合作）→ 看多
                2. 利空新闻（丑闻、诉讼、业绩不及预期）→ 看空
                3. 内部人大量卖出 → 看空信号
                4. 内部人大量买入 → 看多信号

                近期事件权重更高。请返回 JSON 格式：signal（bullish/bearish/neutral）、confidence（0-100）、reasoning（中文）。
                """;
    }
}
```

`ValuationAgent.java` 完整替换：

```java
package com.aihedgefund.agent.specialist;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import org.springframework.stereotype.Component;

/** 估值专项 Agent — 计算内在价值，生成交易信号 */
@Component
public class ValuationAgent extends BaseLlmAgent {

    public ValuationAgent(LlmClientFactory f, StructuredOutputHelper h) {
        super(f, h);
    }

    @Override public String getAgentId() { return "valuation_analyst"; }
    @Override public String getDisplayName() { return "估值分析师"; }

    @Override
    protected String getSystemPrompt() {
        return """
                你是一位定量估值分析师。请用多种方法计算内在价值：
                1. DCF：预测 5 年自由现金流并计算终值
                2. EV/EBITDA 乘数比较
                3. 市盈率与行业平均值对比

                判断股票是否高估（高于内在价值 20% 以上 → 看空）、
                低估（低于内在价值 20% 以上 → 看多），或估值合理。
                请返回 JSON 格式：signal（bullish/bearish/neutral）、confidence（0-100）、reasoning（中文，包含估值结论）。
                """;
    }
}
```

- [ ] **Step 4: 修改 PortfolioManagerAgent — 删除空的 buildAnalysisData()**

在 `src/main/java/com/aihedgefund/agent/portfolio/PortfolioManagerAgent.java` 中，删除这两行：

```java
    @Override
    protected String buildAnalysisData(AgentState state, String ticker) { return ""; }
```

保持 `getSystemPrompt()` 和 `analyze()` 不变。

- [ ] **Step 5: 全量编译验证**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "d:\workspace\ai-hedge\ai-hedge-fund-java"
mvn compile -q 2>&1 | Select-String "ERROR" | Select-Object -Last 5
```

预期：无输出（编译成功）

- [ ] **Step 6: 运行单元测试**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "d:\workspace\ai-hedge\ai-hedge-fund-java"
mvn test -DskipFrontend=true 2>&1 | Select-String "Tests run|BUILD" | Select-Object -Last 5
```

预期：所有测试通过，`BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/aihedgefund/agent/
git commit -m "refactor: remove buildAnalysisData() from all LLM agents, use tool calling via BaseLlmAgent"
```

---

## Task 4: 添加 MCP Server

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/aihedgefund/config/McpConfiguration.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 在 pom.xml 添加 MCP Server 依赖**

在 `spring-ai-ollama` 依赖之后，添加：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
</dependency>
```

- [ ] **Step 2: 验证依赖解析**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "d:\workspace\ai-hedge\ai-hedge-fund-java"
mvn dependency:resolve -q 2>&1 | Select-String "ERROR|BUILD" | Select-Object -Last 3
```

预期：`BUILD SUCCESS`

> 若报 `spring-ai-mcp-server-spring-boot-starter` 找不到，说明该模块在 1.0.0 BOM 中名称不同。
> 改用：`spring-ai-mcp-server-webmvc-spring-boot-starter`（HTTP/SSE 模式）或 `spring-ai-mcp`，参考当时 BOM 内容：
> ```powershell
> mvn dependency:resolve -q 2>&1 | Select-String "spring-ai-mcp"
> ```

- [ ] **Step 3: 创建 McpConfiguration.java**

```java
package com.aihedgefund.config;

import com.aihedgefund.llm.FinancialDataTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server 配置 — 将 FinancialDataTools 的 @Tool 方法注册为 MCP 工具，
 * 供外部 MCP 客户端（Claude Desktop、Cursor 等）通过 SSE 连接使用。
 */
@Configuration
public class McpConfiguration {

    @Bean
    public ToolCallbackProvider financialDataToolProvider(FinancialDataTools financialDataTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(financialDataTools)
                .build();
    }
}
```

> 若 `MethodToolCallbackProvider` 找不到，检查实际包名：
> ```java
> import org.springframework.ai.mcp.server.autoconfigure.ToolCallbackProvider; // 备选
> ```

- [ ] **Step 4: 在 application.yml 添加 MCP Server 配置**

在 `hedge-fund:` 块之前插入：

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        name: ai-hedge-fund-financial-tools
        version: 1.0.0
```

注意与已有的 `spring.datasource` 等合并在同一个 `spring:` 块下，不要重复写 `spring:`。

- [ ] **Step 5: 启动应用验证 MCP 端点可达**

启动应用（后台）：

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "d:\workspace\ai-hedge\ai-hedge-fund-java"
mvn spring-boot:run 2>&1 | Select-String "Started|ERROR|mcp" | Select-Object -First 10
```

预期日志：`Started AiHedgeFundApplication`，无 `ERROR`

检查 MCP 端点（新终端）：

```powershell
# MCP SSE 端点（应返回 SSE stream 或 200）
Invoke-WebRequest -Uri "http://localhost:8000/sse" -Method GET -TimeoutSec 3 2>&1 | Select-Object StatusCode
```

预期：`StatusCode: 200`（可能因 SSE 长连接而 timeout，200 即可）

> 若端点路径不同，查看启动日志中 `mcp` 或 `sse` 关键字找到实际路径。

- [ ] **Step 6: Commit**

```bash
git add pom.xml \
        src/main/java/com/aihedgefund/config/McpConfiguration.java \
        src/main/resources/application.yml
git commit -m "feat: add Spring AI MCP Server, expose FinancialDataTools as MCP tools"
```

---

## Task 5: 全量测试与最终验证

**Files:** 无文件改动，仅验证

- [ ] **Step 1: 运行全量测试**

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "d:\workspace\ai-hedge\ai-hedge-fund-java"
mvn test -DskipFrontend=true 2>&1 | Select-String "Tests run|FAIL|BUILD" | Select-Object -Last 8
```

预期：所有测试通过，`BUILD SUCCESS`

- [ ] **Step 2: 常见问题处理**

**若 `HedgeFundOrchestratorTest` 失败**（PortfolioManagerAgent 构造器报错）：
- 检查 `PortfolioManagerAgent` 构造器是否还有 `buildAnalysisData` 相关问题
- 确认 Task 3 Step 4 中的删除已完成

**若工具调用 @Tool 注解找不到**（`Cannot find symbol: Tool`）：
```java
// 检查 spring-ai-core 中的正确包名
import org.springframework.ai.tool.annotation.Tool;         // Spring AI 1.0.0 标准
// 若找不到，尝试：
import org.springframework.ai.model.tool.ToolAnnotationUtils; // 备选
```

**若 `ChatClient.prompt().tools()` 方法不存在**：
- Spring AI 1.0.0 的 `tools()` 方法接受 `Object...` 参数
- 确认 `spring-ai-core` 已作为传递依赖存在：`mvn dependency:tree | grep spring-ai-core`

- [ ] **Step 3: 验证应用功能正常（手动）**

启动服务并通过前端发起一次分析请求（选 1 个分析师，1 个股票）：

```
http://localhost:5173/#/run
股票：AAPL
分析师：沃伦·巴菲特
```

观察后端日志，预期看到：
```
INFO  [BaseLlmAgent] LLM 分析（工具调用模式）, ticker=AAPL
DEBUG [FinancialDataTools] [Tool] getFinancialMetrics: ticker=AAPL, period=ttm ...
DEBUG [BaseLlmAgent] ticker=AAPL LLM 原始响应: {"signal":"..."}
```

若日志只有 `LLM 分析（工具调用模式）` 但没有 `[Tool]` 日志，说明模型不支持工具调用，会直接返回文本响应。此为预期的降级行为（仍能返回 signal，只是没有数据支撑）。
