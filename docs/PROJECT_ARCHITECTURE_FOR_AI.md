# Knowledge Agent Platform 项目架构速读

> 面向代码模型/协作模型的快速上下文文档。目标是在 3-5 分钟内理解项目职责、请求链路、主要模块和常见扩展点。

## 1. 项目定位

Knowledge Agent Platform 是一个基于 Spring Boot 3 的知识库问答与 Agent Harness 演示平台。

它把一次问答拆成几个可观察的阶段：

```text
用户请求 -> 会话记忆 -> 知识检索 -> Prompt/上下文构建 -> 模型回答 -> QA 检查 -> Trace 记录
```

项目同时提供：

- JWT 登录注册与用户级数据隔离
- 文档片段管理、文件上传解析、关键词检索和可选 pgvector 向量检索
- 同步问答和 SSE 流式问答
- 本地工具注册/调用和 Spring AI MCP Tool 暴露
- Trace、Session、Evaluation、QA Review 等 Agent Harness 周边能力
- 静态首页和 Swagger/OpenAPI 接口文档

## 2. 技术栈与运行方式

- Java 17
- Spring Boot 3.2.5
- Spring MVC + Jakarta Validation
- Spring Data JPA
- 默认 H2 文件数据库：`./data/knowledge-agent-platform`
- 可选 PostgreSQL + pgvector：见 `docker-compose.yml`
- Spring AI：OpenAI-Compatible ChatModel、pgvector VectorStore、MCP Server
- DeepSeek/OpenAI-Compatible 手写 HTTP 客户端兜底
- Micrometer + Actuator + Prometheus
- PDFBox、Apache POI：文件文本抽取

关键配置：

- [`pom.xml`](../pom.xml)：依赖和 Java/Spring AI 版本
- [`src/main/resources/application.yml`](../src/main/resources/application.yml)：数据库、模型、MCP、Actuator、端口
- [`.env.example`](../.env.example)：本地环境变量示例
- [`docker-compose.yml`](../docker-compose.yml)：PostgreSQL/pgvector + 应用容器

本地启动：

```powershell
mvn spring-boot:run
```

默认访问：

- 首页：`http://localhost:8081/`
- Swagger：`http://localhost:8081/swagger-ui/index.html`
- H2 Console：`http://localhost:8081/h2-console`

## 3. 目录地图

```text
src/main/java/com/liujianan/agentdemo
├─ KnowledgeAgentDemoApplication.java   # Spring Boot 启动类
├─ auth/                                # 注册、登录、JWT、接口鉴权、用户实体
├─ chat/                                # /api/chat 问答入口，委托 HarnessOrchestrator
├─ common/                              # 统一响应、异常处理、dotenv、指标、配置
├─ knowledge/                           # 文档片段、上传解析、检索、向量索引
├─ harness/                             # 核心编排、会话、Trace、检索 Agent、回答组合器
├─ llm/                                 # ModelClient 接口和手写 DeepSeek/OpenAI HTTP 客户端
├─ ai/                                  # Spring AI ChatModel 适配器
├─ tool/                                # 本地工具定义、注册表、工具调用接口
├─ mcp/                                 # Spring AI @Tool MCP 暴露
├─ evaluation/                          # 评测样例、运行评测、回答质量检查
├─ agent/                               # agents/*.md 元信息读取接口
├─ skill/                               # skills/*/SKILL.md 元信息读取接口
└─ home/                                # 静态首页路由
```

资源目录：

```text
src/main/resources
├─ application.yml
├─ agents/                              # answer/retrieval/qa/tool agent 描述文件
├─ skills/                              # document-ingestion/rag-answer/... 技能说明
└─ META-INF/spring/...                  # EnvFilePostProcessor 注册
```

测试目录按模块划分在 `src/test/java/com/liujianan/agentdemo/**`，包含知识库、向量服务、MCP、Harness、评测、认证隔离等测试。

## 4. 认证与用户隔离

入口：

- [`AuthController`](../src/main/java/com/liujianan/agentdemo/auth/AuthController.java)：`/api/auth/register`、`/api/auth/login`
- [`WebMvcConfig`](../src/main/java/com/liujianan/agentdemo/auth/WebMvcConfig.java)：拦截 `/api/**`，排除 `/api/auth/**`
- [`AuthInterceptor`](../src/main/java/com/liujianan/agentdemo/auth/AuthInterceptor.java)：解析 `Authorization: Bearer <token>`，把 `userId` 写入 request attribute
- [`JwtUtil`](../src/main/java/com/liujianan/agentdemo/auth/JwtUtil.java)：JWT 生成与解析

业务层基本都显式接收 `String userId`，并通过 repository 的 `findByUserId...` 方法做数据隔离。修改任何查询、删除、检索逻辑时，应优先确认是否保留了 `userId` 过滤。

