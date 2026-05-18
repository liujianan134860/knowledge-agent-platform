# Knowledge Agent Platform 项目企业化改造文档

## 1. 改造背景

本项目原本是一个知识库问答与 Agent 工具调用 Demo。为了匹配飞书多维表格 AI / Agent 后端实习岗位的面试要求，本轮改造重点放在后端工程化、RAG 链路、认证鉴权、评测体系、模型调用稳定性和可观测性上。

当前项目暂时不做完整前后端分离。前端仍由 Spring Boot `static/index.html` 托管，主要用于演示和调试；后端接口已经按 REST API、JWT、DTO、分页、统一响应等方式 API 化，后续可以平滑拆出独立 React/Vue 前端。

## 2. 已完成的核心改造

### 2.1 后端工程化

已完成：

- 将 Spring Boot 升级到 `3.4.5`。
- 将原本 Controller 中的大段 HTML 拆出到 `src/main/resources/static/index.html`。
- `HomeController` 只负责转发首页，避免 Java 字符串常量过大。
- 引入 Flyway 管理数据库 schema。
- 将 `spring.jpa.hibernate.ddl-auto` 改为 `validate`，避免生产环境自动改表。
- 增加 DTO，核心接口不再直接暴露 Entity。
- 增加分页响应 `PageResponse`。
- 增加统一响应结构 `ApiResponse`。
- 增加统一错误码、`requestId`、时间戳。
- 增加 `RequestIdFilter`，每次请求都返回 `X-Request-Id`。
- 修复主代码和资源目录中的明显编码乱码问题。

代表文件：

- `src/main/java/com/liujianan/agentdemo/common/ApiResponse.java`
- `src/main/java/com/liujianan/agentdemo/common/PageResponse.java`
- `src/main/java/com/liujianan/agentdemo/common/RequestIdFilter.java`
- `src/main/resources/db/migration/V1__init_schema.sql`
- `src/main/resources/static/index.html`

### 2.2 认证与权限控制

已完成：

- 从自定义 MVC Interceptor 改为 Spring Security。
- 使用 JWT 进行无状态认证。
- 增加 `JwtAuthenticationFilter`。
- 增加 `SecurityConfig`，统一保护 `/api/**`。
- 用户密码使用 `BCryptPasswordEncoder`。
- 用户增加角色 `UserRole`：`USER` / `ADMIN`。
- 用户增加状态 `UserStatus`：`ACTIVE` / `DISABLED`。
- JWT 中携带用户角色。
- 禁用用户无法通过认证。
- 增加 refresh token。
- refresh token 支持轮换。
- logout 后 access token 会进入黑名单。
- 增加审计日志 `audit_log`。
- 审计注册、登录、登出、token refresh、评测运行等行为。
- 管理员审计日志接口使用 `@PreAuthorize("hasRole('ADMIN')")`。

代表文件：

- `src/main/java/com/liujianan/agentdemo/auth/SecurityConfig.java`
- `src/main/java/com/liujianan/agentdemo/auth/JwtAuthenticationFilter.java`
- `src/main/java/com/liujianan/agentdemo/auth/UserService.java`
- `src/main/java/com/liujianan/agentdemo/auth/RefreshToken.java`
- `src/main/java/com/liujianan/agentdemo/auth/RevokedAccessToken.java`
- `src/main/java/com/liujianan/agentdemo/audit/AuditService.java`
- `src/main/resources/db/migration/V3__auth_tokens_and_indexes.sql`

后续可继续增强：

- 工作空间级权限：`workspace_id` / `tenant_id`。
- 文档级权限：`PRIVATE` / `TEAM` / `PUBLIC`。
- 团队角色：`OWNER` / `ADMIN` / `MEMBER` / `VIEWER`。
- 用户管理接口：禁用用户、修改角色、重置密码。
- 细粒度权限注解：`@PreAuthorize("@permission.canReadDocument(...)")`。

### 2.3 数据模型与持久化

已完成：

- 使用 Flyway 初始化和演进表结构。
- 为核心查询字段增加索引。
- 将聊天消息从 `ChatSession` 内嵌集合拆为独立 `chat_message` 表。
- 评测结果落库到 `evaluation_run`。
- 审计日志落库到 `audit_log`。
- refresh token 和 access token 黑名单落库。
- 所有核心数据都带 `userId` 做用户隔离。

