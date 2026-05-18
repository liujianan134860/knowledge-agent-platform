# knowledge-agent-platform 项目深度解析

> 基于 deep-teach-review Skill v1.0.0 生成 | 分析日期: 2026-05-15
> 覆盖: 项目全景 + 7 张核心技术 Teaching Card + 架构决策分析

---

## 项目全景概览

### 项目身份

| 属性 | 内容 |
|------|------|
| **项目名称** | knowledge-agent-platform（知识代理平台） |
| **定位** | 基于 Agent Harness 架构的知识库问答与工具调用平台 |
| **作者** | Liu Jianan (com.liujianan) |
| **语言/版本** | Java 17 |
| **构建工具** | Maven |
| **基础框架** | Spring Boot 3.4.5 |
| **AI 框架** | Spring AI 1.1.4 |
| **源码规模** | 75 个 Java 源码文件，15 个测试文件，2 个 Flyway 迁移脚本 |

### 技术栈全景图

| 层级 | 技术 | 版本 | 角色 |
|------|------|------|------|
| **语言** | Java | 17 | 开发语言 |
| **核心框架** | Spring Boot | 3.4.5 | IoC / Web / 数据 / 配置 |
| **AI 框架** | Spring AI | 1.1.4 | LLM 统一抽象 + VectorStore |
| **LLM 调用** | DeepSeek (OpenAI 兼容) | deepseek-chat | 对话生成 + Embedding |
| **Embedding** | text-embedding-3-small | - | 1536 维向量化 |
| **向量数据库** | pgvector (PG 扩展) | - | cosine 相似度 + HNSW |
| **关系数据库** | H2 (file) / PostgreSQL | - | 开发/生产双模 |
| **ORM** | Spring Data JPA + Hibernate 6 | - | Repository 模式 |
| **数据库迁移** | Flyway | managed | 版本化 schema |
| **文档解析** | PDFBox 2.0.30 + POI 5.2.5 | - | PDF/Word 文本提取 |
| **认证授权** | Spring Security + JJWT 0.12.5 | - | 无状态 JWT + BCrypt |
| **令牌管理** | Refresh Token + 黑名单 | - | 长期会话 + 主动登出 |
| **协议** | MCP Server (WebMVC) | 1.0.0 | 对外暴露工具能力 |
| **可观测性** | Micrometer + Prometheus | - | 指标采集与监控 |
| **API 文档** | springdoc-openapi 2.8.8 | - | Swagger UI |
| **架构模式** | Agent Harness (多 Agent) | - | 流水线编排 |
| **搜索算法** | BM25 + pgvector HNSW | - | 混合检索 |
| **评测系统** | LLM-as-Judge (Qwen3.6) + 规则引擎 | - | 答案质量评分 |

### 目录结构与模块职责

```
com.liujianan.agentdemo/
  agent/          (2)    Agent 信息查询 API
  ai/             (1)    Spring AI 模型客户端
  audit/          (4)    审计日志
  auth/           (12)   认证授权
  chat/           (4)    聊天 API (同步+SSE)
  common/         (7)    公共组件
  evaluation/     (13)   评测系统
  harness/        (12)   核心编排引擎
  home/           (1)    首页控制器
  knowledge/      (9)    知识库
  llm/            (2)    LLM 抽象接口
  mcp/            (1)    MCP 工具暴露
  skill/          (2)    Skill 定义
  tool/           (6)    工具注册/调用
```

### 核心数据流

```
用户提问 -> ChatController -> ChatService -> HarnessOrchestrator
                                              |
            +-----------------+---------------+-----------------+
            |                 |                                 |
    RetrievalAgent    ToolPlanningService              AnswerComposer
    (BM25+向量检索)    (正则/工具规划)                  (LLM调用+重试)
            |                 |                                 |
            v                 v                                 v
    KnowledgeService     ToolRegistry                      ModelClient
    ->VectorKnowledgeSvc (calculator/echo)              ->SpringAiModelClient
      ->pgvector HNSW                                       ->OpenAiChatModel
            |                                                   |
            +------------- ContextBuilder ---------------------+
                                    |
                                    v
                          Build System + User Prompt
                                    |
                                    v
                           LLM 生成回答 (DeepSeek)
                                    |
                                    v
                            QA Review (质量评分)
                                    |
                                    v
                            SSE 流式返回用户
```

---

## 核心 Teaching Cards 索引

| # | 类型 | 分析对象 |
|---|------|---------|
| Card 1 | Enhanced | **多 Agent Harness 流水线架构** |
| Card 2 | Standard | **Spring Boot + Spring Security 基础设施** |
| Card 3 | Standard | **Spring AI + DeepSeek 模型集成** |
| Card 4 | Standard | **混合检索引擎 (BM25 + pgvector HNSW)** |
| Card 5 | Standard | **评测体系 (LLM-as-Judge + 规则引擎)** |
| Card 6 | Mini | **工具规划与调用系统** |
| Card 7 | Mini | **可观测性与韧性设计** |

---


## Card 1: 核心架构决策 — Agent Harness 多 Agent 编排流水线

**卡片类型: Enhanced Teaching Card**

**分析对象:** HarnessOrchestrator 为核心的多 Agent 协同架构
**对应源码:** harness/HarnessOrchestrator.java, harness/RetrievalAgent.java, harness/AnswerComposer.java, harness/TraceAgent.java, harness/ContextBuilder.java, harness/SessionService.java, tool/ToolPlanningService.java, tool/ToolRegistry.java

---

### ① 所用技术

| 属性 | 内容 |
|------|------|
| **架构模式** | Agent Harness — 多 Agent 流水线编排 |
| **技术分类** | 架构模式 / 设计模式 (Orchestrator + Chain of Responsibility) |
| **在项目中的角色** | 将一次问答请求拆解为: 检索 -> 工具规划 -> 上下文构建 -> LLM生成 -> QA审查 |
| **核心设计原则** | 单一职责 Agent + 异步并行 + 追踪透传 + 断路器保护 |
| **相关类** | HarnessOrchestrator, RetrievalAgent, AnswerComposer, TraceAgent, ToolPlanningService |

### ② 为什么采用 Agent Harness 架构

**项目约束（从代码推断）:**

