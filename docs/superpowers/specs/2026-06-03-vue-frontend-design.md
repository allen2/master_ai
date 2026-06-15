# Vue 前端设计文档

**日期**: 2026-06-03  
**项目**: ai-hedge-fund-java  
**状态**: 已批准，待实现

---

## 1. 概述

为 `ai-hedge-fund-java` Spring Boot 项目开发一套 Vue 3 前端，覆盖所有现有 REST/SSE 接口，提供完整的操作页面。前端随 Java 项目一同打包和启动。

---

## 2. 技术栈

| 层级 | 选型 |
|------|------|
| 框架 | Vue 3 + Composition API |
| 构建 | Vite 5 |
| 路由 | Vue Router 4 |
| 状态 | Pinia |
| HTTP | Axios（REST）+ 原生 `EventSource`（SSE） |
| UI 组件 | Element Plus |
| 构建集成 | `frontend-maven-plugin`（`mvn package` 时自动构建） |

---

## 3. 目录结构

前端源码放在 Java 项目根目录下的 `frontend/` 子目录：

```
ai-hedge-fund-java/
├── frontend/                  ← Vue 项目根
│   ├── src/
│   │   ├── main.js
│   │   ├── App.vue            ← 根组件（侧边栏 + router-view）
│   │   ├── router/index.js
│   │   ├── stores/            ← Pinia stores
│   │   │   ├── runStore.js    ← 分析运行状态（SSE 进度、结果）
│   │   │   └── settingsStore.js
│   │   ├── api/               ← Axios 封装
│   │   │   └── index.js
│   │   └── views/
│   │       ├── RunView.vue         ← 分析运行
│   │       ├── ApiKeysView.vue     ← API Key 管理
│   │       ├── FlowsView.vue       ← 流程管理
│   │       └── SettingsView.vue    ← 设置（模型列表 + 健康状态）
│   ├── index.html
│   ├── vite.config.js
│   └── package.json
└── src/main/resources/
    └── static/                ← mvn package 后 Vue 产物打入此处
```

---

## 4. UI 设计

### 4.1 全局布局

- **文字侧边栏**（宽 160px）+ 右侧内容区
- **浅色主题**：白色背景，蓝色（`#2563eb`）主色，`#f8fafc` 页面背景
- 侧边栏底部显示后端健康状态小圆点（绿/红）
- 路由切换时侧边栏高亮当前页

### 4.2 分析运行页（`/run`）

**布局：上下结构**

- **顶部配置栏**（一行，固定高度）：
  - 股票代码输入（逗号分隔，如 `AAPL, MSFT`）
  - 日期范围选择（开始/结束，默认近 3 个月）
  - LLM 模型下拉（从 `GET /language-models` 获取）
  - 分析师多选下拉（19 位，默认全选）
  - "▶ 开始分析"按钮，运行中变为"⏹ 停止"

- **下方结果区**（两列）：
  - **左列：分析师进度列表**  
    每行显示：状态圆点（等待/进行中/完成）、分析师名、信号徽章（bullish/bearish/neutral + 置信度%）  
    SSE `progress` 事件实时更新状态；`complete` 事件填充最终信号
  - **右列：交易决策**  
    每个 ticker 一张卡片：决策标签（BUY/SELL/HOLD）、bull/bear/neutral 信号比例条、数量和推理文字

- SSE 连接通过 `EventSource` 实现；`start` 事件清空上次结果；`error` 事件显示错误横幅；`complete` 事件标记完成

### 4.3 API Keys 页（`/api-keys`）

- 顶部"+ 添加 Key"按钮打开对话框（Provider 下拉 + Key 值输入）
- 表格列：Provider、状态（启用/禁用徽章）、最后使用时间、创建时间、操作（删除）
- 删除前二次确认弹窗

### 4.4 流程管理页（`/flows`）

- 顶部"+ 新建流程"按钮打开对话框（名称、描述）
- 流程卡片列表，每卡显示：名称、描述、最后运行时间
- 每卡操作：**运行**（跳转到运行页并预填配置）、**编辑**（内联或弹窗）、**删除**
- 点击流程可查看最近一次 FlowRun 详情（状态、结果 JSON）

### 4.5 设置页（`/settings`）

- 可用 LLM 模型只读表格（`GET /language-models`）
- 后端健康状态卡片（`GET /health`，每 30 秒轮询一次）

---

## 5. API 对接

| 后端接口 | 前端页面 | 方式 |
|----------|----------|------|
| `POST /hedge-fund/run` | 分析运行 | SSE（EventSource） |
| `GET /api-keys` | API Keys | Axios GET |
| `POST /api-keys` | API Keys | Axios POST |
| `DELETE /api-keys/{provider}` | API Keys | Axios DELETE |
| `GET /flows` | 流程管理 | Axios GET |
| `GET /flows/{id}` | 流程管理 | Axios GET |
| `POST /flows` | 流程管理 | Axios POST |
| `PUT /flows/{id}` | 流程管理 | Axios PUT |
| `DELETE /flows/{id}` | 流程管理 | Axios DELETE |
| `GET /flow-runs/{id}` | 流程管理 | Axios GET |
| `GET /language-models` | 设置 / 运行配置 | Axios GET |
| `GET /health` | 设置 / 侧边栏 | Axios GET（轮询） |

开发时 Vite 配置 `proxy`，将 `/api` → `http://localhost:8000`，避免 CORS 问题。生产时直接访问同源 `http://localhost:8000`。

---

## 6. 构建集成

### 6.1 开发模式

```bash
# 终端 1：启动 Java 后端（端口 8000）
mvn spring-boot:run

# 终端 2：启动 Vue 开发服务器（端口 5173，热更新）
cd frontend && npm run dev
# 访问 http://localhost:5173
```

`vite.config.js` 中按路径显式代理后端接口：
```js
server: {
  proxy: {
    '/hedge-fund': 'http://localhost:8000',
    '/api-keys':   'http://localhost:8000',
    '/flows':      'http://localhost:8000',
    '/flow-runs':  'http://localhost:8000',
    '/language-models': 'http://localhost:8000',
    '/health':     'http://localhost:8000',
  }
}
```

### 6.2 生产打包

`pom.xml` 加入 `frontend-maven-plugin`，绑定到 `generate-resources` 阶段：

```
mvn package  →  自动执行 npm install + npm run build
              →  产物复制到 src/main/resources/static/
              →  Spring Boot 伺服 http://localhost:8000
```

Spring Boot 已有 `CorsConfig`，开发时允许 5173 跨域，生产时同源无需 CORS。

---

## 7. 错误处理

- API 请求失败：Element Plus `ElMessage.error()` 全局提示
- SSE 断开/错误：横幅提示 + 按钮恢复为可点击状态
- 表单校验：Element Plus 内置表单校验（股票代码非空、日期格式）

---

## 8. 测试范围

- **UT**：Pinia store 逻辑（SSE 事件处理、状态流转）使用 Vitest
- **FT**：核心操作路径手动验证（运行分析、添加/删除 API Key、新建/删除流程）
- 本文档不新增后端测试；现有 Java 测试已覆盖 API 层

---

## 9. 不在范围内

- React Flow 节点图编辑器（流程管理仅做列表+CRUD，不做可视化画布）
- 用户认证/登录
- 回测功能前端（后端尚无对应 REST 接口）
- i18n 国际化
