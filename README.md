# 金木班 — AI 对冲基金分析平台

AI 对冲基金分析平台的 Java 实现，汇聚 21 位投资大师/分析师 Agent 并行分析，配合产业瓶颈反向拆解与逆向对立面研究框架，支持实时 SSE 推流与增量信号推送。

> **仅供学习和研究，不构成投资建议，不执行真实交易。**

---

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 17、Spring Boot 3.2、MyBatis 3、SQLite |
| AI | LangChain4j 0.36.2 + Spring AI（OpenAI / Anthropic / Groq / Ollama / 讯飞 MaaS） |
| 前端 | Vue 3、Vite 5、Element Plus、Pinia |
| 构建 | Maven 3、frontend-maven-plugin |

---

## 前置条件

- **JDK 17+**
- **Maven 3.6+**
- **Node.js 18+**（开发模式下需要；生产打包由 Maven 自动下载 Node v20.11.0）

Windows 推荐使用 IntelliJ 捆绑的 JDK：

```powershell
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH      = "$env:JAVA_HOME\bin;$env:PATH"
```

---

## 配置 API Key

编辑 `src/main/resources/application.yml`，填写所需的 API Key：

```yaml
financial-datasets:
  api-key: YOUR_FINANCIAL_DATASETS_API_KEY   # 金融数据（必填）

openai:
  api-key: YOUR_OPENAI_API_KEY               # 至少填一个 LLM
  base-url: https://api.openai.com           # 可替换为兼容端点（如讯飞 MaaS）

anthropic:
  api-key: ${ANTHROPIC_API_KEY:}             # 可选

groq:
  api-key: ${GROQ_API_KEY:}                 # 可选

ollama:
  base-url: http://localhost:11434           # 本地 Ollama（可选）
```

也可通过页面内 **API Key 管理** 在运行时动态配置。

---

## 启动方式

### 方式一：开发模式（推荐，支持前端热更新）

需要开两个终端：

**终端 1 — 启动 Java 后端（端口 8000）**

```powershell
mvn spring-boot:run
```

**终端 2 — 启动 Vue 前端开发服务器（端口 5173）**

```powershell
cd frontend
npm install   # 首次运行需要
npm run dev
```

打开浏览器访问：**http://localhost:5173**

修改 `frontend/src/` 下的 Vue 代码后，页面自动热更新，无需重启。

---

### 方式二：生产模式（单进程，前后端合一）

一条命令构建并打包：

```powershell
mvn package -DskipTests
```

Maven 会自动执行 `npm run build`，将 Vue 产物内嵌到 jar 包中。

启动 jar：

```powershell
java -jar target/ai-hedge-fund-java-1.0.0-SNAPSHOT.jar
```

打开浏览器访问：**http://localhost:8000**

---

## 页面功能

| 路由 | 功能 |
|------|------|
| `/#/run` | **大师分析**：填写股票代码、选择 LLM 和分析师，实时查看 21 位分析师进度及最终交易决策 |
| `/#/industry-analysis` | **产业分析**：输入一句话产业/标的描述，使用「行业瓶颈反向拆解选股法」五步框架自动研究并展示 Markdown 报告 |
| `/#/contrarian-analysis` | **逆向对立面分析**：输入一句话热门行业/分析需求，使用「热门行业逆向对立面价值标的挖掘」五步框架自动研究并展示 Markdown 报告 |
| `/#/api-keys` | **API Key 管理**：新增、查看、删除各 Provider 的 API Key |
| `/#/flows` | **流程管理**：保存和管理常用分析配置，一键跳转运行 |
| `/#/settings` | **设置**：查看可用 LLM 模型列表及后端健康状态 |
| `/#/wallet` | **我的钱包**：查看金币余额和流水记录 |
| `/#/contact` | **联系我们**：联系方式和平台介绍 |

---

## 用户认证

系统内置 JWT 用户体系，所有 API（除认证接口外）需携带 `Authorization: Bearer <token>` 请求头。