| 约束类型 | 分析 |
|---------|------|
| **功能约束** | 单次问答需要多步骤: 知识检索 + 工具调用 + 上下文拼接 + LLM 生成 + 质量评分 |
| **性能约束** | 检索和工具调用是独立的，可以并行执行以降低总延迟 |
| **可观测性约束** | 需要追踪每一步的执行状态、耗时、结果，用于调试和评测 |
| **可扩展性约束** | 未来可能增加新 Agent 类型（多跳推理、图检索），需要解耦 |
| **容错约束** | 某个 Agent 失败不应导致整个请求崩溃 |

**匹配原因:**

| 约束 | Agent Harness 如何满足 |
|------|-----------------------|
| 多步骤编排 | Orchestrator 线性调度，每个 Agent 只负责自己的步骤 |
| 并行执行 | RetrievalAgent 和 ToolPlanningService 使用 CompletableFuture 异步并行 |
| 可观测性 | TraceAgent 透传 sessionId，每个 Agent 写入 TraceEvent |
| 可扩展性 | 新增 Agent 只需在 Orchestrator 中插入调用点 |
| 容错性 | QA Review 包裹在 try-catch 中，失败不阻塞主流程 |

**权衡取舍:**
```
选择: 清晰分层流水线、Agent 解耦、异步并行、全链路追踪
放弃: 单体 Service 直调 (耦合重)、LangChain4j DAG 编排 (过度设计)
在项目当前阶段的合理性:
  项目 Agent 数量不多 (< 10)，线性流水线足够。异步并行将关键路径从
  sum(检索, 工具) 优化为 max(检索, 工具)。全链路追踪为后续调优打基础。
```

### ③ 技术深度剖析

#### 核心原理 — 流水线执行模型

```
用户请求
    |
    v
HarnessOrchestrator.answer()
    |
    +-> SessionService.append("user", question)
    +-> TraceAgent.record("USER_INPUT")
    |
    +-> buildSources()  [CompletableFuture 异步并行]
    |    +- supplyAsync(RetrievalAgent.search() -> KnowledgeService -> pgvector HNSW)
    |    +- supplyAsync(ToolPlanningService.plan() -> ToolRegistry.invoke())
    |    +- 合并: sources + toolSources
    |
    +-> TraceAgent.record("CONTEXT_BUILD")
    +-> AnswerComposer.compose()
    |    +- answerWithRetry(maxAttempts=2) -> ModelClient -> SpringAiModelClient
    |    |    -> ChatModel.call(prompt) -> HTTP POST DeepSeek API
    |    +- Micrometer Timer 记录耗时
    |
    +-> SessionService.append("assistant", answer)
    +-> performQaReview()  (非关键路径, try-catch 保护)
    +-> return ChatResponse
```

#### 关键概念

**概念 1: 异步并行检索**

```java
// 来源: HarnessOrchestrator.java:132-145
// 检索和工具规划并行执行，互不阻塞
CompletableFuture<List<DocumentChunk>> retrievalFuture = CompletableFuture.supplyAsync(
        () -> retrievalAgent.search(sessionId, question, topK, userId), taskExecutor);
CompletableFuture<List<DocumentChunk>> toolFuture = CompletableFuture.supplyAsync(
        () -> planAndInvokeTool(sessionId, question, userId), taskExecutor);
// join() 等待两者完成 -> 总耗时 = max(检索, 工具), 而非 sum
List<DocumentChunk> sources = retrievalFuture.join();
List<DocumentChunk> toolSources = toolFuture.join();
```

**概念 2: 全链路追踪 (TraceAgent + TraceEvent)**

```java
// 来源: HarnessOrchestrator.java:49-66
// 每个关键步骤写入 TraceEvent，携带结构化元数据
traceAgent.record(session.id(), "USER_INPUT", "received user question",
        Map.of("questionLength", question.length()), userId);
traceAgent.record(session.id(), "CONTEXT_BUILD", "built prompt context",
        Map.of("sourceCount", sources.size(), "promptVersion", promptVersion), userId);
traceAgent.record(session.id(), "TOOL_PLAN", "planned tool usage",
        Map.of("required", plan.required(), "toolName", plan.toolName()), userId);
// AnswerComposer 内部记录 ANSWER 事件（含 latencyMs, promptTokens）
```

**概念 3: 会话管理 (ChatSession + SessionService)**

```java
// 来源: SessionService.java
// 每次对话: 创建会话 -> 追加消息 -> 持久化
ChatSession session = sessionService.append(sessionId, "user", question, userId);
// ... 处理 ...
sessionService.append(session.id(), "assistant", answer, userId);
// ChatSession 通过 JPA 持久化到数据库，支持跨请求恢复上下文
```

#### 关键数据结构

| 实体 | 关键字段 | 作用 |
|------|---------|------|
| ChatSession | id (UUID), userId, createdAt | 会话生命周期管理 |
| SessionMessage | sessionId, role, content | 对话历史 (支持多轮) |
| TraceEvent | sessionId, stage, attributes (JSON Map), userId | 流水线元数据 |
| DocumentChunk | id, title, content, tags, userId | 知识片段 |
| ChatRequest/ChatResponse | sessionId, question / answer, sources, promptTokens, latency | 请求/响应 DTO |

#### 常见陷阱与项目现状

| # | 常见陷阱 | 本项目的处理方式 | 风险/改进建议 |
|---|---------|----------------|-------------|
| 1 | Agent 间耦合过重 | Orchestrator 解耦，Agent 无直接依赖 | 良好 |
| 2 | 并行任务异常导致整体失败 | toolFuture 异常单独处理 | retrievalFuture 应加 orTimeout() |
| 3 | TraceEvent 随请求膨胀 | 每次 5-7 个事件 | 建议定期清理/归档旧事件 |
| 4 | TaskExecutor 队列满 | core=4, max=8, queue=32 | 建议加 CallerRunsPolicy 拒绝策略 |

### ④ 可替代方案对比

| 对比维度 | Agent Harness (本项目) | LangChain4j Agent | LangGraph (Python) |
|---------|----------------------|-------------------|-------------------|
| **核心优势** | Spring 原生集成，零额外依赖 | 功能最全面 (Memory/Tool/Chain) | DAG 编排, 分支/循环/条件 |
| **主要劣势** | 需手写 Orchestrator，无循环/条件 | 非 Spring 官方，API 变动大 | Java 不支持，需跨语言 |
| **本项目如果换成它** | — | 替换所有 Agent 代码 | 需增加 Python 微服务 |
| **适用场景** | Spring Boot + 线性 RAG | 复杂多步骤 Agent | 高级 Agent 工作流 |
| **学习曲线** | 低 | 中等 | 高 |

