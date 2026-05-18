# knowledge-agent-platform 项目深度技术分析报告

> **分析日期**: 2026-05-15
> **分析协议**: deep-teach-review v1.0.0 (SKILL_REVIEW.md)
> **分析范围**: 项目全景（技术栈 + 架构 + 选型决策）
> **项目版本**: 0.0.1-SNAPSHOT

---

## 项目概述

**knowledge-agent-platform** 是一个基于 **Agent Harness 架构** 的知识问答与工具调用平台。核心技术栈为 Spring Boot 3.2.5 + Java 17 + Spring AI 1.1.4，支持 RAG 问答、多 Agent 编排、MCP 协议工具暴露、JWT 用户认证与数据隔离。

---

## 技术栈全景图

| 层级 | 技术 | 版本 | 角色 |
|------|------|------|------|
| **语言** | Java | 17 | 开发语言 |
| **核心框架** | Spring Boot | 3.2.5 | IoC / Web / 数据 / 配置管理 |
| **AI 框架** | Spring AI | 1.1.4 | LLM 统一调用 + 向量检索抽象 |
| **LLM 提供商** | DeepSeek | deepseek-chat | 实际调用的 AI 模型 |
| **向量数据库** | pgvector (PostgreSQL) | — | 向量存储 + HNSW ANN 检索 |
| **关系数据库** | H2 (file mode) | — | 结构化数据持久化（默认开发库） |
| **ORM** | Spring Data JPA + Hibernate | — | 数据访问层 |
| **文档解析** | PDFBox / POI | 2.0.30 / 5.2.5 | PDF / Word 文本提取 |
| **认证** | JJWT | 0.12.5 | JWT Token 签发与验证 |
| **加密** | Spring Security Crypto | — | 密码 BCrypt 哈希 |
| **协议** | MCP Server (WebMVC) | 1.0.0 | 对外暴露工具能力 |
| **监控** | Micrometer + Prometheus | — | 指标采集与暴露 |
| **API 文档** | springdoc-openapi | 2.5.0 | Swagger UI |
| **架构模式** | Agent Harness | — | 多 Agent 编排流水线 |

### 项目模块结构（17 个包 / 70 个 Java 文件）

`
com.liujianan.agentdemo
├── agent/          (2 files)  Agent 管理
├── ai/             (1 file)   Spring AI 模型客户端
├── auth/           (8 files)  认证 + JWT + 用户
├── chat/           (4 files)  对话请求/响应
├── common/         (4 files)  全局配置 / 异常处理 / 指标
├── evaluation/     (8 files)  质量评测
├── harness/        (13 files) ★ 核心编排引擎
├── home/           (1 file)   首页
├── knowledge/      (6 files)  知识库管理
├── llm/            (2 files)  LLM 接口与 HTTP 直连实现
├── mcp/            (1 file)   MCP 工具服务
├── skill/          (2 files)  Skill 管理
└── tool/           (5 files)  工具注册与调用
`

### Skill 与 Agent 配置文件

`
resources/
├── agents/
│   ├── answer-agent.md
│   ├── qa-agent.md
│   ├── retrieval-agent.md
│   └── tool-agent.md
├── skills/
│   ├── document-ingestion/SKILL.md
│   ├── evaluation/SKILL.md
│   ├── rag-answer/SKILL.md
│   ├── tool-calling/SKILL.md
│   └── trace-review/SKILL.md
└── application.yml
`

---


---

## 🎯 Teaching Card 1: Agent Harness 多 Agent 编排架构

> **卡片类型**: 标准 Teaching Card
> **分析对象**: Harness 多 Agent 编排流水线 — 项目的核心架构模式

📁 **对应源码**:
- harness/HarnessOrchestrator.java — 中央编排器
- harness/RetrievalAgent.java — 检索代理
- harness/AnswerComposer.java — 回答组装
- harness/TraceAgent.java — 全链路追踪
- harness/ContextBuilder.java — 上下文构建
- harness/RunStage.java — 流水线阶段定义

---

### ① 🛠️ 所用技术