## 5. 问答主链路

同步入口：

```text
ChatController.post /api/chat
  -> ChatService.answer(...)
  -> HarnessOrchestrator.answer(...)
```

流式入口：

```text
ChatController.post /api/chat/stream
  -> ChatService.answerStream(...)
  -> HarnessOrchestrator.answerStream(...)
  -> SseEmitter 分段输出 session/sources/delta/done
```

核心编排在 [`HarnessOrchestrator`](../src/main/java/com/liujianan/agentdemo/harness/HarnessOrchestrator.java)：

1. `SessionService.append(...)` 写入用户消息；无 `sessionId` 时创建新会话。
2. `TraceAgent.record(..., "USER_INPUT", ...)` 记录输入事件。
3. `RetrievalAgent.search(...)` 从知识库取 Top-K 片段。
4. 记录 `CONTEXT_BUILD` Trace。
5. `AnswerComposer.compose(...)` 或 `composeStream(...)` 调用模型客户端生成回答。
6. `SessionService.append(...)` 写入 assistant 消息。
7. `performQaReview(...)` 做轻量质量检查并记录 `QA_REVIEW` Trace。
8. 返回 `ChatResponse(sessionId, answer, sources, promptTokens, latencyMs)`。

模型回答的最终调用点是 [`AnswerComposer`](../src/main/java/com/liujianan/agentdemo/harness/AnswerComposer.java)，它只依赖抽象接口 [`ModelClient`](../src/main/java/com/liujianan/agentdemo/llm/ModelClient.java)。

## 6. LLM 适配层

项目有两个 `ModelClient` 实现：

- [`SpringAiModelClient`](../src/main/java/com/liujianan/agentdemo/ai/SpringAiModelClient.java)
  - `@Primary`
  - `@ConditionalOnBean(ChatModel.class)`
  - 使用 Spring AI `ChatModel`
  - 负责把历史消息、系统提示词、知识片段组装成 Spring AI `Prompt`

- [`LlmClient`](../src/main/java/com/liujianan/agentdemo/llm/LlmClient.java)
  - 手写 HTTP 调用 OpenAI-Compatible `/chat/completions`
  - 读取 `deepseek.api-key/base-url/model`
  - 未配置 API Key 时返回检索摘要兜底

Prompt 构造辅助：

- [`ContextBuilder`](../src/main/java/com/liujianan/agentdemo/harness/ContextBuilder.java)：构建系统提示词和用户消息

如果要更换模型供应商，优先新增或替换 `ModelClient` 实现，尽量不要改 `HarnessOrchestrator`。

## 7. 知识库与检索

主要文件：

- [`KnowledgeController`](../src/main/java/com/liujianan/agentdemo/knowledge/KnowledgeController.java)：`/api/documents` CRUD、上传、搜索
- [`KnowledgeService`](../src/main/java/com/liujianan/agentdemo/knowledge/KnowledgeService.java)：文档新增、上传切块、删除、检索
- [`DocumentTextExtractor`](../src/main/java/com/liujianan/agentdemo/knowledge/DocumentTextExtractor.java)：解析 `.txt`、`.md`、`.pdf`、`.docx`
- [`DocumentChunk`](../src/main/java/com/liujianan/agentdemo/knowledge/DocumentChunk.java)：文档片段实体
- [`VectorKnowledgeService`](../src/main/java/com/liujianan/agentdemo/knowledge/VectorKnowledgeService.java)：可选向量索引和相似度搜索

检索策略：

1. 如果存在 `VectorStore` Bean，则优先走 `VectorKnowledgeService.search(query, topK, userId)`。
2. 向量检索结果通过 metadata 中的 `chunkId` 映射回 `DocumentChunk`。
3. 向量检索失败或没有结果时，回退到 `keywordSearch(...)`。
4. 关键词检索只搜索当前用户的 `DocumentChunk`，基于 token/bigram/全文包含做简单打分。

上传切块：

- 标题为空时使用文件名
- 默认 tag 为 `upload`
- 先按 Markdown 标题拆，再按段落拆，长段落继续按句子/固定长度拆
- 单片段目标长度约 1500 字符
- 写入 JPA 后尝试同步索引到向量库，失败只记录 warning，不中断主流程

## 8. 会话、Trace 与可观察性

会话：

- [`SessionService`](../src/main/java/com/liujianan/agentdemo/harness/SessionService.java)：创建、追加消息、列表、删除
- [`ChatSession`](../src/main/java/com/liujianan/agentdemo/harness/ChatSession.java)：会话实体
- [`SessionMessage`](../src/main/java/com/liujianan/agentdemo/harness/SessionMessage.java)：消息值对象
- [`SessionController`](../src/main/java/com/liujianan/agentdemo/harness/SessionController.java)：`/api/sessions`

Trace：