### 注册

注册用户名必须是邮箱格式，注册前需先获取邮箱验证码：

```bash
# 1. 发送验证码（60 秒内仅可发送一次）
curl -X POST http://localhost:8000/auth/send-code \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com"}'

# 2. 完成注册
curl -X POST http://localhost:8000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "user@example.com", "password": "your_password", "nickname": "昵称", "code": "123456"}'
```

未配置邮件服务时，验证码会以 `WARN` 级别打印到后端日志（开发模式下可直接查看）。

### 登录

```bash
curl -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user@example.com", "password": "your_password"}'
```

返回 `token` 字段，后续请求带在 `Authorization` 头中。

### 找回密码

忘记密码时，可通过邮箱验证码自行重置密码：

```bash
# 1. 发送重置密码验证码（需为已注册邮箱）
curl -X POST http://localhost:8000/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com"}'

# 2. 使用验证码重置密码
curl -X POST http://localhost:8000/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "code": "123456", "newPassword": "new_password", "confirmPassword": "new_password"}'
```

重置成功后请使用新密码重新登录。

---

## 金币系统

每次 AI 分析消耗 **1 枚金币**，余额不足时返回 `402 Payment Required`。
新用户注册成功后自动赠送 **3 枚金币**，后续金币由管理员通过后台 API 发放。

### 发放金币（管理员操作）

```bash
# 按用户 ID 发放金币（需要 X-Admin-Token 请求头）
curl -X POST http://localhost:8000/admin/coins/grant \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: mumu-admin-2024" \
  -d '{"userId": 1, "amount": 10, "reason": "初始金币"}'

# 按注册邮箱发放金币（userId 与 email 二选一）
curl -X POST http://localhost:8000/admin/coins/grant \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: mumu-admin-2024" \
  -d '{"email": "user@example.com", "amount": 10, "reason": "初始金币"}'
```

> Windows CMD 下 JSON 双引号需用 `{\"...\"}` 转义：
>
> ```cmd
> curl -X POST http://localhost:8000/admin/coins/grant -H "X-Admin-Token: mumu-admin-2024" -H "Content-Type: application/json" -d "{\"email\":\"user@example.com\",\"amount\":10,\"reason\":\"初始金币\"}"
> ```
>
> Windows PowerShell 下推荐使用 `Invoke-RestMethod`：
>
> ```powershell
> Invoke-RestMethod -Uri "http://localhost:8000/admin/coins/grant" -Method Post `
>   -Headers @{ "X-Admin-Token" = "mumu-admin-2024" } -ContentType "application/json" `
>   -Body '{"email": "user@example.com", "amount": 10, "reason": "初始金币"}'
> ```

### 查询所有用户余额

```bash
curl http://localhost:8000/admin/users/balances \
  -H "X-Admin-Token: mumu-admin-2024"
```

### 为老用户初始化钱包

注册时间早于钱包功能上线的老用户，需先在数据库初始化钱包记录，再发放金币：

```bash
sqlite3 ai-hedge-fund.db "
  INSERT OR IGNORE INTO user_wallets (user_id, balance, created_at, updated_at)
    SELECT id, 0, datetime('now'), datetime('now') FROM users;
"
```

### 自定义管理员 Token

默认 Token 为 `mumu-admin-2024`，通过环境变量覆盖：

```bash
ADMIN_TOKEN=your-secret-token mvn spring-boot:run
```

### 钱包相关 API（用户侧）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/wallet/balance` | 查询当前用户金币余额 |
| `GET` | `/wallet/transactions` | 查询金币流水（分页，`pageNum` / `pageSize`） |

---

