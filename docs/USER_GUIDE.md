# User Guide

本文档用于演示 Knowledge Agent Platform 的主要功能。

在线地址：https://knowledge-agent-platform.onrender.com/

## 1. 注册与登录

访问首页后，首先需要注册一个账号：

1. 点击 **Register** 切换至注册表单
2. 输入用户名和密码（至少 6 位）
3. 注册成功后自动登录

下次访问时使用 **Login** 直接登录。

## 2. 首页

首页左侧导航栏提供以下功能面板：

- **知识库** — 查看和管理知识片段
- **上传文档** — 上传 `.docx` / `.pdf` / `.txt` / `.md` 文件
- **工具** — 调用内置工具（echo、calculator、http_mock）
- **Run** — 查看问答执行时间线
- **Agents** — 查看 Agent 团队定义
- **Skills** — 查看平台技能定义
- **Trace** — 查看请求追踪记录
- **评测** — 添加和运行评测用例
- **接口** — 快速访问各 API 入口

## 3. 查看接口文档

访问：

```text
https://knowledge-agent-platform.onrender.com/swagger-ui/index.html
```

可以在 Swagger UI 中直接调试接口。

## 4. 添加文档片段

接口：

```text
POST /api/documents
```

也可以在首页左侧“上传文档”区域直接选择文件上传。当前支持：

- Word: `.docx`
- PDF: `.pdf`
- Text: `.txt`
- Markdown: `.md`

上传后系统会抽取文本并自动切分为多个知识片段。

文件上传接口：

```text
POST /api/documents/upload
```

示例：

```bash
curl -X POST https://knowledge-agent-platform.onrender.com/api/documents/upload \
  -F "file=@/path/to/document.pdf" \
  -F "title=项目文档" \
  -F "tags=upload,rag"
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

如果部署环境配置了 `DEEPSEEK_API_KEY`，`answer` 会由 DeepSeek 根据检索到的来源片段生成。未配置时，系统会返回检索摘要。

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

## 8. 查看 Agent 定义

接口：

```text
GET /api/agents
```

示例：

```bash
curl https://knowledge-agent-platform.onrender.com/api/agents
```

返回 Agent 团队定义，包括名称、角色、职责、输入输出描述等信息。

## 9. 查看 Skill 定义

接口：

```text
GET /api/skills
```

示例：

```bash
curl https://knowledge-agent-platform.onrender.com/api/skills
```

返回平台技能定义，涵盖文档解析、评测、RAG 回答、工具调用、Trace 审查等。

## 10. 查看 Trace

接口：

```text
GET /api/traces
```

示例：

```bash
curl https://knowledge-agent-platform.onrender.com/api/traces
```

Trace 用于观察问答、检索、工具调用、回答生成等步骤。

## 11. 维护评测样例

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

## 12. 回答质量检查（QA Review）

在问答页面，每条 AI 回答下方都有一个 **检查回答** 按钮。点击后会自动对回答进行质量评估，包括：

- 是否命中知识库
- 是否标注来源引用
- 关键词覆盖率
- 是否存在无依据断言
- 综合评分（0-100）

检查结果直接显示在回答下方。可以直接调用 API 进行质量检查：

```bash
curl -X POST https://knowledge-agent-platform.onrender.com/api/qa/review \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is Java?",
    "answer": "Java is a programming language [1].",
    "sources": [{"title": "Source", "content": "Java is a programming language", "chunkId": "1", "tags": "", "score": 0}],
    "expectedKeywords": ["Java", "programming"]
  }'
```