- [`TraceAgent`](../src/main/java/com/liujianan/agentdemo/harness/TraceAgent.java)：编排层使用的记录门面
- [`TraceRecorder`](../src/main/java/com/liujianan/agentdemo/harness/TraceRecorder.java)：写入和查询 Trace
- [`TraceEvent`](../src/main/java/com/liujianan/agentdemo/harness/TraceEvent.java)：Trace 实体
- [`TraceController`](../src/main/java/com/liujianan/agentdemo/harness/TraceController.java)：`/api/traces`

常见 Trace stage：

- `USER_INPUT`
- `RETRIEVAL`
- `CONTEXT_BUILD`
- `ANSWER`
- `QA_REVIEW`

指标：

- [`HarnessMetrics`](../src/main/java/com/liujianan/agentdemo/common/HarnessMetrics.java)：记录检索命中、LLM 延迟、工具失败、评测分数等
- Actuator 暴露：`/actuator/health`、`/actuator/metrics`、`/actuator/prometheus`

## 9. 工具调用与 MCP

本地工具：

- [`ToolRegistry`](../src/main/java/com/liujianan/agentdemo/tool/ToolRegistry.java)
  - `echo`
  - `calculator`
  - `http_mock`
- [`ToolController`](../src/main/java/com/liujianan/agentdemo/tool/ToolController.java)：`/api/tools`、`/api/tools/{name}/invoke`

MCP 暴露：

- [`McpToolService`](../src/main/java/com/liujianan/agentdemo/mcp/McpToolService.java)
  - `calculator`
  - `echo`
  - `searchKnowledge`
  - `httpMock`
  - `platformStatus`

MCP 使用 Spring AI `@Tool` 注解暴露方法，服务端开关在 `application.yml` 的 `spring.ai.mcp.server` 下。新增工具时，通常需要同时考虑：

1. 是否只是 Web API 工具：改 `ToolRegistry` 即可。
2. 是否也要给 MCP 客户端发现：在 `McpToolService` 增加 `@Tool` 方法。
3. 是否需要记录 Trace/指标：在调用路径中补充 `TraceAgent` 或 `HarnessMetrics`。

## 10. 评测与 QA Review

主要文件：

- [`EvaluationController`](../src/main/java/com/liujianan/agentdemo/evaluation/EvaluationController.java)：`/api/evaluations`
- [`EvaluationService`](../src/main/java/com/liujianan/agentdemo/evaluation/EvaluationService.java)：新增评测样例、运行评测
- [`QaReviewController`](../src/main/java/com/liujianan/agentdemo/evaluation/QaReviewController.java)：`/api/qa/review`
- [`QaReviewService`](../src/main/java/com/liujianan/agentdemo/evaluation/QaReviewService.java)：回答质量检查逻辑

QA Review 当前是规则式检查，主要维度：

- 是否命中知识库 sources
- 回答是否包含 `[数字]` 形式引用
- 是否覆盖期望关键词
- 是否存在疑似无来源断言
- 汇总成 0-1 分数

`HarnessOrchestrator.performQaReview(...)` 目前内置一个更轻量的 Trace 记录版检查；`EvaluationService` 复用 `QaReviewService` 做更完整的结构化评测。

## 11. API 概览

除 `/api/auth/**` 外，`/api/**` 默认都需要 JWT。

```text
POST   /api/auth/register
POST   /api/auth/login

GET    /api/documents
POST   /api/documents
POST   /api/documents/upload
GET    /api/documents/search?q=...&topK=...
DELETE /api/documents/{documentId}

POST   /api/chat
POST   /api/chat/stream

POST   /api/sessions
GET    /api/sessions
DELETE /api/sessions/{sessionId}

GET    /api/traces
GET    /api/runs

GET    /api/tools
POST   /api/tools/{name}/invoke

GET    /api/agents
GET    /api/skills

GET    /api/evaluations
POST   /api/evaluations
POST   /api/evaluations/run

POST   /api/qa/review
```

## 12. 数据模型速览

核心实体：

- `User`：登录用户
- `DocumentChunk`：知识库片段，包含 title/content/tags/createdAt/userId
- `ChatSession`：会话，保存 `SessionMessage` 列表和 userId
- `TraceEvent`：Trace 事件，包含 sessionId/stage/message/attributes/userId
- `EvaluationCase`：评测样例，包含 question/expectedKeywords/feedback/userId
- `HarnessRun`：运行记录视图相关实体

JSON/集合字段通过 JPA converter 保存，相关类包括：

- [`JsonMapConverter`](../src/main/java/com/liujianan/agentdemo/harness/JsonMapConverter.java)
- 各实体内部或相关 converter

## 13. 模型修改代码时的优先阅读路径

处理问答行为：