## 后端 API

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/auth/send-code` | 发送邮箱验证码（注册用） |
| `POST` | `/auth/register` | 注册（需验证码） |
| `POST` | `/auth/login` | 登录，返回 JWT token |
| `GET` | `/auth/me` | 获取当前用户信息 |
| `POST` | `/auth/forgot-password` | 发送密码重置验证码 |
| `POST` | `/auth/reset-password` | 使用验证码重置密码 |
| `POST` | `/hedge-fund/run` | 运行大师分析（SSE 流式响应，消耗 1 金币） |
| `POST` | `/industry-analysis/run` | 产业瓶颈反向拆解分析（SSE 流式响应，消耗 1 金币） |
| `POST` | `/contrarian-analysis/run` | 热门行业逆向对立面分析（SSE 流式响应，消耗 1 金币） |
| `GET` | `/wallet/balance` | 查询金币余额 |
| `GET` | `/wallet/transactions` | 金币流水（分页） |
| `POST` | `/admin/coins/grant` | 管理员发放金币 |
| `GET` | `/admin/users/balances` | 管理员查询所有用户余额 |
| `GET` | `/api-keys` | 列出所有 API Key |
| `POST` | `/api-keys` | 新增或更新 API Key |
| `DELETE` | `/api-keys/{provider}` | 删除 API Key |
| `GET` | `/flows` | 列出所有流程 |
| `POST` | `/flows` | 新建流程 |
| `PUT` | `/flows/{id}` | 更新流程 |
| `DELETE` | `/flows/{id}` | 删除流程 |
| `GET` | `/flow-runs/{id}` | 查询运行记录 |
| `GET` | `/analysis-runs` | 分析记录列表（分页） |
| `GET` | `/analysis-runs/{id}` | 分析记录详情 |
| `GET` | `/language-models` | 可用模型列表 |
| `GET` | `/health` | 健康检查 |

> `/hedge-fund/run` 为 SSE 长连接，单次请求最长等待时长由 `hedge-fund.sse-timeout-ms` 配置
> （默认 `30000000` 毫秒 ≈ 500 分钟），也可用环境变量 `HEDGE_FUND_SSE_TIMEOUT_MS` 覆盖。

**SSE 事件类型：**

| 事件 | 时机 | data |
|------|------|------|
| `start` | 请求开始 | `status`、`tickers` |
| `progress` | 各 agent 进度变化 | `agent`、`ticker`、`status`（analyzing/done/error） |
| `activity` | agent 分析过程中的细粒度活动 | `agent`、`agent_id`、`message`（如「调用工具 getDaily 获取数据…」「调用大模型推理中…」「命中缓存：fina_indicator」） |
| `signal` | 单个 agent 分析完成即推送 | `agent`、`agent_id`、`signals`（`ticker → {signal, confidence, reasoning}`） |
| `complete` | 全部完成 | `analyst_signals`、`decisions`（最终汇总）、`status` |
| `error` | 执行失败 | `message` |

> `activity` 事件解决「单个 agent 分析时间过长」时的体感问题：分析过程中实时推送「调用工具/获取数据/命中缓存/推理中」等
> 简短进度，前端在该分析师行下以橙色 ⏳ 行展示最新一条，分析完成（done）后自动清除。
>
> `signal` 事件实现**增量展示**：某个分析师一算完，其信号立即推给前端渲染，无需等待全部分析师；
> 最终的组合决策（汇总）仍在 `complete` 事件统一下发。

---

## Agent 体系（21 位）

### 投资大师（13 位）

| Agent ID | 人格 |
|----------|------|
| `warren_buffett` | 巴菲特 — 价值投资、护城河 |
| `charlie_munger` | 芒格 — 安全边际、品质优先 |
| `ben_graham` | 格雷厄姆 — 深度价值、净流动资产 |
| `phil_fisher` | 费雪 — 成长投资、闲聊法 |
| `peter_lynch` | 林奇 — 自下而上、PEG |
| `cathie_wood` |木头 — 破坏性创新、ARK |
| `bill_ackman` | 阿克曼 — 集中押注、激进价值 |
| `michael_burry` | 布里 — 逆向深价值、危机嗅觉 |
| `stanley_druckenmiller` | 德鲁肯米勒 — 宏观趋势、不对称风险 |
| `rakesh_jhunjhunwala` | 金瓦拉 — 印度成长价值 |
| `nassim_taleb` | 塔勒布 — 黑天鹅、尾部风险 |
| `mohnish_pabrai` | 帕布莱 — 克隆投资、低风险高回报 |
| `aswath_damodaran` | 达摩达兰 — DCF 估值、企业叙事 |

### 专项分析师（4 位）

| Agent ID | 职责 |
|----------|------|
| `valuation_analyst` | 估值分析（PE/PB/EV/DCF） |
| `news_sentiment_analyst` | 新闻情绪分析 |
| `industry_analysis` | 产业瓶颈反向拆解 |
| `contrarian_analysis` | 逆向对立面研究 |

### 风控 + 组合（2 位）

| Agent ID | 职责 |
|----------|------|
| `risk_manager` | 风险管理 |
| `portfolio_manager` | 组合决策（质量价值三重门槛） |

---

## 组合决策：质量价值三重门槛

`PortfolioManagerAgent` 不做简单的多数表决，而是采用 **「必过门槛（veto gate）」** 逻辑，
专门用于筛选 **低估值 + 高护城河 + 强 DCF 现金流折现** 的优质公司。

三个门槛组（按 `agentId` 划分），均为必过项：

| 门槛组 | 含义 | 包含的分析师 Agent |
|--------|------|--------------------|
| 护城河组 | 持久竞争优势 + 负责任的管理层 | `warren_buffett`、`charlie_munger` |
| 低估值组 | 安全边际 / 价格折让 | `ben_graham`、`mohnish_pabrai`、`michael_burry`、`valuation_analyst` |
| DCF 内在价值组 | 未来现金流折现能力 | `aswath_damodaran`、`valuation_analyst` |

**每组判定规则（严格否决）：**

- 缺组（未选该组任何分析师，组内无信号）→ 不过
- 组内出现 **任一看空（bearish）信号** → 否决，不过
- 组内无任何看多（bullish）信号（全中性）→ 未确认，不过
- 组内有看多且无看空 → 通过

三组 **全部通过** 才给出 `buy`；任一组未过即淘汰为 `hold`。

> **用法提示：** 要让门槛真正生效，运行时 `selected_analysts` 至少要覆盖三组各一个 Agent，
> 否则缺失的组会直接判不过（全部 `hold`）。推荐组合：
> `warren_buffett, charlie_munger, ben_graham, mohnish_pabrai, aswath_damodaran, valuation_analyst`。
>
> 前端 `/#/run` 页面「分析师」选择器旁提供 **「质量价值组合」一键预设按钮**，
> 点击即自动选中上述 6 位分析师。