代表文件：

- `src/main/java/com/liujianan/agentdemo/harness/ChatMessage.java`
- `src/main/java/com/liujianan/agentdemo/evaluation/EvaluationRun.java`
- `src/main/resources/db/migration/V2__enterprise_features.sql`
- `src/main/resources/db/migration/V3__auth_tokens_and_indexes.sql`

## 3. RAG 链路改造

### 3.1 文档解析与切分

已完成：

- 支持 TXT、Markdown、PDF、Word 文档解析。
- 文档上传后进行文本标准化。
- 使用多级切分策略：
  - Markdown 标题切分。
  - 段落切分。
  - 句子切分。
  - 固定长度 + overlap 切分。
- 每个 chunk 保存标题、内容、标签、用户 ID、创建时间。

代表文件：

- `src/main/java/com/liujianan/agentdemo/knowledge/DocumentTextExtractor.java`
- `src/main/java/com/liujianan/agentdemo/knowledge/KnowledgeService.java`

### 3.2 Embedding 接入

已完成：

- Embedding 已切换到阿里云百炼。
- 默认模型：`text-embedding-v4`。
- 默认维度：`1536`。
- 通过 Spring AI `VectorStore` 自动调用 embedding API。
- 文档入库时调用 `vectorStore.add(...)`，内部完成文本向量化并写入 pgvector。
- 用户提问时调用 `vectorStore.similaritySearch(...)`，内部完成 query 向量化并检索。
- 增加 `EmbeddingConfigurationValidator`，启动时校验 embedding 配置和向量维度是否一致。

当前配置：

```yaml
spring:
  ai:
    openai:
      embedding:
        base-url: ${EMBEDDING_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}
        api-key: ${EMBEDDING_API_KEY:${BAILIAN_API_KEY:${QWEN_API_KEY:}}}
        options:
          model: ${EMBEDDING_MODEL:text-embedding-v4}
          dimensions: ${EMBEDDING_DIMENSIONS:1536}
```

代表文件：

- `src/main/java/com/liujianan/agentdemo/knowledge/VectorKnowledgeService.java`
- `src/main/java/com/liujianan/agentdemo/knowledge/EmbeddingConfigurationValidator.java`
- `src/main/resources/application.yml`

### 3.3 混合检索

已完成：

- 实现 hybrid retrieval。
- 向量召回负责语义相似度检索。
- BM25 风格关键词召回负责精确词匹配和兜底。
- 向量召回和关键词召回结果通过 `chunkId` 去重融合。
- 当前融合策略是 vector-first，再用 BM25 结果补齐候选池。
- 向量检索失败时自动回退到 BM25。

代表文件：

- `src/main/java/com/liujianan/agentdemo/knowledge/KnowledgeService.java`
- `src/main/java/com/liujianan/agentdemo/knowledge/VectorKnowledgeService.java`

### 3.4 Rerank 重排

已完成：

- 接入阿里云百炼重排模型。
- 默认模型：`qwen3-rerank`。
- 重排位置：向量召回 + BM25 召回融合之后，进入 prompt 之前。
- 重排 API Key 复用 `QWEN_API_KEY`，也可以单独配置 `BAILIAN_API_KEY`。
- 重排失败或未配置时，自动回退到原有融合顺序。

当前配置：

```yaml
rerank:
  bailian:
    api-key: ${BAILIAN_API_KEY:${QWEN_API_KEY:}}
    base-url: ${BAILIAN_RERANK_BASE_URL:https://dashscope.aliyuncs.com/compatible-api/v1}
    model: ${BAILIAN_RERANK_MODEL:qwen3-rerank}
```

代表文件：

- `src/main/java/com/liujianan/agentdemo/knowledge/RerankClient.java`
- `src/main/java/com/liujianan/agentdemo/knowledge/AliyunBailianRerankClient.java`
- `src/main/java/com/liujianan/agentdemo/knowledge/RerankResult.java`

### 3.5 答案生成

已完成：

- 主回答模型仍使用 DeepSeek。
- `AnswerComposer` 统一负责最终回答生成。
- Prompt 中注入 sources、历史会话、版本信息。
- 模型调用支持重试。
- 模型调用支持轻量级熔断。
- 熔断状态接入 Micrometer gauge。