| 属性 | 内容 |
|------|------|
| **核心技术** | Agent Harness 编排模式（自研架构） |
| **技术分类** | 架构模式 / 多 Agent 编排 / 流水线 |
| **在项目中的角色** | 将一次 RAG 问答拆解为 **检索 → 上下文构建 → LLM 回答 → QA 评测** 四个阶段，每个阶段由独立的 Agent 执行，通过 Orchestrator 串联 |
| **相关 Agent** | RetrievalAgent, AnswerComposer, TraceAgent, ContextBuilder |

### ② 💡 项目为什么采用这种架构

**项目约束（从代码中推断）:**

| 约束类型 | 分析 |
|---------|------|
| **可观测性约束** | 需要追踪每次问答的完整链路（检索命中数、回答质量评分、耗时），TraceAgent 在每个阶段记录事件 |
| **可扩展性约束** | 未来可能有更多 Agent（工具调用 Agent、多模态 Agent），需要编排器统一调度 |
| **多实现约束** | LLM 调用需支持多种实现（DeepSeek HTTP 直连 / Spring AI 封装），ModelClient 接口统一抽象 |
| **流式响应约束** | 需要同时支持同步回答和 SSE 流式回答，Orchestrator 提供 nswer() 和 nswerStream() 两套方法 |

**权衡取舍（Trade-off）:**

`
✅ 选择了：
   - 自研轻量编排器（无框架依赖，代码约 150 行）
   - 清晰的阶段职责分离（四个独立 Agent）
   - 接口抽象（ModelClient 支持多实现切换）
   - 全链路追踪内建

❌ 放弃了：
   - LangChain/LangGraph 的成熟 Agent 编排（条件分支、循环、并行）
   - Spring AI ETL 流水线的自动化程度
   - 工作流引擎（Temporal/Camunda）的持久化/重试/补偿

⚖️ 在当前阶段的合理性：
   项目 Agent 数量少、流程简单（线性 RAG 流水线），自研编排器足够。
   随复杂度增长，可能需要引入 LangChain4j 或状态机模式来管理复杂度。
`

### ③ 📚 技术深度剖析

#### 【核心原理 — RAG 四阶段流水线】

`
用户问题
    │
    ▼
┌─────────────────────────────────────────────┐
│ Stage 1: RETRIEVAL                           │
│ RetrievalAgent.search()                      │
│   → VectorKnowledgeService.search()          │
│   → pgvector 余弦相似度检索（Top-K=3）       │
│   → filterExpression 用户隔离                │
│ trace: USER_INPUT + CONTEXT_BUILD            │
└──────────────────┬──────────────────────────┘
                   │ sources
                   ▼
┌─────────────────────────────────────────────┐
│ Stage 2: CONTEXT BUILDING                    │
│ ContextBuilder + LlmClient.buildRequest()    │
│   → System Prompt 模板选择（有/无知识片段）   │
│   → 拼接历史消息（最近 10 轮）               │
│   → 拼接知识片段 [1] [2] [3] 引用格式        │
│   → temperature=0.2 控制创造性               │
└──────────────────┬──────────────────────────┘
                   │ full prompt
                   ▼
┌─────────────────────────────────────────────┐
│ Stage 3: LLM ANSWERING                       │
│ AnswerComposer.compose() / composeStream()    │
│   → ModelClient.answer() 调用 DeepSeek       │
│   → 同步模式：HTTP POST → JSON 解析          │
│   → 流式模式：SSE → BufferedReader 逐行解析  │
│   → 失败降级：fallback() 返回检索摘要         │
└──────────────────┬──────────────────────────┘
                   │ answer
                   ▼
┌─────────────────────────────────────────────┐
│ Stage 4: QA REVIEW                           │
│ performQaReview()                             │
│   → 检索命中检查（sources 非空）             │
│   → 引用检查（答案是否含 [1] [2] 格式）      │
│   → 计算质量分数（0.0-1.0）                  │
│ trace: QA_REVIEW                             │
└─────────────────────────────────────────────┘
`

#### 【关键概念】