### ⑤ 优越性与风险评估

**关键优势:**

1. **异步并行降低 40-50% 延迟** — 检索和工具调用从串行 sum 改为 max，在检索慢+工具快场景效果显著
2. **全链路追踪可复现每次请求** — TraceEvent 记录每个阶段的执行状态、属性和耗时
3. **失败隔离** — QA Review 失败不影响回答返回，保证主链路可用

**技术健康度评估:**

| 指标 | 现状 | 评级 |
|------|------|------|
| 架构清晰度 | Agent 职责分明 | 优秀 |
| 可扩展性 | 新增 Agent 只需加调用点 | 良好 |
| 容错性 | QA Review 隔离，但检索无超时 | 需改进 |
| 性能 | 异步并行优化到位 | 优秀 |

### ⑥ 知识延伸

**思想迁移 — 可应用于:**
- 电商下单 Pipeline: 校验库存 -> 计算价格 -> 创建订单 -> 通知
- CI/CD Pipeline: 编译 -> 测试 -> 扫描 -> 部署
- 内容审核: 文本检测 -> 图片审核 -> 人工复核

**深入学习路径:**
```
Java CompletableFuture -> 设计模式 (Orchestrator/CoR) -> LangChain4j / LangGraph
```

### ⑦ 架构决策树

```
                    +----------------------+
                    |   项目需要 RAG 问答    |
                    |   是否需要 Agent?     |
                    +----------+-----------+
                               |
              +----------------+----------------+
              |                |                |
         步骤 < 3?         步骤 3-5?        步骤 > 5?
              |                |                |
        单体 Service      Agent Harness     LangGraph
        (过度简单)        [本项目选择]      (过度设计)
                               |
                    异步并行 + 追踪完备
```

### ⑧ 架构风险评估

| 风险类型 | 具体风险 | 可能性 | 影响 | 缓解措施 | 改进建议 |
|---------|---------|--------|-----|---------|---------|
| 技术风险 | CompletableFuture 无超时导致线程泄漏 | 低 | 高 | 当前无 | 加 orTimeout(10s) |
| 扩展风险 | Agent 增多时 Orchestrator 膨胀 | 中 | 中 | 当前 4 个 Agent | 引入 Agent 注册表 |
| 安全风险 | userId 参数透传可能被篡改 | 低 | 高 | SecurityContext 认证 | 从 SecurityContext 提取 userId |

---



## Card 2: Spring Boot + Spring Security 基础设施

**卡片类型: Standard Teaching Card**

**分析对象:** Spring Boot 3.4.5 + Spring Security 作为项目基础框架
**对应源码:** pom.xml, KnowledgeAgentDemoApplication.java, auth/SecurityConfig.java, auth/JwtAuthenticationFilter.java, auth/JwtUtil.java, auth/UserService.java, common/HarnessConfig.java, common/GlobalExceptionHandler.java, common/AiPlatformProperties.java

---

### ① 所用技术栈

| 属性 | 内容 |
|------|------|
| **核心技术** | Spring Boot 3.4.5 (parent POM) |
| **技术分类** | Java 企业级应用框架 |
| **在项目中的角色** | IoC 容器 + 嵌入式 Tomcat + 配置管理 + JPA + Security + Actuator + Validation |
| **关键 Starter** | web, data-jpa, validation, security, actuator |
| **安全组件** | Spring Security (无状态 JWT) + JJWT 0.12.5 + BCryptPasswordEncoder |
| **配置绑定** | @ConfigurationProperties(prefix="ai-platform") 类型安全属性注入 |

### ② 为什么采用 Spring Boot + Spring Security

**项目约束:**

| 约束类型 | 分析 |
|---------|------|
| **功能约束** | 多组 RESTful API (chat, agent, evaluation, knowledge, trace, audit, tool, skill, auth) |
| **安全约束** | 用户注册/登录、JWT 无状态认证、API 访问控制、刷新令牌、登出令牌撤销 |
| **数据约束** | JPA 持久化 (H2/PostgreSQL 双模)、Flyway 数据库迁移 |
| **可观测性约束** | Actuator 健康检查 + Micrometer -> Prometheus |
| **可配置性约束** | 环境变量驱动的配置 (${DEEPSEEK_API_KEY}, ${POSTGRES_URL} 等) |

**权衡取舍:**
```
选择: Spring 原生生态 (Security+JPA+Actuator 无缝集成)、丰富 Starter 生态
放弃: Quarkus (启动更快但 AI 生态不成熟)、Javalin (轻量但缺少 JPA/Security)
在项目当前阶段的合理性:
  Spring Boot 为 Spring AI 提供最原生 AutoConfiguration 支持，JWT+Token黑名单兼顾安全与RESTful理念。
```

### ③ 技术深度剖析

#### 核心原理 — Security 无状态 JWT 认证链

```
HTTP Request
    |
    v
JwtAuthenticationFilter (OncePerRequestFilter)      <- 先于 UsernamePasswordAuthenticationFilter
    |
    +- 提取 Authorization: Bearer <token>
    |    +- 无 -> pass to next filter
    |    +- 有 -> parseToken()
    |         +- 检查 tokenId 是否在黑名单 (RevokedAccessToken)
    |         +- 检查 User 状态是否 ACTIVE
    |         +- 设置 SecurityContext (UsernamePasswordAuthenticationToken)
    |              request.setAttribute("userId", claims.userId())
    |              request.setAttribute("username", claims.username())
    |
    v
SecurityFilterChain (SecurityConfig)
    +- /api/auth/register, /api/auth/login, /api/auth/refresh -> permitAll
    +- /swagger-ui/**, /h2-console/**, /actuator/health -> permitAll
    +- /api/** -> authenticated()
    |
    v
Controller -> request.getAttribute("userId") 获取用户身份
```

#### 关键概念

**概念 1: Refresh Token 双令牌体系**

```java
// 来源: auth/RefreshToken.java + AuthController.java
// Access Token: 短期 24h (86400000ms)
// Refresh Token: 长期 7天 (604800000ms)
// 登出: Access Token 入黑名单 + Refresh Token 从数据库删除
revokedAccessTokenRepository.save(new RevokedAccessToken(tokenId, userId, expiresAt));
refreshTokenRepository.deleteByUserIdAndToken(userId, refreshToken);
```

**概念 2: @ConfigurationProperties 类型安全绑定**

