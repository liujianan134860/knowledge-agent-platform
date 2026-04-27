# User Guide

本文档用于演示 Knowledge Agent Platform 的主要功能。

## 1. 首页

访问：

```text
https://knowledge-agent-platform.onrender.com/
```

首页提供以下入口：

- Swagger UI
- Documents API
- Tools API
- Trace API
- Evaluation API

## 2. 查看接口文档

访问：

```text
https://knowledge-agent-platform.onrender.com/swagger-ui/index.html
```

可以在 Swagger UI 中直接调试接口。

## 3. 添加文档片段

接口：

```text
POST /api/documents
```

示例：

```bash
curl -X POST https://knowledge-agent-platform.onrender.com/api/documents \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Agent Harness\",\"content\":\"Harness separates model, memory, tools and trace for better debugging.\",\"tags\":[\"agent\",\"harness\"]}"
```

## 4. 知识库问答

接口：

```text
POST /api/chat
```

示例：

```bash
curl -X POST https://knowledge-agent-platform.onrender.com/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"How does Agent Harness improve debugging?\"}"
```

返回内容包括：

- `sessionId`
- `answer`
- `sources`
- `promptTokens`
- `latencyMs`

## 5. SSE 流式问答

接口：

```text
POST /api/chat/stream
```

示例：

```bash
curl -N -X POST https://knowledge-agent-platform.onrender.com/api/chat/stream \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"Explain the tool calling flow\"}"
```

## 6. 查看工具列表

接口：

```text
GET /api/tools
```

示例：

```bash
curl https://knowledge-agent-platform.onrender.com/api/tools
```

当前内置工具：

- `echo`
- `calculator`
- `http_mock`

## 7. 调用工具

接口：

```text
POST /api/tools/{name}/invoke
```

示例：

```bash
curl -X POST https://knowledge-agent-platform.onrender.com/api/tools/calculator/invoke \
  -H "Content-Type: application/json" \
  -d "{\"input\":\"12 + 30\"}"
```

## 8. 查看 Trace

接口：

```text
GET /api/traces
```

示例：

```bash
curl https://knowledge-agent-platform.onrender.com/api/traces
```

Trace 用于观察问答、检索、工具调用、回答生成等步骤。

## 9. 维护评测样例

接口：

```text
POST /api/evaluations
```

示例：

```bash
curl -X POST https://knowledge-agent-platform.onrender.com/api/evaluations \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"What is RAG?\",\"expectedKeywords\":[\"retrieval\",\"context\",\"answer\"]}"
```

查看评测样例：

```bash
curl https://knowledge-agent-platform.onrender.com/api/evaluations
```
