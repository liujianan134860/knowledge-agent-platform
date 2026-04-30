# Spring AI / pgvector / Micrometer / MCP 项目改造文档

本文档用于指导 `knowledge-agent-platform` 从当前的 Agent Harness + RAG 演示项目，升级为包含 Spring AI、Embedding、pgvector、Micrometer 和标准 MCP 协议能力的 AI 应用后端项目。

当前项目已有能力：

- Spring Boot 3 后端
- DeepSeek / OpenAI-Compatible API 调用
- 文档上传与解析：Word、PDF、TXT、Markdown
- 知识库问答
- Agent Harness 分层：`HarnessOrchestrator`、`RetrievalAgent`、`ContextBuilder`、`AnswerComposer`、`TraceAgent`
- SSE 流式响应
- Trace、Evaluation、QA Review
- JWT 登录鉴权
- H2 + JPA 持久化
- `.claude/agents` 与 `.claude/skills`

当前尚未完全实现：

- Spring AI ChatModel / EmbeddingModel
- pgvector 向量数据库
- Embedding + VectorStore 检索
- Micrometer 指标采集
- 标准 MCP Server / Client

## 1. 改造目标

最终目标是让项目具备以下能力：

```text
Document Upload
  -> Text Extraction
  -> Chunking
  -> Embedding
  -> pgvector
  -> Similarity Search
  -> Context Builder
  -> Spring AI ChatModel
  -> Answer with Citations
  -> Trace + Metrics + Evaluation
  -> MCP Tool Exposure
```

升级后，项目可以更稳妥地在简历中描述为：

```text
基于 Spring AI、pgvector 和 Agent Harness 架构实现知识库问答平台，支持文档上传、向量检索、模型调用、工具协议封装、Trace 观测和质量评测。
```

## 2. 推荐改造顺序

不要一次性改完，建议按以下顺序推进：

1. 接入 Spring AI ChatModel。
2. 切换 H2 到 PostgreSQL。
3. 接入 pgvector。
4. 实现 Embedding + VectorStore 检索。
5. 接入 Micrometer + Actuator。
6. 实现 MCP Server。
7. 实现 MCP Client。
8. 更新前端可视化页面。
9. 更新 README、部署文档和简历表述。

## 3. Phase 1：接入 Spring AI ChatModel

### 3.1 修改 `pom.xml`

添加 Spring AI BOM：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.1.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

添加 OpenAI-compatible 模型 starter：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

### 3.2 修改 `application.yml`

```yaml
spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY:}
      base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
      chat:
        options:
          model: ${DEEPSEEK_MODEL:deepseek-chat}
```

### 3.3 改造 `ModelClient`

当前已有：

```text
src/main/java/com/liujianan/agentdemo/llm/ModelClient.java
src/main/java/com/liujianan/agentdemo/llm/LlmClient.java
```

建议新增：

```text
SpringAiModelClient.java
```

职责：

- 使用 Spring AI `ChatModel` 或 `ChatClient`。
- 复用 `ContextBuilder.buildMessages(...)`。
- 保留 `ModelClient` 接口，避免影响 `AnswerComposer`。

示例结构：

```java
@Service
public class SpringAiModelClient implements ModelClient {
    private final ChatModel chatModel;

    public SpringAiModelClient(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String answer(String question, List<DocumentChunk> sources, List<SessionMessage> history) {
        // 将 ContextBuilder 结果转换为 Spring AI Prompt
        // 调用 chatModel.call(...)
        return "...";
    }
}
```

完成后，原有 `LlmClient` 可以保留为 fallback，也可以删除。

## 4. Phase 2：切换 PostgreSQL