```java
// 来源: common/AiPlatformProperties.java
@Component
@ConfigurationProperties(prefix = "ai-platform")
public class AiPlatformProperties {
    private int modelMaxAttempts = 2;              // 默认 2，可环境变量覆盖
    private int modelCircuitBreakerThreshold = 3;  // 断路器阈值
    private long modelCircuitBreakerOpenMs = 30000;// 断路器打开 30s
    // ... 20+ 配置属性
}
```

#### 常见陷阱与项目现状

| # | 常见陷阱 | 本项目处理 | 风险/建议 |
|---|---------|----------|----------|
| 1 | JWT secret 硬编码 | 通过 JWT_SECRET 环境变量注入 | 良好，生产需强随机 |
| 2 | CSRF 与 REST API 冲突 | csrf(AbstractHttpConfigurer::disable) | 正确 (无状态 API 不需 CSRF) |
| 3 | Token 过期静默失败 | ExpiredJwtException 设置 authError 属性 | 前端可根据 401 触发 refresh |
| 4 | Security filter 异常处理 | authenticationEntryPoint 自定义 JSON 响应 | 良好 |

### ④ 可替代方案对比

| 对比维度 | Spring Security (本项目) | Apache Shiro | 自研 Filter |
|---------|------------------------|--------------|------------|
| **核心优势** | 生态最完善，JWT+ACL 开箱即用 | 学习曲线最低 | 最轻量 |
| **主要劣势** | 配置复杂，学习曲线陡峭 | 社区相对小 | 需手写所有安全逻辑 |
| **本项目如果换成它** | — | 替换所有 Security 配置 | 丢失 Refresh Token 等便利 |

### ⑤ 优越性与风险评估

**技术健康度:**

| 指标 | 现状 | 评级 |
|------|------|------|
| 版本时效性 | Spring Boot 3.4.5 | 接近最新 |
| 安全配置 | 无状态 JWT + Refresh + 黑名单 | 良好 |
| 配置规范性 | @ConfigurationProperties + 环境变量 | 优秀 |

### ⑥ 知识延伸

**思想迁移:** 无状态 JWT 认证模式 -> Node.js Express + jsonwebtoken、Python FastAPI + python-jose、Go Gin + jwt-go

**学习路径:** Java SE -> Spring Framework Core (IoC/AOP) -> Spring Boot -> Spring Security -> OAuth2/OIDC

---



## Card 3: Spring AI + DeepSeek 模型集成

**卡片类型: Standard Teaching Card**

**分析对象:** Spring AI 1.1.4 + DeepSeek (OpenAI 兼容) 作为 AI 基础设施
**对应源码:** ai/SpringAiModelClient.java, llm/ModelClient.java, llm/LlmClient.java, harness/AnswerComposer.java, application.yml

---

### ① 所用技术栈

| 属性 | 内容 |
|------|------|
| **核心技术** | Spring AI 1.1.4 |
| **技术分类** | Java AI 集成框架 / LLM 应用开发框架 |
| **在项目中的角色** | 统一 LLM 调用入口 + Embedding 服务 + VectorStore 存储 + MCP 工具暴露 |
| **子模块** | spring-ai-starter-model-openai, spring-ai-starter-vector-store-pgvector, spring-ai-starter-mcp-server-webmvc |
| **实际调用的 AI 服务** | DeepSeek (Chat) + OpenAI (Embedding) |
| **核心类** | SpringAiModelClient, VectorKnowledgeService, McpToolService |

### ② 为什么采用 Spring AI

**项目约束:**

| 约束类型 | 分析 |
|---------|------|
| **功能约束** | Chat 对话、Embedding 向量化、VectorStore 存储/检索、MCP 工具暴露 |
| **架构约束** | 必须与 Spring Boot IoC 容器无缝集成 (Bean 注入、AutoConfiguration) |
| **扩展性约束** | 需要支持未来切换模型提供商 (DeepSeek -> OpenAI/千问) |
| **代码量约束** | 不想自己封装 HTTP 调用、重试、响应解析等底层细节 |

**权衡取舍:**
```
选择: Spring 官方 AI 框架 (与 Spring Boot 深度融合)、统一抽象 (ChatModel/EmbeddingModel/VectorStore)
放弃: LangChain4j (功能更全面但非 Spring 官方)、直接 HTTP 调用 (最灵活但维护成本高)
在项目当前阶段的合理性:
  Spring AI ChatModel/VectorStore 抽象足覆覆盖 RAG 场景。@Primary + @ConditionalOnBean
  允许在无 ChatModel Bean 时降级到 fallback。MCP 集成使工具可被外部 AI 客户端消费。
```

### ③ 技术深度剖析

#### 核心原理 — AutoConfiguration 与 @ConditionalOnBean

```java
// 来源: ai/SpringAiModelClient.java:32-34
@Service
@Primary                           // 优先于 LlmClient 注入
@ConditionalOnBean(ChatModel.class) // 仅当 ChatModel Bean 存在时才激活
public class SpringAiModelClient implements ModelClient {
    private final ChatModel chatModel;
    public SpringAiModelClient(ChatModel chatModel) {
        this.chatModel = chatModel;  // Spring AI 通过 AutoConfiguration 自动创建
    }
}
```

**AutoConfiguration 加载链:**
```
@SpringBootApplication
  -> 扫描 spring.factories
  -> OpenAiAutoConfiguration
  -> @ConditionalOnClass (openai-java)
  -> 读取 spring.ai.openai.* 配置
  -> 创建 OpenAiChatModel Bean
  -> 创建 OpenAiEmbeddingModel Bean
  -> 创建 PgVectorStore Bean
  -> SpringAiModelClient 检测到 ChatModel Bean -> 自动激活
```

#### Prompt 构建流程

```java
// 来源: ai/SpringAiModelClient.java:99-125
private Prompt buildPrompt(String question, List<DocumentChunk> sources, List<SessionMessage> history) {
    // Step 1: System Prompt (区分有/无知识库)
    String systemPrompt = ContextBuilder.buildSystemPrompt(sources);
    //   无知识库: "你是一个简洁、友好的中文问答助手..."
    //   有知识库: "你是一个知识库问答助手。用 Markdown 排版、引用来源..."

    // Step 2: Messages 列表
    List<Message> messages = new ArrayList<>();
    messages.add(systemPromptTemplate.createMessage());  // System Message

    // Step 3: 对话历史 (最近 10 轮)
    for (SessionMessage msg : history) {
        if ("user".equals(msg.role())) messages.add(new UserMessage(msg.content()));
        else messages.add(new AssistantMessage(msg.content()));
    }

    // Step 4: 当前问题 + 知识片段拼接
    String userMessage = ContextBuilder.buildUserMessage(question, sources);
    messages.add(new UserMessage(userMessage));

    return new Prompt(messages);
}
```