**单个 ticker 的决策响应字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `action` | string | `buy`（三组全过）/ `hold`（任一组未过） |
| `quantity` | int | 买入数量；`hold` 时为 0 |
| `confidence` | int | 全部参与分析师的平均置信度 |
| `gate_passed` | boolean | 三组门槛是否全部通过 |
| `gate_detail` | string[] | 各组明细 |
| `bull_count` / `bear_count` | int | 该 ticker 的总看多 / 看空信号数 |
| `reasoning` | string | 决策说明（含门槛汇总） |

---

## 产业瓶颈反向拆解分析

`POST /industry-analysis/run` 接受用户输入的一句话产业/标的描述（及附加筛选条件，如 PE 上限、市值区间），
由 `IndustryBottleneckResearchAgent` 调用大模型 + 网络搜索等工具，
按「行业瓶颈反向拆解选股法」五步框架（逆向拆解瓶颈 → 锁定非对称标的 → 穿透财务拐点 → AI 红队测试 → 设定熔断机制）
完成研究，以 SSE 流式返回分析进度和最终 Markdown 报告。

请求开始时扣减用户 1 个金币（余额不足返回 402），并创建一条「分析记录」（状态 `RUNNING`），
分析完成后回填 Markdown 报告（状态 `COMPLETE`），失败则记录错误信息（状态 `ERROR`）。