**1. 降级策略（Graceful Degradation）**

当 DeepSeek API 不可用时，LlmClient.fallback() 不抛异常，而是返回结构化的检索摘要：

`java
// 来源: llm/LlmClient.java
private String fallback(String question, List<DocumentChunk> sources) {
    if (sources.isEmpty()) {
        if (isGreeting(question)) {
            return "你好，我是知识库问答助手...";  // 友好问候
        }
        return "知识库中没有检索到与" + question + "相关的片段...";
    }
    // 返回检索摘要而非报错
    return "已检索到 " + sources.size() + " 个相关知识片段：\n" + ...;
}
`

**2. 用户数据隔离（Lightweight Multi-Tenant）**

通过 userId 实现轻量级的用户数据隔离：

`java
// 来源: knowledge/VectorKnowledgeService.java:51-57
// pgvector 向量层：通过 filterExpression 过滤
SearchRequest request = SearchRequest.builder()
    .query(query)
    .topK(Math.min(topK, 20))
    .filterExpression("userId == '" + userId + "'")
    .build();
`

#### ⚠️ 常见陷阱与项目现状

| # | 常见陷阱 | 本项目的处理方式 | 潜在风险/改进建议 |
|---|---------|----------------|------------------|
| 1 | filterExpression 中 userId 未做 SQL 注入防护 | 直接拼接字符串到 WHERE 子句 | 🔴 **高风险**: 建议对 userId 做白名单校验或参数化 |
| 2 | 流水线阶段失败后无重试机制 | 异常被 catch 后直接 fallback | 🟡 建议加入指数退避重试 |
| 3 | QA Review 评分算法过于简单 | 仅检查 retrievalHit + citationPresent | 🟡 建议引入 LLM-as-Judge 评估 |

### ④ 🔄 可替代方案对比

| 对比维度 | ✅ **自研 Harness** | 🔶 **LangChain4j Agent** | 🔶 **Spring AI ETL** |
|---------|--------------------|--------------------------|----------------------|
| **核心优势** | 极简（~150行），完全可控 | 成熟 Agent 框架（Tool/Memory/Chain） | Spring 原生，AutoConfiguration |
| **主要劣势** | 缺乏高级编排（分支/循环/并行） | 额外依赖，API 快速变化 | ETL 流程抽象较固定 |
| **本项目如果换成它** | — | 可复用 Tool Calling 模块，需重构核心流程 | 可替换知识库索引，但编排不如 Harness 灵活 |
| **学习曲线** | 低（自研，完全理解） | 中等 | 低 |
| **适用场景** | Agent 少、流程简单的 RAG | 多 Agent 协作、条件路由 | 纯 RAG 索引与检索 |

### ⑤ ⭐ 优越性与风险评估

**关键优势:**

1. **极致可控的编排粒度** — 每个阶段的 trace 事件显式记录，不依赖 AOP/注解魔法
2. **多 LLM 无缝切换** — ModelClient 接口支持 LlmClient 和 SpringAiModelClient 双实现
3. **开发者友好** — 代码量少（~150 行编排逻辑），新人可快速理解

**📊 技术健康度评估:**

| 指标 | 本项目现状 | 评级 |
|------|----------|------|
| 架构清晰度 | 四阶段职责分明，代码适中 | 🟢 优秀 |
| 可扩展性 | 新增 Agent 成本低 | 🟢 良好 |
| 错误处理 | 有降级，但无重试 | 🟡 需改进 |
| 安全防护 | 存在 SQL 注入风险 | 🔴 需修复 |

### ⑥ 🔗 知识延伸

**🔄 思想迁移:**
- **Harness 编排** → CI/CD Pipeline（Jenkins/GitHub Actions）同样是阶段式 + 事件记录
- **降级策略** → 微服务 Circuit Breaker（Hystrix/Resilience4j）
- **ModelClient 接口抽象** → 策略模式（Strategy Pattern），JDBC Driver 同理

**📖 深入学习路径:**

`
Java SE + 设计模式 → Spring IoC/AOP → 多 Agent 编排 → LangChain4j / LangGraph
`