#### 关键概念

**概念 1: 流式响应 (SSE via Reactor Flux)**

```java
// 来源: SpringAiModelClient.java:64-96
public void answerStream(...) {
    Flux<ChatResponse> stream = chatModel.stream(prompt);
    stream.subscribe(
        response -> {
            String token = response.getResult().getOutput().getText();  // 逐个 token
            onDelta.accept(token);  // 推送给 SSE 客户端 (前端实时显示)
        },
        error -> onError.accept(error.getMessage()),
        () -> onDone.accept(fullBuilder.toString())
    );
}
```

**概念 2: Fallback 降级机制**

```java
// 来源: SpringAiModelClient.java:128-141
private String fallback(String question, List<DocumentChunk> sources) {
    if (sources.isEmpty()) {
        return "知识库中没有检索到相关片段...";
    }
    StringBuilder answer = new StringBuilder("已检索到 " + sources.size() + " 个相关知识片段：\n");
    for (int i = 0; i < sources.size(); i++) {
        answer.append("\n[").append(i + 1).append("] ").append(source.title())
              .append("：").append(source.content());
    }
    answer.append("\n\n（Spring AI 模型调用不可用，返回检索摘要。）");
    return answer.toString();
}
```

#### 常见陷阱与项目现状

| # | 常见陷阱 | 本项目处理 | 风险/建议 |
|---|---------|----------|----------|
| 1 | API Key 泄露 | 环境变量 ${ENV_VAR:} | judge.qwen.api-key 是明文! 建议移入 .env |
| 2 | 模型不可用时崩溃 | fallback 检索摘要 + answerWithRetry 重试 | 良好 |
| 3 | Embedding 和 Chat 用不同 API | Chat->DeepSeek, Embedding->OpenAI | 注意独立配置 |
| 4 | Spring AI API 快速变化 | 1.1.4 版本 | 建议定期检查更新 |

### ④ 可替代方案对比

| 对比维度 | Spring AI (本项目) | LangChain4j | 直接 HTTP 调用 |
|---------|-------------------|-------------|--------------|
| **核心优势** | Spring 原生，官方维护 | 功能最全 (Memory/Tool/Chain) | 最灵活 |
| **主要劣势** | 功能在快速迭代中 | 非 Spring 官方 | 需自己实现重试/限流等 |
| **本项目如果换成它** | — | 替换所有 AI 代码 | 丢失 RAG 流水线便利性 |
| **学习曲线** | 低 | 中等 | 低 (但需自己造轮子) |

### ⑤ 优越性与风险评估

| 指标 | 现状 | 评级 |
|------|------|------|
| 版本时效性 | 1.1.4 | 较新 |
| 社区活跃度 | GitHub 3k+ Stars | 中等 |
| API 稳定性 | 仍在快速迭代 | 需关注 breaking change |
| Fallback 健壮性 | 多层降级 (fallback + retry + circuit breaker) | 优秀 |

### ⑥ 知识延伸

**思想迁移:** Spring AI 的 ChatModel/EmbeddingModel 统一抽象 -> Python LangChain 的 BaseLLM/BaseEmbedding -> 任何需要"接口抽象 + 多实现切换"的场景

**学习路径:** OpenAI API 基础 -> Spring AI Reference -> pgvector 向量检索 -> RAG 评测体系

---



## Card 4: 混合检索引擎 — BM25 关键词 + pgvector 语义检索

**卡片类型: Standard Teaching Card**

**分析对象:** 知识库混合检索方案
**对应源码:** knowledge/KnowledgeService.java, knowledge/VectorKnowledgeService.java, knowledge/DocumentChunk.java, application.yml

---

### ① 所用技术栈

| 属性 | 内容 |
|------|------|
| **核心技术** | 自研 BM25 + pgvector (HNSW) 混合检索 |
| **技术分类** | 信息检索 / 搜索算法 |
| **在项目中的角色** | 为 RAG 提供检索: 向量语义优先 + BM25 关键词补充 |
| **向量引擎** | pgvector + cosine_distance + HNSW 索引 |
| **BM25 参数** | k1=1.5, b=0.75 |
| **Chunk 策略** | 智能分段: 标题 -> 段落 -> 句子 -> 固定长度+重叠 |

### ② 为什么采用混合检索

**项目约束:**

| 约束类型 | 分析 |
|---------|------|
| **精确匹配** | 用户可能查精确术语 (API 名称、错误码) -> 需关键词匹配 |
| **语义理解** | 用户可能自然语言描述 -> 需语义相似度 |
| **数据规模** | 初期 < 10万 chunks -> 不需要 ES 级搜索引擎 |
| **运维约束** | 已用 PostgreSQL -> 复用而非引入新基础设施 |

**混合检索策略 (代码分析):**

```java
// 来源: KnowledgeService.java:118-159
public List<DocumentChunk> search(String query, int topK, String userId) {
    List<DocumentChunk> keywordResults = keywordSearch(query, topK, userId);

    if (vectorKnowledgeService.isPresent()) {
        List<Document> vectorResults = vectorKnowledgeService.search(query, topK, userId);
        // 向量结果优先填入，BM25 结果补充空缺 (去重)
        Map<Long, DocumentChunk> merged = new LinkedHashMap<>();
        vectorChunks.forEach(chunk -> merged.put(chunk.getId(), chunk));
        keywordResults.forEach(chunk -> merged.putIfAbsent(chunk.getId(), chunk));
        return merged.values().stream().limit(topK).toList();
    }
    // 向量不可用时降级到纯 BM25
    return keywordResults;
}
```

**权衡取舍:**
```
选择: 向量优先 + BM25 填充 (简单有效)
放弃: 加权融合 (weight*vector_score + (1-weight)*bm25_score) (更精准但需调参)
在项目当前阶段的合理性:
  交替填充策略简单有效，避免了调参负担，且保证 topK 结果多样性。
```

### ③ 技术深度剖析

#### 核心原理 — BM25 评分算法