代表文件：

- `src/main/java/com/liujianan/agentdemo/harness/AnswerComposer.java`
- `src/main/java/com/liujianan/agentdemo/llm/ModelClient.java`
- `src/main/java/com/liujianan/agentdemo/ai/SpringAiModelClient.java`

## 4. Agent 与工具调用

已完成：

- 增加 `ToolPlanningService`。
- 当前采用规则规划工具调用。
- 简单算术类问题会规划调用 `calculator`。
- 工具调用结果会包装成伪 `DocumentChunk` 加入 sources。
- 主链路中检索和工具规划并行执行。
- 每个阶段都记录 trace。

代表文件：

- `src/main/java/com/liujianan/agentdemo/tool/ToolPlanningService.java`
- `src/main/java/com/liujianan/agentdemo/tool/ToolPlan.java`
- `src/main/java/com/liujianan/agentdemo/harness/HarnessOrchestrator.java`

后续可继续增强：

- 使用 LLM 进行工具规划。
- 支持多工具调用。
- 增加工具参数 schema 校验。
- 增加工具调用超时、重试、权限控制。

## 5. 引用溯源

已完成：

- 回答接口返回 `sources`。
- Prompt 要求模型在关键结论后使用 `[1]`、`[2]` 标注引用。
- QA Review 会检查答案中是否存在 citation。
- 工具结果也可以作为 source 进入回答上下文。

当前不足：

- `DocumentChunk` 还没有保存页码、段落位置、原文偏移。
- 引用目前主要是 chunk 级，不是 page/paragraph/offset 级。
- 没有实现点击引用跳转原文。
- 没有对每个 citation 做严格一致性校验。

后续计划：

- 增加 `document_id`、`chunk_index`、`page_number`、`start_offset`、`end_offset`。
- 上传文件时保存原始文件 metadata 和 hash。
- 返回结构化 citation。
- 生成后校验 `[n]` 是否存在、是否越界、是否支持对应句子。

## 6. 缓存策略

已完成：

- 当前没有系统性缓存。
- 已有的是模型调用重试、熔断、检索 fallback 和数据库持久化。

建议后续增加：

- Query embedding 缓存。
- Rerank 缓存：`query + candidateIds + rerankModelVersion`。
- 检索结果缓存：`userId + query + topK + knowledgeVersion`。
- 文档解析缓存：基于文件 hash 避免重复解析。
- FAQ 型 answer 缓存。
- 本地 Caffeine 缓存。
- 分布式 Redis 缓存。
- 文档新增/删除后通过 `knowledgeVersion` 失效缓存。

面试表述：

> 当前项目优先保证 RAG 链路正确性和可观测性，缓存层尚未系统接入。后续我会优先缓存 embedding、rerank 和 retrieval 结果，并通过知识库版本号控制缓存失效。

## 7. 评测体系

已完成：

- 增加 `EvaluationCase` 管理评测用例。
- 增加 `EvaluationRun` 保存每次评测结果。
- 评测结果落库。
- 评测运行写入审计日志。
- 评测指标包括：
  - retrievalHit
  - citationPresent
  - keywordMatch
  - hasUnsupportedClaims
  - score
- 增加 LLM-as-judge。
- Judge 模型与主回答模型解耦。
- Judge 使用 Qwen `qwen3.6-plus`。
- Judge 失败时回退到规则/语义 overlap 评分。
- Micrometer 记录 evaluation pass/fail。

代表文件：

- `src/main/java/com/liujianan/agentdemo/evaluation/EvaluationService.java`
- `src/main/java/com/liujianan/agentdemo/evaluation/SemanticJudgeService.java`
- `src/main/java/com/liujianan/agentdemo/evaluation/QwenJudgeModelClient.java`
- `src/main/java/com/liujianan/agentdemo/evaluation/EvaluationRun.java`

后续可继续增强：

- 批量评测任务。
- 评测集分类：事实问答、多文档、工具调用、多轮对话。
- 增加 RAG 指标：
  - Recall@K
  - MRR
  - nDCG
  - Faithfulness
  - Answer Relevance
  - Citation Precision