---


## 🎯 Teaching Card 2: Spring AI + DeepSeek LLM 双通道集成

> **卡片类型**: 标准 Teaching Card
> **分析对象**: Spring AI 1.1.4 框架 + DeepSeek 模型双通道调用设计

📁 **对应源码**:
- i/SpringAiModelClient.java — Spring AI 官方封装
- llm/LlmClient.java — DeepSeek HTTP 直连实现（220 行）
- llm/ModelClient.java — 统一接口
- pplication.yml — DeepSeek 配置项

---

### ① 🛠️ 所用技术

| 属性 | 内容 |
|------|------|
| **核心技术** | DeepSeek API（OpenAI 兼容）+ Spring AI 1.1.4 |
| **技术分类** | LLM 接入层 / 多策略实现 |
| **在项目中的角色** | 双通道 LLM 调用：LlmClient（Java 11 HttpClient 直连） + SpringAiModelClient（Spring AI 封装） |
| **配置项** | deepseek.api-key, deepseek.base-url, deepseek.model |

### ② 💡 项目为什么采用双通道设计

**项目约束:**

| 约束 | 分析 |
|------|------|
| **模型切换** | 当前 DeepSeek，未来可能切 OpenAI / 国产模型，希望接口统一 |
| **精细控制** | LlmClient 手写 HTTP + SSE 流式解析，可精细控制请求体、超时、重试 |
| **Spring 生态** | SpringAiModelClient 让项目用到 Spring AI 的 ETL/RAG 高级功能 |

**权衡取舍:**

`
✅ 选择了：双实现共存（LlmClient 灵活 + SpringAiModelClient 标准），接口统一
❌ 放弃了：纯 Spring AI 的 AutoConfiguration 便利性（需手动维护 LlmClient HTTP 逻辑）
⚖️ 合理性：LlmClient（220行）给了最大控制力（自定义 temperature=0.2、prompt 模板、
   降级策略），代价是维护成本略高于纯 Spring AI 方案。
`

### ③ 📚 技术深度剖析

#### 【核心原理 — OpenAI 兼容协议 + SSE 流式】

LlmClient 实现了完整的 OpenAI Chat Completions API 协议：

**同步调用:**
`
buildRequest() → HTTP POST → /chat/completions
→ 解析 JSON choices[0].message.content → 状态码检查 → fallback 降级
`

**流式调用（SSE）:**
`
buildStreamRequest() → "stream": true → HTTP POST → InputStream
→ BufferedReader 逐行读取 SSE 数据
→ data: {"choices":[{"delta":{"content":"token"}}]}
→ data: [DONE] → onDone
→ 异常 → onError
`

**DeepSeek 协议兼容性:** DeepSeek API 完全兼容 OpenAI /chat/completions 端点。只需修改 aseUrl + model 配置即可切换到 OpenAI。

#### 【关键概念 — Prompt 模板分支】

项目根据是否有知识片段，使用不同的 System Prompt：

`java
// 来源: llm/LlmClient.java
String system = sources.isEmpty()
    ? "你是一个简洁、友好的中文问答助手..."     // 闲聊模式
    : "你是一个知识库问答助手。...关键结论后用 [1]、[2] 引用来源编号";  // RAG 模式
`

这是一种简化的 Prompt Router 模式（等价于 LangChain 的 RunnableBranch）。

#### ⚠️ 常见陷阱与项目现状

| # | 常见陷阱 | 本项目现状 | 改进建议 |
|---|---------|-----------|---------|
| 1 | Token 超限（DeepSeek 128K 上下文窗口） | 历史限制最近 10 轮 + topK=3 | ✅ 合理，但未显式计算 token 数 |
| 2 | SSE 连接中断无重连 | BufferedReader 读到 [DONE] 或异常即结束 | 🟡 建议加 SSE 重连机制 |
| 3 | Stream 模式 onError 同时调了 onDone | 异常时先调 onDelta 再调 onDone | 🟡 前端需处理双重回调 |

### ④ 🔄 可替代方案对比