当前项目使用：

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/knowledge-agent-platform
```

要使用 pgvector，需要切换为 PostgreSQL。

### 4.1 修改 `docker-compose.yml`

新增 PostgreSQL + pgvector：

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: knowledge-agent-postgres
    environment:
      POSTGRES_DB: knowledge_agent
      POSTGRES_USER: agent
      POSTGRES_PASSWORD: agent123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### 4.2 修改 `pom.xml`

添加 PostgreSQL Driver：

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 4.3 修改 `application.yml`

```yaml
spring:
  datasource:
    url: ${POSTGRES_URL:jdbc:postgresql://localhost:5432/knowledge_agent}
    username: ${POSTGRES_USER:agent}
    password: ${POSTGRES_PASSWORD:agent123}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

保留 H2 作为本地演示 fallback 也可以，但简历中如果写 pgvector，建议默认使用 PostgreSQL。

## 5. Phase 3：接入 pgvector VectorStore

### 5.1 修改 `pom.xml`

添加 Spring AI pgvector：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
</dependency>
```

### 5.2 修改 `application.yml`

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true
        dimensions: 1536
        distance-type: cosine_distance
```

注意：

- `dimensions` 必须和 Embedding 模型输出维度一致。
- 如果使用不同 Embedding 模型，需要同步修改维度。

## 6. Phase 4：Embedding + 向量检索

### 6.1 新增服务

建议新增：

```text
src/main/java/com/liujianan/agentdemo/knowledge/VectorKnowledgeService.java
```

职责：

- 将 `DocumentChunk` 转为 Spring AI `Document`。
- 写入 `VectorStore`。
- 执行 similarity search。
- 将检索结果转换回来源片段。

示例：

```java
@Service
public class VectorKnowledgeService {
    private final VectorStore vectorStore;

    public VectorKnowledgeService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void index(DocumentChunk chunk) {
        Document document = new Document(
                chunk.content(),
                Map.of(
                        "chunkId", chunk.id(),
                        "title", chunk.title(),
                        "userId", chunk.userId()
                )
        );
        vectorStore.add(List.of(document));
    }

    public List<Document> search(String query, int topK, String userId) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .filterExpression("userId == '" + userId + "'")
                        .build()
        );
    }
}
```

### 6.2 修改上传流程

当前流程：

```text
upload -> extract text -> split chunks -> save DocumentChunk
```

目标流程：

```text
upload -> extract text -> split chunks -> save DocumentChunk -> embedding -> vectorStore.add
```

修改位置：

```text
KnowledgeService.upload(...)
KnowledgeService.add(...)
```

保存每个 chunk 后，调用：

```java
vectorKnowledgeService.index(chunk);
```

### 6.3 修改检索流程

当前：

```text
RetrievalAgent -> KnowledgeService.search -> keyword score
```

目标：

```text
RetrievalAgent -> VectorKnowledgeService.search -> similarity search
```

建议保留关键词检索 fallback：

```text
if vector search failed or no result:
    use keyword search
```

这样项目演示更稳。

## 7. Phase 5：Micrometer + Actuator

### 7.1 修改 `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 7.2 修改 `application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### 7.3 新增 Metrics 服务

建议新增：

```text
src/main/java/com/liujianan/agentdemo/common/HarnessMetrics.java
```

示例：

```java
@Component
public class HarnessMetrics {
    private final Counter retrievalHitCounter;
    private final Counter retrievalMissCounter;
    private final Timer llmAnswerTimer;
    private final Counter toolSuccessCounter;
    private final Counter toolFailureCounter;

    public HarnessMetrics(MeterRegistry registry) {
        this.retrievalHitCounter = Counter.builder("rag.retrieval.hit").register(registry);
        this.retrievalMissCounter = Counter.builder("rag.retrieval.miss").register(registry);
        this.llmAnswerTimer = Timer.builder("llm.answer.latency").register(registry);
        this.toolSuccessCounter = Counter.builder("tool.call.success").register(registry);
        this.toolFailureCounter = Counter.builder("tool.call.failure").register(registry);
    }
}
```

埋点位置：

```text
RetrievalAgent
AnswerComposer
ToolController / ToolRegistry
EvaluationService
QaReviewController
```

## 8. Phase 6：MCP Server

当前项目有工具注册：

```text
ToolRegistry
ToolController
calculator / echo / http_mock
```

它目前是 MCP 思路模拟，不是完整 MCP 协议。

### 8.1 添加 MCP Server 依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

### 8.2 新增 MCP Tool Service

```text
src/main/java/com/liujianan/agentdemo/mcp/McpToolService.java
```

示例：

```java
@Service
public class McpToolService {
    private final ToolRegistry toolRegistry;
    private final KnowledgeService knowledgeService;

    public McpToolService(ToolRegistry toolRegistry, KnowledgeService knowledgeService) {
        this.toolRegistry = toolRegistry;
        this.knowledgeService = knowledgeService;
    }

    @Tool(description = "Calculate simple arithmetic expression")
    public String calculator(String input) {
        return toolRegistry.invoke("calculator", input).output();
    }

    @Tool(description = "Search user knowledge base")
    public String searchKnowledge(String query) {
        // 调用 KnowledgeService 或 VectorKnowledgeService
        return "...";
    }
}
```

### 8.3 暴露 Tools

目标：

- Claude Code 或其它 MCP Client 可以发现你的工具。
- 外部客户端可以调用知识库检索、计算器等能力。

实现后，简历中可以写：

```text
基于 Spring AI MCP Server 将知识库检索、计算器和 HTTP Mock 封装为标准 MCP Tools，支持工具发现、调用和异常兜底。
```

## 9. Phase 7：MCP Client

如果要更完整，可以让项目连接外部 MCP Server。

### 9.1 添加依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client-webflux</artifactId>
</dependency>
```

### 9.2 配置 Client

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        name: knowledge-agent-platform
```

### 9.3 集成外部工具

可接入：

- filesystem MCP
- GitHub MCP
- browser MCP
- database MCP

目标：

```text
用户问题 -> ToolPlanningAgent 判断是否需要外部工具 -> MCP Client 调用外部 MCP Server -> 结果进入 ContextBuilder -> LLM 回答
```

## 10. Phase 8：前端可视化改造

当前网页已经有登录、会话、知识库、上传、工具、Trace、问答。

建议新增：

### 10.1 Harness Run 时间线

接口：

```text
GET /api/runs?sessionId=xxx
```

展示：

```text
USER_INPUT
RETRIEVAL
CONTEXT_BUILD
ANSWER
QA_REVIEW
```

字段：

- 阶段名称
- 状态
- 耗时
- 输入摘要
- 输出摘要
- attributes JSON

### 10.2 Agent Team 面板

接口：

```text
GET /api/agents
```

展示 `.claude/agents`：

- Retrieval Agent
- Answer Agent
- Tool Agent
- QA Agent

### 10.3 Skills 面板

接口：

```text
GET /api/skills
```

展示 `.claude/skills`：

- document-ingestion
- rag-answer
- tool-calling
- trace-review
- evaluation

### 10.4 Metrics 面板

接口：

```text
GET /actuator/metrics
GET /actuator/prometheus
```

展示：

- RAG 检索次数
- 检索命中率
- LLM 平均延迟
- Tool 调用成功率
- QA Review 平均得分

## 11. Phase 9：README 与简历更新

完成上述功能后，README 技术栈可以改为：

```text
Java 17、Spring Boot 3、Spring AI、Spring Data JPA、PostgreSQL、pgvector、
Micrometer、MCP、DeepSeek/OpenAI-Compatible API、SSE、Docker
```

简历中第二项目可以写：

```text
• 基于 Spring AI 接入 OpenAI-Compatible ChatModel 与 EmbeddingModel，使用 pgvector 持久化文档向量，完成文档上传、文本切分、向量入库、Top-K 相似度检索和来源片段引用。

• 基于 Agent Harness 思路拆分 RetrievalAgent、ContextBuilder、AnswerComposer、TraceAgent 与 Evaluation 模块，记录每次问答的检索命中、上下文构建、模型调用、响应延迟和质量检查结果。

• 引入 Micrometer + Actuator 采集 RAG 检索、LLM 调用、工具调用和响应耗时指标，并通过可视化页面展示 Harness Run 时间线和运行状态。

• 基于 Spring AI MCP 实现 MCP Server/Client，将知识库检索、计算器、HTTP Mock 等能力封装为标准 MCP Tools，支持工具发现、调用和异常兜底。
```

如果没有完成对应功能，不建议提前写入简历。

## 12. 验收清单

### Spring AI

- [ ] 项目引入 Spring AI BOM。
- [ ] 使用 ChatModel / ChatClient 调用 DeepSeek。
- [ ] 原 `ModelClient` 接口仍可用。

### pgvector

- [ ] Docker Compose 能启动 PostgreSQL + pgvector。
- [ ] 应用能连接 PostgreSQL。
- [ ] pgvector schema 自动初始化或手动初始化。

### Embedding

- [ ] 上传文档后生成 embedding。
- [ ] 文档 chunk 写入 VectorStore。
- [ ] 问答时使用 similarity search。
- [ ] 保留关键词检索 fallback。

### Micrometer

- [ ] `/actuator/health` 可访问。
- [ ] `/actuator/metrics` 可访问。
- [ ] `/actuator/prometheus` 可访问。
- [ ] RAG、LLM、Tool、Evaluation 有自定义指标。

### MCP

- [ ] 项目作为 MCP Server 暴露工具。
- [ ] 至少暴露 calculator、knowledge search 两个工具。
- [ ] 可被 Claude Code 或其它 MCP Client 发现。
- [ ] 项目作为 MCP Client 可连接至少一个外部 MCP Server。

### 前端

- [ ] 页面展示 Harness Run 时间线。
- [ ] 页面展示 Agent Team。
- [ ] 页面展示 Skills。
- [ ] 页面展示 Metrics。
- [ ] 页面展示 QA Review 结果。

## 13. 注意事项

- 不要提交 `.env`。
- 不要在脚本中写真实 API Key。
- pgvector 的维度必须和 Embedding 模型一致。
- DeepSeek Chat API 与 Embedding API 需要分开确认，不要默认同一个模型同时支持。
- MCP Server/Client 实现前，简历中不要写“完整 MCP 协议”，可以写“参考 MCP 思路实现工具调用链路”。
- 所有新能力都应至少有本地验证命令和 README 说明。