- 支持 prompt/model/embedding/rerank/retrieval 参数 A/B 对比。
- 生成评测报告页面。
- 对 bad case 做失败归因。

## 8. 可观测性

已完成：

- 增加业务指标 `HarnessMetrics`。
- 指标包括：
  - RAG retrieval hit/miss
  - LLM answer latency
  - tool call success/failure
  - evaluation pass/fail
  - model circuit breaker open state
- 暴露 Prometheus endpoint。
- 每个 RAG 阶段通过 trace 记录：
  - USER_INPUT
  - RETRIEVAL
  - TOOL_PLAN
  - TOOL_CALL
  - TOOL_RESULT
  - CONTEXT_BUILD
  - ANSWER
  - QA_REVIEW

代表文件：

- `src/main/java/com/liujianan/agentdemo/common/HarnessMetrics.java`
- `src/main/java/com/liujianan/agentdemo/harness/TraceAgent.java`
- `src/main/java/com/liujianan/agentdemo/harness/TraceRecorder.java`

## 9. 当前模型分工

当前项目中的外部模型调用分工如下：

| 环节 | 模型/服务 | 用途 |
|------|----------|------|
| 文档 embedding | 阿里云百炼 `text-embedding-v4` | 文档向量化 |
| Query embedding | 阿里云百炼 `text-embedding-v4` | 用户问题向量化 |
| Rerank | 阿里云百炼 `qwen3-rerank` | 候选 chunk 精排 |
| 主回答生成 | DeepSeek `deepseek-chat` | 根据上下文生成答案 |
| LLM-as-judge | Qwen `qwen3.6-plus` | 评测回答质量 |

## 10. API Key 配置

推荐使用环境变量或 `.env`，不要把密钥写死到 `application.yml`。

最小配置：

```env
DEEPSEEK_API_KEY=your_deepseek_api_key_here
QWEN_API_KEY=your_bailian_dashscope_api_key_here
```

如果要分别配置：

```env
EMBEDDING_API_KEY=your_bailian_embedding_key
BAILIAN_API_KEY=your_bailian_rerank_key
QWEN_API_KEY=your_qwen_judge_key
```

当前 `.env.example` 已经补充了相关示例。

## 11. 测试情况

已新增或更新测试：

- 认证集成测试。
- refresh token / logout / access token 黑名单测试。
- Tool planning 测试。
- BM25 排序测试。
- Rerank 接入测试。
- Qwen Judge 测试。
- Semantic Judge fallback 测试。
- 熔断 gauge 测试。
- 评测结果落库测试。

最近一次全量测试：

```text
mvn test
Tests run: 89, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 12. 面试推荐表述

可以这样介绍项目：

> 这个项目是一个基于 Spring Boot 的知识库 Agent 平台。我主要做了后端企业化和 RAG 链路升级：认证上从自定义拦截器升级到 Spring Security + JWT + refresh token，并增加 RBAC、用户状态和审计日志；数据层使用 Flyway 管理 schema，并拆分 chat message、evaluation run、audit log 等独立表；RAG 链路上接入了阿里云百炼 embedding、pgvector 向量检索、BM25 关键词检索、百炼 rerank，并用 DeepSeek 生成最终回答；评测链路上使用 Qwen 作为独立 Judge 模型，避免主模型自评，同时保留规则评分 fallback。整个链路有 requestId、统一响应、分页、trace 和 Prometheus 指标，便于排查和观测。

需要诚实说明的点：

- 当前前端仍是 Spring Boot static 托管的演示页面，不是完整前后端分离。
- 当前 Tool Planning 是规则驱动的初版，还不是 LLM Planner。
- 当前缓存策略尚未系统接入。
- 当前引用溯源是 chunk 级，还不是页码/offset 级。
- 当前熔断是轻量级进程内实现，还不是 Resilience4j 或分布式熔断。

## 13. 后续优先级

建议后续继续按以下顺序增强：

1. 引用溯源增强：document/page/chunk/offset 级 citation。
2. 缓存策略：embedding、rerank、retrieval 多级缓存。
3. 权限模型：workspace/tenant + 文档级权限。
4. 批量评测：评测集、评测报告、A/B 对比。
5. Tool Planning：从规则规划升级为 LLM Planner。
6. 前端企业化：独立前端工程和管理后台。