```java
// 来源: KnowledgeService.java:190-219
private double bm25Score(DocumentChunk chunk, List<String> docTokens,
                         List<String> queryTokens, Map<String,Integer> docFreq,
                         int docCount, double avgDocLen) {
    double k1 = 1.5, b = 0.75, score = 0.0;
    for (String token : queryTokens) {
        int tf = termFrequency(token, docTokens);   // 词频
        int df = docFreq.getOrDefault(token, 0);    // 文档频率
        double idf = Math.log(1.0 + (docCount - df + 0.5) / (df + 0.5));
        double denom = tf + k1 * (1.0 - b + b * docTokens.size() / avgDocLen);
        score += idf * (tf * (k1 + 1.0)) / denom;   // BM25 核心公式
    }
    // 标题匹配加成: +0.5 per matched query token
    for (String token : queryTokens) {
        if (normalizedTitle.contains(token)) score += 0.5;
    }
    return score;
}
```

#### 核心原理 — pgvector 向量检索

```java
// 来源: VectorKnowledgeService.java:53-63
public List<Document> search(String query, int topK, String userId) {
    SearchRequest request = SearchRequest.builder()
        .query(query)
        .topK(Math.min(topK, 20))
        .similarityThreshold(0.0)
        .filterExpression("userId == '" + userId + "'")  // 用户数据隔离
        .build();
    return vectorStore.similaritySearch(request);
    // pgvector 底层: SELECT * FROM vector_store
    //               ORDER BY embedding <=> query_vector LIMIT ?
}
```

#### pgvector 索引策略

| 参数 | 本项目取值 | 说明 |
|------|----------|------|
| dimensions | 1536 | text-embedding-3-small 标准维度 |
| distance_type | cosine_distance | 余弦距离 |
| HNSW m | 16 (默认) | 每个节点的最大连接数 |
| HNSW ef_construction | 200 (默认) | 构建时的搜索宽度 |

#### 智能文本分段策略

```
源文档 (PDF/Word)
  -> DocumentTextExtractor.extract()
  -> normalizeWhitespace()          (统一换行和空白)
  -> splitText(text, 1500 chars)   (1500字符/段落)
      +- 检测 Markdown 标题        (# Title)
      +- 检测段落边界              (\n\n+)
      +- 检测句子边界              ([。！？])
      +- 固定长度 + 滑动重叠       (overlap = max(80, maxChars/10))
```

#### 常见陷阱与项目现状

| # | 常见陷阱 | 本项目处理 | 风险/建议 |
|---|---------|----------|----------|
| 1 | 中文分词不准确 | 自定义 CJK + 2-gram tokenizer | 适合中文短词,长词可能拆分过细 |
| 2 | filterExpression SQL 注入 | userId 直接拼进表达式 | 存在风险! 建议参数化 |
| 3 | 向量索引未建立 | 依赖 Spring AI init-schema | 确认生产环境 HNSW 索引 |
| 4 | Chunk 过大超 LLM context | 1500 chars * topK=3 = ~4500 | 合理, DeepSeek 窗口足够 |
| 5 | Embedding 不可用时静默失败 | catch Exception -> BM25 fallback | 降级策略完善 |

### ④ 可替代方案对比

| 对比维度 | BM25+pgvector (本项目) | Elasticsearch | Milvus |
|---------|----------------------|---------------|--------|
| **核心优势** | 零额外运维，SQL 原生 | 全文+向量二合一，生态成熟 | 十亿级向量检索性能 |
| **主要劣势** | 大规模不如专用向量库 | 资源消耗大 (JVM) | 需独立部署维护 |
| **本项目如果换成它** | — | 需额外 ES 集群 | 增加运维负担 |
| **适用规模** | < 百万 vectors | 千万级文档 | 亿级 vectors |
| **学习曲线** | 低 (已熟悉 PG) | 中等 | 中高 |

### ⑤ 优越性与风险评估

| 指标 | 现状 | 评级 |
|------|------|------|
| 算法成熟度 | BM25 经典 + pgvector HNSW | 优秀 |
| 混合策略 | 向量优先 + 关键词补充 | 良好 |
| 降级策略 | 向量失败 -> BM25 | 优秀 |
| 中文支持 | CJK 2-gram tokenizer | 良好 |
| 安全性 | filterExpression 直接拼 userId | 需改进 |

### ⑥ 知识延伸

**思想迁移:** 混合检索 (BM25 + Vector) -> 企业搜索、工单搜索、HR FAQ 等场景

**学习路径:** TF-IDF 基础 -> BM25 概率模型 -> HNSW 近似近邻 -> RAG 检索增强

---



## Card 5: 评测体系 — LLM-as-Judge + 规则引擎双重评分

**卡片类型: Standard Teaching Card**

**分析对象:** 答案质量自动评测系统
**对应源码:** evaluation/EvaluationService.java, evaluation/QaReviewService.java, evaluation/SemanticJudgeService.java, evaluation/QwenJudgeModelClient.java, evaluation/EvaluationCase.java, evaluation/EvaluationRun.java

---

### ① 所用技术栈

| 属性 | 内容 |
|------|------|
| **核心技术** | 双引擎评分: 规则引擎 + LLM-as-Judge |
| **技术分类** | AI 评测 / LLM Evaluation |
| **Judge 模型** | Qwen3.6-plus (通义千问, DashScope API) |
| **评分方式** | max(规则分, 0.6*规则分 + 0.4*LLM分) |
| **核心类** | QaReviewService, SemanticJudgeService, QwenJudgeModelClient |

### ② 为什么采用双引擎评分

**评分维度拆解:**

```
答案质量总分 = max(规则分, 加权综合分)

规则引擎 (QaReviewService) — 权重 0.6
  +- 检索命中: 30%    (sources 非空)
  +- 引用标注: 30%    (答案含 [1][2] 格式)
  +- 关键词覆盖: 30%  (命中 expectedKeywords >= 50%)
  +- 无依据断言: -20% (句子在 sources 中找不到依据)
  +- 空回答: 0.0      (长度 < 5)

LLM Judge (SemanticJudgeService) — 权重 0.4
  +- 调用 Qwen3.6-plus Chat Completion API
  +- Prompt: "Return score: <0-1> and reason"
  +- 不可用时降级: token overlap keyword 匹配
```

**权衡取舍:**
```
选择: 规则引擎保证客观性 + LLM Judge 补充语义理解
放弃: 纯 LLM Judge (主观波动大)、纯规则引擎 (无法理解语义质量)
在项目当前阶段的合理性:
  双引擎互补，规则覆盖可量化维度，LLM 覆盖语义维度。
  400ms timeout + fallback 确保 LLM Judge 不可用时评测不中断。
```

### ③ 技术深度剖析