1. [`ChatController`](../src/main/java/com/liujianan/agentdemo/chat/ChatController.java)
2. [`HarnessOrchestrator`](../src/main/java/com/liujianan/agentdemo/harness/HarnessOrchestrator.java)
3. [`RetrievalAgent`](../src/main/java/com/liujianan/agentdemo/harness/RetrievalAgent.java)
4. [`AnswerComposer`](../src/main/java/com/liujianan/agentdemo/harness/AnswerComposer.java)
5. [`ModelClient`](../src/main/java/com/liujianan/agentdemo/llm/ModelClient.java)

处理知识库/上传/检索：

1. [`KnowledgeController`](../src/main/java/com/liujianan/agentdemo/knowledge/KnowledgeController.java)
2. [`KnowledgeService`](../src/main/java/com/liujianan/agentdemo/knowledge/KnowledgeService.java)
3. [`DocumentTextExtractor`](../src/main/java/com/liujianan/agentdemo/knowledge/DocumentTextExtractor.java)
4. [`VectorKnowledgeService`](../src/main/java/com/liujianan/agentdemo/knowledge/VectorKnowledgeService.java)

处理鉴权或数据隔离：

1. [`AuthController`](../src/main/java/com/liujianan/agentdemo/auth/AuthController.java)
2. [`AuthInterceptor`](../src/main/java/com/liujianan/agentdemo/auth/AuthInterceptor.java)
3. [`WebMvcConfig`](../src/main/java/com/liujianan/agentdemo/auth/WebMvcConfig.java)
4. 目标业务 service/repository 的 `userId` 查询条件

处理工具/MCP：

1. [`ToolRegistry`](../src/main/java/com/liujianan/agentdemo/tool/ToolRegistry.java)
2. [`ToolController`](../src/main/java/com/liujianan/agentdemo/tool/ToolController.java)
3. [`McpToolService`](../src/main/java/com/liujianan/agentdemo/mcp/McpToolService.java)

处理评测：

1. [`EvaluationService`](../src/main/java/com/liujianan/agentdemo/evaluation/EvaluationService.java)
2. [`QaReviewService`](../src/main/java/com/liujianan/agentdemo/evaluation/QaReviewService.java)
3. 对应 controller 和 test

## 14. 常见扩展点

新增模型供应商：

- 新增 `ModelClient` 实现，或调整 `SpringAiModelClient` 配置。
- 保持 `AnswerComposer` 只依赖 `ModelClient`。
- 补充同步和流式两种调用路径。

升级检索：

- 在 `KnowledgeService.search(...)` 中调整优先级或融合策略。
- 如果使用向量库，优先改 `VectorKnowledgeService`。
- 注意所有检索必须按 `userId` 隔离。

新增文件类型：

- 修改 `DocumentTextExtractor.extract(...)`。
- 增加对应解析依赖和测试。
- 确认上传切块仍能处理空文本、长文本和异常文件。

新增工具：

- 在 `ToolRegistry` 增加工具定义和 handler。
- 在 `McpToolService` 增加 `@Tool` 方法。
- 在 `ToolControllerTest` 或 MCP 测试中补验证。

新增 Trace 阶段：

- 在业务关键点注入或使用 `TraceAgent.record(...)`。
- stage 名称建议使用大写下划线，如 `TOOL_CALL`。
- attributes 保持小而结构化，避免塞入大段正文。

## 15. 测试与验证

常用命令：

```powershell
mvn test
```

仓库还提供：

```powershell
.\run_tests.ps1
```

重点测试文件：

- `KnowledgeServiceTest`
- `VectorKnowledgeServiceTest`
- `RetrievalAgentTest`
- `HarnessOrchestratorTest`
- `AnswerComposerTest`
- `McpToolServiceTest`
- `EvaluationServiceTest`
- `QaReviewControllerTest`
- `UserDataIsolationTest`

修改风险较高的区域：

- 用户数据隔离：一定跑相关测试或补测试。
- 流式回答：注意回调完成、错误回退和 session 写入。
- 向量检索：注意没有 pgvector/embedding 配置时的降级行为。
- 文档上传：注意大文件、空文件、乱码、长段落切块。

## 16. 已知代码阅读提示

- 这是一个后端为主、静态页面为辅的项目；业务核心不在前端页面，而在 `harness/knowledge/llm/evaluation/tool`。
- `SpringAiModelClient` 有 `@Primary`，当 Spring AI `ChatModel` 存在时会优先使用；否则使用手写 `LlmClient`。
- `VectorKnowledgeService` 有 `@ConditionalOnBean(VectorStore.class)`，没有向量库 Bean 时不会启用。
- `.env` 会被 [`EnvFilePostProcessor`](../src/main/java/com/liujianan/agentdemo/common/EnvFilePostProcessor.java) 加载到 Spring Environment。
- README 或部分源码注释在某些终端编码下可能显示为乱码；判断逻辑时以 Java 结构、测试和配置为准。