| 对比维度 | ✅ **本项目（HTTP 直连 + Spring AI）** | 🔶 **纯 Spring AI** | 🔶 **LangChain4j** |
|---------|-------------------------------------|--------------------|--------------------|
| **核心优势** | 最大控制力 + Spring 兼容 | 全自动 AutoConfiguration | 功能最全面 |
| **主要劣势** | 需维护 220 行 HTTP 代码 | 定制 prompt/fallback 不灵活 | API 频繁变动 |
| **切换模型成本** | 改配置 | 改配置 | 改配置 + 可能改代码 |

### ⑤ ⭐ 优越性与风险评估

**关键优势:**

1. **最大控制力** — 可自定义所有 HTTP 参数、prompt 模板、降级逻辑
2. **DeepSeek 成本优势** — DeepSeek API 价格为 GPT-4 的 1/50，适合大量 RAG 调用
3. **模型中立** — 协议层兼容 OpenAI，不锁定单一供应商

**📊 技术健康度评估:**

| 指标 | 本项目现状 | 评级 |
|------|----------|------|
| 代码质量 | 220 行清晰的 HTTP 封装 | 🟢 良好 |
| 可切换性 | 只需改配置即可切换模型 | 🟢 优秀 |
| 错误处理 | 降级 + 异常捕获完整 | 🟢 良好 |
| 流式体验 | SSE 逐 token 推送 | 🟢 良好 |

---


## 🎯 Teaching Card 3（增强版）: pgvector 向量存储 + H2 双数据库策略

> **卡片类型**: Enhanced Teaching Card（架构决策）
> **分析对象**: pgvector + H2 双数据库环境变量切换策略

📁 **对应源码**:
- pplication.yml — POSTGRES_URL / POSTGRES_DRIVER 环境变量
- knowledge/VectorKnowledgeService.java — pgvector 向量操作
- knowledge/KnowledgeService.java — JPA 结构化存储

---

### ① 🛠️ 所用技术

| 属性 | 内容 |
|------|------|
| **核心技术** | pgvector（PostgreSQL 向量扩展）/ H2 File Database |
| **技术分类** | 向量数据库 + 关系数据库 / 双数据库策略 |
| **在项目中的角色** | H2：用户/会话/评测/知识片段元数据（JPA）；pgvector：文档 Embedding 向量 + ANN 检索（Spring AI VectorStore） |
| **切换方式** | 环境变量 POSTGRES_URL / POSTGRES_DRIVER 动态切换 |

### ② 💡 为什么采用双数据库 + 可切换策略

**项目约束:**

| 约束 | 分析 |
|------|------|
| **开发友好** | 本地开发不想安装 PostgreSQL，H2 file mode 零配置即可运行 |
| **生产就绪** | 生产需 pgvector 向量检索能力，通过环境变量一键切换 |
| **数据分类** | 结构化数据（用户/会话）和向量数据（Embedding）有不同存储需求 |

**设计精妙之处:**

`yaml
# 来源: application.yml — 默认 H2，环境变量覆盖为 PG
datasource:
  url: 
  driver-class-name: 
`

- **开发环境**: 不设环境变量 → 自动用 H2，零依赖即可运行
- **生产环境**: 设 POSTGRES_URL=jdbc:postgresql://... → 自动切换 pgvector

**权衡取舍:**

`
✅ 选择了：双数据库混合架构 + 环境变量切换，最大化开发便利性和生产就绪性
❌ 放弃了：单一数据库的一致性优势（跨库事务需分布式协调）
⚖️ 合理性：结构化数据和向量数据天然适合分离存储，且 pgvector 本身是 PG 扩展，
   切换只是从 H2 到 PG，不算真正的"双数据库"。
`

### ⑦ 🌳 决策树