**请求体：**

```json
{
  "query": "分析AI产业，找到PE在100以内的标的",
  "model_name": "astron-code-latest",
  "model_provider": "OpenAI"
}
```

**SSE 事件类型：**

| 事件 | 时机 | data |
|------|------|------|
| `start` | 请求开始 | `status`、`query` |
| `activity` | 分析过程中的活动进度 | `message` |
| `complete` | 分析完成 | `report`（Markdown 格式五段式报告）、`status` |
| `error` | 执行失败 | `message` |

系统提示词外置于 `src/main/resources/prompts/industry_bottleneck_research.md`，
通过 `PromptLoader` 加载，修改框架内容无需改动代码。

---

## 热门行业逆向对立面分析

`POST /contrarian-analysis/run` 接受用户输入的一句话热门行业/分析需求，
由 `ContrarianSectorResearchAgent` 调用大模型 + 网络搜索等工具，
按「通用热门行业逆向对立面价值标的挖掘」五步框架
（量化判定识别热门行业 → 四大维度推导对立面赛道 → 对立面三层漏斗标的筛选 → 标的统一量化打分模型 → 风控体系+仓位约束）
完成研究，以 SSE 流式返回分析进度和最终 Markdown 报告。

请求开始时扣减用户 1 个金币（余额不足返回 402），并创建一条「分析记录」（状态 `RUNNING`），
分析完成后回填 Markdown 报告（状态 `COMPLETE`），失败则记录错误信息（状态 `ERROR`）。

**请求体：**

```json
{
  "query": "分析新能源行业的逆向对立面标的",
  "model_name": "astron-code-latest",
  "model_provider": "OpenAI"
}
```

**SSE 事件类型：**

| 事件 | 时机 | data |
|------|------|------|
| `start` | 请求开始 | `status`、`query` |
| `activity` | 分析过程中的活动进度 | `message` |
| `complete` | 分析完成 | `report`（Markdown 格式五段式报告）、`status` |
| `error` | 执行失败 | `message` |

系统提示词外置于 `src/main/resources/prompts/contrarian_sector_research.md`，
通过 `PromptLoader` 加载，修改框架内容无需改动代码。

前端 `complete` 事件返回的 `report` 为 Markdown 文本，页面通过 `marked` + `DOMPurify`
渲染为带样式的 HTML，并对渲染结果做 XSS 净化。

---

## 数据工具与缓存

### TushareDataTools（A股金融数据）

暴露 5 个金融数据工具（`@Tool`），既供 LLM Agent 主动调用，也作为 MCP Server 工具对外暴露：

| 工具 | Tushare 接口 | 用途 |
|------|--------------|------|
| `getDaily` | `daily` | A股日线行情（OHLCV） |
| `getFinaIndicator` | `fina_indicator` | 财务指标（PE/PB/ROE 等） |
| `getIncome` | `income` | 利润表 |
| `getStockBasic` | `stock_basic` | 全市场股票列表 |
| `getIndexDaily` | `index_daily` | 指数日线行情 |

**全路径结果缓存**：每个工具方法内置缓存（复用 `ToolCallCacheService` / `tool_call_cache` 表），
对 LLM 工具调用、MCP 外部调用、直接 Java 调用三条路径均生效：

1. 用调用参数构造确定性缓存键（参数按 key 排序）
2. 先查缓存 → 命中直接返回，跳过 Tushare API
3. 未命中 → 请求 API → 结果非空才写回缓存并返回
4. 缓存命名空间前缀 `tushare.`，与 LLM 工具调用层缓存键隔离
5. 缓存读写异常自动降级（仅告警，绝不影响取数）

> TTL 由 `hedge-fund.tool-cache-ttl` 配置（默认 3600s），启动时自动清理过期条目。

### WebSearchTools（网络搜索）

让 Agent 具备联网搜索能力，检索金融数据接口里没有的实时信息。

