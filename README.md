# Knowledge Agent Platform

一个基于 Spring Boot 3 的知识库问答与工具调用平台。项目采用 Agent Harness 思路组织代码，将模型适配、上下文构建、知识检索、工具注册、调用轨迹和评测记录拆分为相对独立的模块，便于观察、调试和扩展。

当前实现使用内存知识库和关键词检索模拟 RAG 流程，使用本地工具注册表模拟 Tool Calling/MCP 链路。代码结构保留 LLM、Embedding、向量数据库和 MCP 协议适配的扩展点。

## 功能概览

- 知识库管理：文档片段写入、列表查询、按关键词 Top-K 检索
- 问答链路：接收问题、检索上下文、生成回答、返回来源片段
- 会话管理：创建会话、记录多轮问答消息
- 流式响应：提供 SSE 形式的分段响应接口
- 工具注册：维护工具定义、参数示例、权限范围和超时配置
- 工具调用：内置 `echo`、`calculator`、`http_mock` 三个示例工具
- 调用轨迹：记录检索、上下文构建、工具调用、回答生成等 Trace 事件
- 轻量评测：维护问答样例，记录期望关键词、检索命中和人工反馈
- 接口文档：集成 Swagger/OpenAPI
- 容器化：提供 Dockerfile 和 Docker Compose

## 技术栈

- Java 17
- Spring Boot 3
- Spring MVC
- Jakarta Validation
- Server-Sent Events
- springdoc-openapi
- Docker / Docker Compose

## 架构说明

```text
src/main/java/com/liujianan/agentdemo
├── chat        # 问答入口、上下文组装和回答生成
├── common      # 统一响应、异常处理
├── evaluation  # 评测样例与反馈记录
├── harness     # Harness 编排、Trace、会话状态
├── knowledge   # 文档片段、检索、来源片段
└── tool        # 工具定义、注册、调用与结果
```

### Harness 流程

```text
User Question
  -> Session Memory
  -> Knowledge Retrieval
  -> Context Builder
  -> Tool Registry / Tool Executor
  -> Answer Composer
  -> Trace Recorder
  -> Evaluation Record
```

## 快速启动

```bash
mvn spring-boot:run
```

启动后访问：

- Home: http://localhost:8081/
- Swagger UI: http://localhost:8081/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs

## API 示例

### 添加文档片段

```bash
curl -X POST http://localhost:8081/api/documents \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Agent Harness\",\"content\":\"Harness separates model, memory, tools and trace for better debugging.\",\"tags\":[\"agent\",\"harness\"]}"
```

### 知识库问答

```bash
curl -X POST http://localhost:8081/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"How does Agent Harness improve debugging?\"}"
```

### SSE 流式问答

```bash
curl -N -X POST http://localhost:8081/api/chat/stream \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"Explain the tool calling flow\"}"
```

### 查看工具列表

```bash
curl http://localhost:8081/api/tools
```

### 调用计算工具

```bash
curl -X POST http://localhost:8081/api/tools/calculator/invoke \
  -H "Content-Type: application/json" \
  -d "{\"input\":\"12 + 30\"}"
```

### 查看 Trace

```bash
curl http://localhost:8081/api/traces
```

### 添加评测样例

```bash
curl -X POST http://localhost:8081/api/evaluations \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"What is RAG?\",\"expectedKeywords\":[\"retrieval\",\"context\",\"answer\"]}"
```

## Docker Compose

```bash
docker compose up --build
```

服务默认暴露在：

- http://localhost:8081

## Render 部署

项目已经支持读取 `PORT` 环境变量：

```yaml
server:
  port: ${PORT:8081}
```

在 Render 创建 Web Service 时选择 Docker 部署即可。部署完成后，Render 会提供一个 `https://xxx.onrender.com` 地址，可直接访问：

- `/swagger-ui/index.html`
- `/api/documents`
- `/api/chat`
- `/api/tools`
- `/api/traces`

## 扩展方向

- 接入真实 OpenAI-Compatible LLM API
- 使用 Embedding + pgvector/Milvus 替换关键词检索
- 增加 MCP Server/Client 协议适配
- 增加工具权限、审批节点和更完整的失败重试策略
- 接入 Micrometer 记录 Token、延迟、工具调用成功率等指标