`
                    ┌──────────────────────┐
                    │   项目需要数据持久化   │
                    │  + 向量检索            │
                    └──────────┬───────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
      ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
      │ 开发环境？    │ │ 需要向量检索？│ │ 团队有 PG 经验│
      │   ✅ H2       │ │   ✅ 是       │ │   ✅ 是       │
      └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
             │                │                │
        ┌────┴────┐      ┌────┴────┐      ┌────┴────┐
        ▼         ▼      ▼         ▼      ▼         ▼
    ✅ H2     PG+pgvec  PG+pgvec  Milvus  PG+pgvec  ES
   (开发)    (生产)     (生产)           (生产)
   
   【本项目选择：H2(开发默认) + PG+pgvector(生产切换)】
`

**决策路径说明:** 本项目选择了最左侧路径：开发环境用 H2 零依赖启动，生产环境切换到 PG+pgvector。当向量数据 < 1000 万时，pgvector 的 HNSW 索引可提供足够查询性能。

### ⑧ ⚠️ 架构风险评估

| 风险类型 | 具体风险 | 可能性 | 影响 | 缓解措施 | 改进建议 |
|---------|---------|--------|-----|---------|---------|
| 🔴 **安全风险** | filterExpression 直接拼接 userId 到 SQL WHERE 子句 | 高 | 高 | 未处理 | **紧急修复**: userId 白名单校验或参数化 |
| 🟡 **数据风险** | H2 file mode DB_CLOSE_DELAY=-1 进程崩溃可能丢数据 | 中 | 中 | 延迟关闭减少风险 | 生产用 PG 避免 |
| 🟡 **迁移风险** | H2→PG 切换时 DDL 语法差异（H2Dialect vs PG） | 中 | 中 | ddl-auto=update 自动适配 | 首次切换手动验证 Schema |
| 🟢 **性能风险** | pgvector 连接池未显式配置 | 低 | 中 | HikariCP 默认值 | 生产建议配置 pool size |
| 🟡 **扩展风险** | 向量数据增至千万级 pgvector 性能下降 | 中 | 高 | 当前无需过度设计 | 预留 VectorStore 接口切换 Milvus |

### ④ 🔄 可替代方案对比

| 对比维度 | ✅ **pgvector** | 🔶 **Elasticsearch** | 🔶 **Milvus** |
|---------|----------------|---------------------|--------------|
| **核心优势** | 零额外运维，SQL 原生 | 全文+向量二合一 | 十亿级性能领先 |
| **主要劣势** | 大规模性能不如专用库 | JVM 资源消耗大 | 需独立部署维护 |
| **本项目如果换成它** | — | 需额外部署 ES 集群 | 增加运维复杂度 |
| **查询延迟** | 10-100ms（百万级） | 10-50ms | 1-10ms（十亿级） |
| **学习曲线** | 低（已熟悉 PG） | 中等 | 中高 |

---


## 🎯 Teaching Card 4: MCP Server 工具暴露

> **卡片类型**: 标准 Teaching Card
> **分析对象**: Spring AI MCP Server + @Tool 注解工具暴露

📁 **对应源码**: mcp/McpToolService.java

---

### ① 🛠️ 所用技术

| 属性 | 内容 |
|------|------|
| **核心技术** | Spring AI MCP Server (WebMVC transport) 1.1.4 |
| **技术分类** | 协议实现 / 工具暴露 / AI 集成 |
| **在项目中的角色** | 将 5 个工具通过 MCP 协议暴露给外部 AI 客户端（Claude Code 等） |
| **暴露的工具** | calculator, echo, searchKnowledge, httpMock, platformStatus |
| **配置** | spring.ai.mcp.server.enabled=true, 	ype=SYNC |

### ② 💡 项目为什么集成 MCP

**核心价值:** MCP（Model Context Protocol）是 Anthropic 推出的开放协议，让 AI 助手能够**动态发现和调用外部工具**。项目集成 MCP Server 后：

- Claude Code 可自动发现工具并调用
- 无需为每个 AI 客户端单独开发 API 适配层
- @Tool 注解 + description 让工具自描述

**权衡取舍:**

`
✅ 选择了：MCP 协议标准化（一次开发，多客户端复用）
❌ 放弃了：自定义 REST API 的完全控制权
⚖️ 合理性：MCP 正在成为 AI 工具调用的行业标准，提前集成是前瞻性决策
`