| 工具 | 参数 | 用途 |
|------|------|------|
| `webSearch` | `query`（必填）、`maxResults`（选填，默认 5，最大 10） | 互联网搜索，返回标题/链接/摘要 |

**可插拔的搜索服务商**：

| provider | 服务商 | 鉴权 | API Key 配置 |
|----------|--------|------|--------------|
| `tavily` | Tavily（AI 搜索，默认） | body `api_key` | `TAVILY_API_KEY` |
| `serper` | Serper.dev（Google） | header `X-API-KEY` | `SERPER_API_KEY` |
| `bocha` | 博查（国内中文搜索） | header `Bearer` | `BOCHA_API_KEY` |

配置示例：

```yaml
web-search:
  provider: ${WEB_SEARCH_PROVIDER:tavily}
```

- 结果带缓存（命名空间 `websearch.{provider}`）
- 切换服务商只改 `web-search.provider`，无需改代码

---

## 系统提示词外置

Agent 的系统提示词支持从代码外置到 classpath 下的 Markdown 文件，便于调整措辞、做 A/B 实验，无需改代码重编译。

- 提示词文件：`src/main/resources/prompts/{name}.md`
- 加载器：`PromptLoader.load(name)` — 读取并按文件名缓存
- 加载时机：Agent Bean 实例化（系统启动）时通过构造器加载
- 文件缺失：抛出 `IllegalStateException`，启动即失败

---

## 常用命令

```powershell
# 编译
mvn clean compile

# 运行所有测试
mvn test

# 运行指定测试
mvn test -Dtest=HedgeFundControllerTest

# 运行前端单元测试
cd frontend && npm test

# 打包（含前端构建）
mvn package -DskipTests

# 跳过前端构建（仅后端）
mvn package -DskipTests -Dfrontend.skip=true
```

---

## 项目结构

```
master_ai/
├── frontend/                   # Vue 3 前端源码
│   ├── src/
│   │   ├── api/index.js        # Axios API 封装
│   │   ├── stores/             # Pinia 状态（auth/run/settings/wallet/industry/contrarian）
│   │   └── views/              # 页面组件（Run/Industry/Contrarian/ApiKeys/Flows/Settings/Wallet/Login/Register/Contact/History）
│   └── vite.config.js
├── src/main/java/com/aihedgefund/
│   ├── agent/                  # 21 位 Agent（13 投资大师 + 4 专项分析师 + 风控 + 组合）
│   ├── controller/             # REST/SSE 接口
│   ├── orchestrator/           # 并行工作流（HedgeFundOrchestrator）
│   ├── service/                # 业务逻辑（Auth/Wallet/Admin/Email/Cache）
│   ├── mapper/                 # MyBatis Mapper
│   ├── auth/                   # JWT 鉴权（JwtUtil + AuthInterceptor）
│   └── llm/                    # LLM 客户端工厂（LangChain4j）
├── src/main/resources/
│   ├── application.yml         # 运行配置
│   ├── application-template.yml # 配置模板（不含真实密钥）
│   ├── prompts/                # 21 个 Agent 系统提示词（Markdown）
│   └── db/schema.sql           # SQLite 表结构（启动时自动初始化）
├── ai-hedge-fund.db            # SQLite 数据库文件（自动创建）
├── ai_anly.md                  # 示例分析报告（ABF载板/液冷散热/OLTC）
└── pom.xml
```

---

## 数据存储

使用 SQLite，数据库文件为项目根目录下的 `ai-hedge-fund.db`，首次启动自动创建。无需额外安装数据库。

**核心表：**

| 表 | 用途 |
|----|------|
| `users` | 用户账号（邮箱 + 密码 + 昵称） |
| `user_wallets` | 金币余额 |
| `coin_transactions` | 金币流水 |
| `api_keys` | 用户动态配置的 API Key |
| `flows` | 保存的分析流程配置 |
| `analysis_runs` | 分析运行记录（个股/产业/逆向共用） |
| `tool_call_cache` | 工具调用结果缓存 |