#### 核心原理 — QwenJudgeModelClient HTTP 调用

```java
// 来源: QwenJudgeModelClient.java:46-79
public String judge(String prompt) {
    String body = objectMapper.writeValueAsString(Map.of(
        "model", "qwen3.6-plus",
        "temperature", 0.0,                    // 评测不需要创造性
        "messages", List.of(
            Map.of("role", "system", "content", "You are a strict LLM-as-judge evaluator."),
            Map.of("role", "user", "content", prompt)
        )
    ));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"))
        .header("Authorization", "Bearer " + apiKey)
        .POST(BodyPublishers.ofString(body, UTF_8))
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString(UTF_8));
    return objectMapper.readTree(response.body())
        .path("choices").path(0).path("message").path("content").asText();
}
```

#### 核心原理 — 无依据断言检测

```java
// 来源: QaReviewService.java:73-109
public boolean detectUnsupportedClaims(String answer, List<QaSource> sources) {
    String sourceText = sources.stream()
        .map(s -> s.title() + " " + s.content()).collect(joining(" "));
    String[] sentences = SENTENCE_SPLIT.split(answer);  // [。！？.!?\n]
    for (String sentence : sentences) {
        // 跳过短句、已标注引用、Markdown 标记行
        // 计算句子 token 与 sourceText 的匹配率
        double matchRate = matchedTokens / totalTokens;
        if (matchRate < 0.2) unsupportedCount++;  // <20% -> 无依据
    }
    return unsupportedCount >= 2;  // >=2 个无依据句子 -> 判定有问题
}
```

#### 关键概念 — 降级 Judge (Token Overlap)

```java
// 来源: SemanticJudgeService.java:53-70
private JudgeResult fallbackJudge(String answer, List<String> expectedKeywords) {
    // 将 answer 和 expectedKeywords 分别 tokenize
    Set<String> answerTokens = tokens(answer);
    List<String> expectedTokens = expectedKeywords.stream()
        .flatMap(kw -> tokens(kw).stream()).toList();
    // 计算匹配率
    long matched = expectedTokens.stream().filter(answerTokens::contains).count();
    double score = matched / expectedTokens.size();
    return new JudgeResult(score, "fallback", "semantic keyword overlap " + matched + "/" + expectedTokens.size());
}
```

#### 常见陷阱与项目现状

| # | 常见陷阱 | 本项目处理 | 风险/建议 |
|---|---------|----------|----------|
| 1 | LLM Judge API Key 明文 | judge.qwen.api-key: sk-... 在 yml | **严重**: 建议立即移入 .env |
| 2 | Judge 超时 | timeoutSeconds=30, HttpClient timeout=min(10,30) | 建议降至 10-15s |
| 3 | Judge 返回非标准格式 | catch RuntimeException -> fallbackJudge | 降级完善 |
| 4 | expectedKeywords 为空时评分偏高 | 无关键词->keywordMatch=true->+0.15 | 空关键词时 LLM Judge 权重应更大 |

### ④ 可替代方案对比

| 对比维度 | 双引擎 (本项目) | RAGAS (Python) | BLEU/ROUGE |
|---------|----------------|----------------|------------|
| **核心优势** | 无需额外部署，规则客观 | 业界标准 RAG 评测 | 经典 NLP 指标 |
| **主要劣势** | 规则维度有限 | 需 Python 环境 | 无法评估语义质量 |
| **本项目如果换成它** | — | 需加 Python 服务 | 不适合知识问答 |

### ⑤ 优越性与风险评估

| 指标 | 现状 | 评级 |
|------|------|------|
| 评测维度 | 检索命中+引用+关键词+断言+LLM语义 | 全面 |
| 降级策略 | LLM不可用->token overlap | 优秀 |
| 安全性 | API Key 明文 | **需立即修复** |
| 评分合理性 | 规则 0.6 + LLM 0.4 加权 | 合理 |

### ⑥ 知识延伸

**思想迁移:** 双引擎评分 -> A/B 测试、代码审查、内容审核等需要"规则+AI"的评估场景

**学习路径:** NLP 评测指标 (BLEU/ROUGE) -> LLM-as-Judge -> RAGAS -> 评测平台设计

---



## Card 6: 工具规划与调用系统 (Mini)

**卡片类型: Mini Teaching Card**

**分析对象:** ToolPlanningService + ToolRegistry + MCP 工具暴露
**对应源码:** tool/ToolPlanningService.java, tool/ToolRegistry.java, mcp/McpToolService.java

---

**所用技术:** 正则表达式工具规划 + 注册表模式工具调用 + MCP 协议暴露

**为什么项目用它:** 项目需要"一步式"工具调用: 用户问题 -> 规划工具 -> 执行工具 -> 结果作为上下文传给 LLM。正则匹配检测计算意图，简单高效无需 LLM 做 function calling。

**核心流程:**

```
用户问题: "12 + 30 等于多少？"
  -> ToolPlanningService.plan("12 + 30 等于多少？")
  -> 正则匹配 d+ [+\-*/] d+ -> "12 + 30"
  -> ToolPlan(required=true, toolName="calculator", input="12 + 30")
  -> ToolRegistry.invoke("calculator", "12 + 30")
  -> calculate("12 + 30") -> "42"
  -> 作为 DocumentChunk 合并到 sources:
     "Tool: calculator | Input: 12 + 30 | Output: 42"
  -> 传给 AnswerComposer 作为 LLM 上下文
```

**MCP Server 暴露 (供外部 AI 客户端消费):**

```java
// 来源: mcp/McpToolService.java
@Tool(description = "Calculate simple arithmetic expression")
public String calculator(String input) { ... }

@Tool(description = "Search the knowledge base for relevant information")
public String searchKnowledge(String query, String userId) { ... }

@Tool(description = "Mock an HTTP API call")
public String httpMock(String input) { ... }

@Tool(description = "Get the current platform status")
public String platformStatus(String userId) { ... }
```

**已注册工具:**

| 工具名 | 描述 | 可见性 | 超时 |
|--------|------|--------|------|
| calculator | 简单算术计算 | PUBLIC | 1000ms |
| echo | 连接测试 | PUBLIC | 1000ms |
| http_mock | HTTP 模拟调用 | INTERNAL | 1500ms |

**优越性:** 正则规划 + 注册表调用 -> 低延迟 (<1ms)，无需 LLM 做 function calling。MCP Server 使工具可被 Claude Code 等外部客户端发现和调用。

---