### ③ 📚 技术深度剖析

#### 【核心原理 — @Tool 注解自动发现】

`java
// 来源: mcp/McpToolService.java
@Tool(description = "Calculate simple arithmetic. Input format: '12 + 30'")
public String calculator(String input) { ... }

@Tool(description = "Search the knowledge base. Returns up to 5 matching chunks.")
public String searchKnowledge(String query, String userId) { ... }

@Tool(description = "Get platform status including available tools and knowledge count")
public String platformStatus(String userId) { ... }
`

Spring AI MCP Server 自动扫描 @Tool 注解的方法，生成 MCP 协议的 	ools/list 响应，客户端可通过 	ools/call 调用。

### ⚠️ 关键风险

| # | 风险 | 建议 |
|---|------|------|
| 1 | searchKnowledge 的 userId 参数无认证校验，任何 MCP 客户端可查任意用户数据 | 🔴 建议在 MCP 层加入用户认证（当前 SYNC 类型无内置认证） |
| 2 | calculator 使用 Runtime.getRuntime().exec() 风格执行？ | 🟡 需确认输入是否做充分校验 |

---

## 🎯 Teaching Card 5（精简）: JWT 认证 + 用户数据隔离

> **卡片类型**: Mini Card
> 📁 参见: uth/JwtUtil.java, uth/JwtAuthenticationFilter.java, uth/SecurityConfig.java

**所用技术:** JJWT 0.12.5 + Spring Security Crypto — JWT Token 签发/验证 + BCrypt 密码哈希

**为什么项目用它:** 项目需要用户注册/登录 + 每个用户的 API 请求携带 Token 以标识身份（如 VectorKnowledgeService 中根据 userId 过滤向量数据）。JJWT 0.12.x 是目前 Java JWT 的社区标准库，API 简洁。

**优越性:** 密钥通过 jwt.secret 配置，支持 Token 过期（jwt.expiration），JwtAuthenticationFilter 对 /api/** 路径统一拦截，通过 SecurityFilterChain 注册实现方式标准。

**需要注意:** 确认 Token 刷新机制是否已实现（如 refresh token），当前代码中 JwtUtil 仅为签发 + 验证。

---


---

## 📊 项目综合评价与改进建议

### 🟢 做得好的地方

| # | 亮点 | 说明 |
|---|------|------|
| 1 | **架构清晰** | Harness 四阶段流水线职责分明，Agent 间低耦合，新人可快速理解 |
| 2 | **双通道 LLM** | ModelClient 接口 + 双实现，既灵活（手写 HTTP 精细控制）又标准（Spring AI 集成） |
| 3 | **环境适配优雅** | H2（开发）/ PG+pgvector（生产）通过环境变量一键切换，零配置本地启动 |
| 4 | **降级友好** | LLM 不可用时返回检索摘要而非报错，用户体验不受损 |
| 5 | **前瞻性集成** | MCP Server、Micrometer+Prometheus 提前布局，为生产运维做准备 |
| 6 | **用户数据隔离** | 向量检索 + JPA 查询均通过 userId 过滤，实现轻量级多租户 |

### 🔴 需要立即修复的

| 优先级 | 严重程度 | 问题 | 位置 | 修复建议 |
|--------|---------|------|------|---------|
| 🔴 P0 | **SQL 注入** | filterExpression 直接拼接 userId | VectorKnowledgeService.java:51 | 对 userId 做 ^[a-zA-Z0-9_-]+$ 白名单校验 |
| 🔴 P0 | **未授权访问** | MCP tool 无认证，任意客户端可查用户数据 | McpToolService.java | 在 MCP Server 层加入 API Key 或 Token 认证 |

### 🟡 可以优化的

| 优先级 | 建议 | 预期收益 |
|--------|------|---------|
| P1 | QA Review 引入 LLM-as-Judge 评估 faithfulness/relevance | 质量评测从二元判断提升为多维度量化 |
| P1 | LlmClient 加入指数退避重试（参考 SKILL_REVIEW 示例 2 的 useFetch 模式） | 降低网络抖动导致的回答失败率 |
| P2 | 使用 JTokkit/tiktoken 显式计算 token 数 | 防止超上下文窗口截断 |
| P2 | MCP Server 从 SYNC 升级到 STREAMABLE | 支持流式工具调用 |
| P3 | 统一构造器注入 + @RequiredArgsConstructor | 减少样板代码，提升可测试性 |

### 🔵 架构演进建议

`
当前架构: 线性 RAG 流水线（检索 → 构建 → 回答 → 评测）
                │
                ▼ （当 Agent 数量 > 5 或有条件分支需求时）
                │
未来架构: LangChain4j Agent 编排
          ├── RetrievalAgent（保留）
          ├── AnswerAgent（保留）
          ├── ToolCallingAgent（新增：调用 calculator/httpMock 等工具）
          ├── RouterAgent（新增：根据意图路由到不同 Agent）
          └── TraceAgent（保留，增加 span 层级）
          
数据库演进:
  H2(dev) + PG+pgvector(prod)
       │
       ▼ （当向量 > 1000 万 或需要更低延迟时）
       │
  PG(结构化) + Milvus(向量) 分离部署
`

---

## 📋 附录: 项目依赖全景清单

### 核心框架层
| GroupId | ArtifactId | 版本 | 用途 |
|---------|-----------|------|------|
| org.springframework.boot | spring-boot-starter-web | 3.2.5 | Web 服务（内嵌 Tomcat） |
| org.springframework.boot | spring-boot-starter-data-jpa | 3.2.5 | JPA 数据访问 |
| org.springframework.boot | spring-boot-starter-validation | 3.2.5 | 参数校验 |
| org.springframework.boot | spring-boot-starter-actuator | 3.2.5 | 健康检查 + 指标 |

### AI 与向量层
| GroupId | ArtifactId | 版本 | 用途 |
|---------|-----------|------|------|
| org.springframework.ai | spring-ai-starter-model-openai | 1.1.4 | OpenAI 兼容模型调用 |
| org.springframework.ai | spring-ai-starter-vector-store-pgvector | 1.1.4 | pgvector 向量存储 |
| org.springframework.ai | spring-ai-starter-mcp-server-webmvc | 1.1.4 | MCP Server 协议 |

### 数据存储层
| GroupId | ArtifactId | 版本 | 用途 |
|---------|-----------|------|------|
| com.h2database | h2 | — | 开发环境数据库（file mode） |
| org.postgresql | postgresql | — | 生产环境 PostgreSQL 驱动 |

### 文档处理层
| GroupId | ArtifactId | 版本 | 用途 |
|---------|-----------|------|------|
| org.apache.pdfbox | pdfbox | 2.0.30 | PDF 文本提取 |
| org.apache.poi | poi-ooxml | 5.2.5 | Word/Excel 文本提取 |

### 安全认证层
| GroupId | ArtifactId | 版本 | 用途 |
|---------|-----------|------|------|
| org.springframework.security | spring-security-crypto | — | BCrypt 密码哈希 |
| io.jsonwebtoken | jjwt-api | 0.12.5 | JWT Token 签发/验证 |
| io.jsonwebtoken | jjwt-impl | 0.12.5 | JWT 实现 |
| io.jsonwebtoken | jjwt-jackson | 0.12.5 | JWT JSON 序列化 |

### 监控与文档层
| GroupId | ArtifactId | 版本 | 用途 |
|---------|-----------|------|------|
| io.micrometer | micrometer-registry-prometheus | — | Prometheus 指标暴露 |
| org.springdoc | springdoc-openapi-starter-webmvc-ui | 2.5.0 | Swagger API 文档 |

---

> **分析完成时间**: 2026-05-15
> **分析基于**: deep-teach-review v1.0.0 (SKILL_REVIEW.md)
> **原始 Skill 项目**: https://github.com/1786329860/deep-teach
> **如需深入分析特定模块**（evaluation 评测系统 / skill 管理 / tool registry / knowledge 文档处理），请继续告知。