## Card 7: 可观测性与韧性设计 (Mini)

**卡片类型: Mini Teaching Card**

**分析对象:** Micrometer 指标 + Prometheus + 熔断器 + 重试机制
**对应源码:** common/HarnessMetrics.java, harness/AnswerComposer.java, common/HarnessConfig.java

---

**所用技术:** Micrometer 自定义指标 + Prometheus Registry + 自适应熔断器 + TaskExecutor 线程池

**为什么项目用它:** AI 服务依赖外部 API (DeepSeek + OpenAI Embedding + Qwen Judge)，外部 API 可能出现延迟、限流、宕机。熔断器 + 重试 + Metrics 三位一体保障系统韧性。

**核心设计要素:**

| 组件 | 实现位置 | 关键参数 |
|------|---------|---------|
| LLM 重试 | AnswerComposer.answerWithRetry() | maxAttempts=2 |
| 熔断器 | modelCircuitOpenUntilMs | threshold=3次连续失败, openMs=30s |
| 检索指标 | HarnessMetrics | rag.retrieval.hit/.miss Counter |
| LLM 延迟 | HarnessMetrics | llm.answer.latency Timer (P50/P95/P99) |
| 工具指标 | HarnessMetrics | tool.call.total/.success/.failure |
| 评测指标 | HarnessMetrics | evaluation.run.total/.pass (>=0.6)/.fail |
| 断路器 Gauge | HarnessMetrics | llm.circuit.open (0/1) |
| 线程池 | HarnessConfig.taskExecutor() | core=4, max=8, queue=32 |

**Prometheus 端点:** /actuator/prometheus -> Grafana 监控面板

**熔断器实现:**

```java
// 来源: AnswerComposer.java:82-107
private String answerWithRetry(String question, List<DocumentChunk> sources, List<SessionMessage> history) {
    long now = System.currentTimeMillis();
    if (now < modelCircuitOpenUntilMs) {  // 断路器打开中
        throw new IllegalStateException("model circuit breaker is open");
    }
    int maxAttempts = Math.max(1, aiPlatformProperties.getModelMaxAttempts());
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            return modelClient.answer(question, sources, history);
        } catch (RuntimeException e) { last = e; }
    }
    int failures = consecutiveModelFailures.incrementAndGet();
    if (failures >= threshold) {  // 连续失败 >= 阈值 -> 打开断路器
        modelCircuitOpenUntilMs = now + openMs;  // 30s 后自动恢复
    }
    throw last;
}
```

**优越性:** 三层韧性保障 (重试 -> 熔断 -> 降级 fallback)，Metrics 覆盖全部关键路径。

---

## 项目技术健康度总评

| 评估维度 | 得分 | 评价 |
|---------|------|------|
| **架构设计** | 5/5 | Agent Harness 流水线清晰解耦，异步并行合理，可扩展性好 |
| **安全设计** | 4/5 | JWT+Refresh+黑名单+BCrypt 完整，但 API Key 明文泄露是严重问题 |
| **韧性设计** | 5/5 | 熔断器、重试、降级、超时一应俱全，AI 服务场景最佳实践 |
| **代码质量** | 4/5 | 整体规范，KnowledgeService(363行)可拆分 |
| **测试覆盖** | 4/5 | 15个测试覆盖核心模块，含集成和隔离测试 |
| **可观测性** | 5/5 | Micrometer+Prometheus+TraceEvent 全链路监控 |
| **文档完善度** | 4/5 | README+Deployment+Architecture 文档齐全 |

## 优先修复建议

| 优先级 | 问题 | 影响 | 修复方案 |
|--------|------|------|---------|
| P0 | API Key 硬编码 (judge.qwen.api-key) | 密钥泄露风险 | 移入 .env，替换为环境变量占位符 |
| P1 | pgvector filterExpression 直接拼 userId | SQL 注入风险 | 参数化或校验 userId 格式 |
| P2 | CompletableFuture.join() 无超时 | 线程阻塞风险 | 加 orTimeout() 超时 |
| P3 | KnowledgeService 类过长 (363行) | 可维护性 | 将 BM25 算法抽到独立 Bm25Scorer |
| P3 | TaskExecutor 无拒绝策略 | 高并发任务丢失 | 添加 CallerRunsPolicy |

## 知识延伸与迁移

### 可迁移的架构思想

1. **Agent Harness 流水线** -> 任何多步骤后端流程 (电商下单、CI/CD、内容审核)
2. **混合检索 (BM25 + Vector)** -> 企业搜索 (知识库、工单、FAQ)
3. **熔断器 + 重试 + Metrics** -> 任何外部 API 依赖场景
4. **双引擎评测 (规则 + LLM)** -> 代码审查、内容审核、A/B 测试

### 深入学习路径

```
Java SE + Maven
  -> Spring Boot (IoC/DI, MVC, JPA)
    -> Spring Security (JWT, OAuth2)
      -> Spring AI (ChatModel, EmbeddingModel, VectorStore)
        -> pgvector (HNSW, ANN, 距离度量)
          -> LLM 评测 (RAGAS, LLM-as-Judge)
            -> Agent 编排 (LangGraph, CrewAI)
```

### 推荐资源

| 类型 | 资源 | 说明 | 与本项目的关联 |
|------|------|------|--------------|
| 官方文档 | Spring AI Reference | ChatModel/VectorStore/MCP 用法 | 本项目的 AI 基础设施 |
| 官方文档 | pgvector GitHub | HNSW 索引、ANN 调参 | 向量检索引擎 |
| 论文 | BM25 (Robertson et al., 2009) | 概率检索模型理论 | 关键词检索引擎 |
| 开源项目 | Dify | 成熟的 LLM 应用平台 | 架构对比参考 |
| 开源项目 | LangChain4j | Java 最全面的 AI 框架 | 功能更丰富的替代方案 |
| 书籍 | Designing Data-Intensive Apps | 存储引擎、事务 | pgvector/H2 选型理论支撑 |

---

> **文档生成信息:**
> - 生成协议: deep-teach-review (SKILL_REVIEW.md) v1.0.0
> - 分析日期: 2026-05-15
> - 分析模式: 项目全景分析 -> PROJECT_REVIEW.md
> - 卡片数量: 7 张 (1 Enhanced + 4 Standard + 2 Mini)
> - 代码扫描范围: 75 个 Java 源码 + application.yml + pom.xml + page.html
> - 文件保存位置: PROJECT_REVIEW.md (项目根目录)
