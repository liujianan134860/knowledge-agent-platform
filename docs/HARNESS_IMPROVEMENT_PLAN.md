# Knowledge Agent Platform Harness Improvement Plan

本文档记录当前项目相对 `revfactory/harness` 的差距，以及后续可执行的完善方案。

参考项目：

- https://github.com/revfactory/harness

## 1. 当前项目状态

当前项目已经具备一个知识库问答平台的核心雏形：

- Spring Boot 3 后端服务
- DeepSeek / OpenAI-Compatible Chat Completions 调用
- 文档上传与解析：`.docx`、`.pdf`、`.txt`、`.md`
- 内存知识库与关键词检索
- 知识库 + 大模型结合回答
- SSE 流式问答
- 会话记录与删除
- 知识片段新增、上传、删除
- 工具注册与工具调用示例
- Trace 事件记录
- 简单 Evaluation 样例管理
- 类 ChatGPT 的左右布局页面

项目已经具备 `Agent Harness` 的基础模块拆分意识，例如：

```text
chat        # 问答入口、上下文组装和回答生成
harness     # 会话、Trace、编排状态
knowledge   # 文档片段、上传解析、检索
llm         # 模型适配
tool        # 工具定义、注册、调用
evaluation  # 评测样例
```

但目前整体仍更接近一个 RAG Demo，还没有完全体现 `revfactory/harness` 的“Agent Team + Skill + Orchestration + Validation”的 Harness 架构思想。

## 2. 高优先级风险

### 2.1 避免提交真实 API Key

当前本地存在 `start-claude.ps1`，其中包含真实 API Key。该文件如果被提交到 GitHub，会造成密钥泄露。

建议：

- 不提交包含真实密钥的脚本。
- 将真实密钥只放在本地 `.env` 或系统环境变量中。
- 提交 `.env.example`，但只保留占位符。
- 如果密钥曾经被暴露在截图、日志、仓库或聊天记录中，应立即在服务商控制台轮换 Key。

推荐的模板写法：

```powershell
$env:ANTHROPIC_BASE_URL=${env:ANTHROPIC_BASE_URL}
$env:ANTHROPIC_AUTH_TOKEN=${env:ANTHROPIC_AUTH_TOKEN}
$env:ANTHROPIC_MODEL=${env:ANTHROPIC_MODEL}

claude --permission-mode bypassPermissions
```

### 2.2 恢复 `.env.example`

README 中已经说明可以复制 `.env.example` 为 `.env`，但本地状态显示 `.env.example` 被删除。

建议恢复：

```env
DEEPSEEK_API_KEY=your_deepseek_api_key_here
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_BASE_URL=https://api.deepseek.com
```

并确保 `.env` 保持在 `.gitignore` 中。

## 3. 对照 revfactory/harness 的差距

`revfactory/harness` 的核心不是单个 RAG 应用，而是一个 Team-Architecture Factory。它的重点包括：

- 根据项目领域生成 Agent Team
- 定义 `.claude/agents/`
- 定义 `.claude/skills/`
- 支持多种团队架构模式
- 支持 Agent 间编排、数据传递、错误处理
- 支持 Validation、dry-run、对比测试
- 支持 Harness Evolution，把实际项目使用中的经验反哺到下一版 Harness

当前项目主要缺口：

- `.claude/` 下还没有正式的 agents 和 skills。
- 后端代码里还没有清晰的 Orchestrator / Agent / Skill 抽象。
- Trace 记录还没有完整表达 Agent 执行链路。
- Evaluation 还没有形成 Harness Validation 流程。
- 工具调用还偏静态示例，没有形成可扩展 Tool/Skill 层。

## 4. 推荐目标结构

建议补充类似下面的 Harness 配置结构：

```text
.claude/
├── agents/
│   ├── retrieval-agent.md
│   ├── answer-agent.md
│   ├── tool-agent.md
│   └── qa-agent.md
└── skills/
    ├── rag-answer/
    │   └── SKILL.md
    ├── document-ingestion/
    │   └── SKILL.md
    ├── tool-calling/
    │   └── SKILL.md
    ├── trace-review/
    │   └── SKILL.md
    └── evaluation/
        └── SKILL.md
```

### 4.1 Agents 建议

| Agent | 职责 |
| --- | --- |
| `retrieval-agent` | 分析用户问题，执行知识库检索，返回候选片段 |
| `answer-agent` | 根据问题、会话历史、检索片段组织最终回答 |
| `tool-agent` | 识别是否需要工具调用，执行工具并返回结果 |
| `qa-agent` | 检查回答是否引用来源、是否偏离知识库、是否缺失信息 |

### 4.2 Skills 建议

| Skill | 职责 |
| --- | --- |
| `rag-answer` | 定义知识库问答流程、提示词、引用格式 |
| `document-ingestion` | 定义 Word/PDF/TXT/Markdown 上传、解析、切分规范 |
| `tool-calling` | 定义工具注册、调用、失败兜底和权限边界 |
| `trace-review` | 定义 Trace 检查方式和排错步骤 |
| `evaluation` | 定义评测样例、命中率、引用质量、人工反馈标准 |

## 5. 后端架构完善建议

### 5.1 拆分 ChatService

当前 `ChatService` 同时承担以下职责：

- 会话管理
- 知识库检索
- 上下文构建
- LLM 调用
- Trace 记录
- 流式回调

建议逐步拆成：

```text
HarnessOrchestrator
├── RetrievalAgent
├── ContextBuilder
├── AnswerComposer
├── ToolPlanningAgent
├── StreamOrchestrator
└── TraceAgent
```

这样可以更好对应 Harness 的 Pipeline / Producer-Reviewer / Supervisor 等模式。

### 5.2 引入 Orchestration Run

可以新增一个运行态对象：

```java
public record HarnessRun(
    String runId,
    String sessionId,
    String userQuestion,
    List<DocumentChunk> sources,
    List<ToolResult> toolResults,
    String finalAnswer,
    List<TraceEvent> traces
) {}
```

它用于串起一次问答中的检索、上下文、工具、回答、评测。

### 5.3 流式接口线程管理

当前流式接口如果直接 `new Thread`，演示可用，但工程上不够稳。

建议改为：

- Spring `TaskExecutor`
- `CompletableFuture`
- 设置线程池大小
- 处理客户端断开
- 记录流式失败原因

### 5.4 LLM 调用抽象

当前 `LlmClient` 已经能调用 DeepSeek，但后续可以抽象出：

```text
ModelClient
├── DeepSeekClient
├── OpenAiCompatibleClient
└── MockModelClient
```

好处：

- 测试时不用真实消耗 API
- 后续可以切换模型
- 可以增加超时、重试、错误分类

## 6. 知识库完善建议

### 6.1 从关键词检索升级到向量检索

当前关键词检索适合演示，但对真实知识问答不够稳定。

建议阶段性升级：

1. 保留关键词检索作为 fallback。
2. 增加 Embedding 接口。
3. 使用本地内存向量索引做演示。
4. 后续接入 pgvector、Milvus 或 Elasticsearch。

### 6.2 文档元数据

当前 `DocumentChunk` 只有标题、内容、标签等基础信息。

建议增加：

- `sourceFilename`
- `sourceType`
- `chunkIndex`
- `totalChunks`
- `checksum`
- `parentDocumentId`
- `pageNumber`

这样回答来源会更可信，也更方便前端展示。

### 6.3 切分策略可配置

当前切分逻辑已经比初版更好，但可以继续配置化：

- 最大 chunk 长度
- overlap 长度
- 是否按 Markdown 标题切分
- 是否按 PDF 页码切分
- 是否保留段落标题

## 7. Trace 完善建议

当前 Trace 可以记录阶段，但还可以增强为 Harness Run Trace。

建议字段：

```text
traceId
parentTraceId
runId
sessionId
agentName
stage
inputSummary
outputSummary
status
durationMs
errorMessage
createdAt
```

推荐阶段：

```text
USER_INPUT
SESSION_MEMORY
RETRIEVAL
CONTEXT_BUILD
TOOL_PLAN
TOOL_CALL
ANSWER_GENERATION
QA_REVIEW
EVALUATION
```

## 8. Evaluation / Validation 完善建议

`revfactory/harness` 很强调 Validation。你的项目可以把 Evaluation 从“样例记录”升级为“回答质量验证”。

建议增加：

- 检索命中率
- 回答是否引用来源
- 回答是否包含期望关键词
- 回答是否出现无来源断言
- DeepSeek 调用失败时 fallback 是否可用
- 有知识库 vs 无知识库回答对比
- 流式回答和非流式回答一致性

示例输出：

```json
{
  "question": "RAG Flow 是什么？",
  "expectedKeywords": ["检索", "上下文", "引用"],
  "retrievalHit": true,
  "citationPresent": true,
  "answerContainsExpectedKeywords": true,
  "score": 0.86
}
```

## 9. 前端完善建议

### 9.1 来源片段展示

当前来源展示较简洁，可以做成可展开卡片：

- 来源编号
- 文档标题
- 文件名
- 页码 / chunk 序号
- 命中关键词
- 原文片段

### 9.2 会话列表

可以进一步接近 ChatGPT：

- 左侧上半部分显示历史会话
- 支持重命名会话
- 支持删除会话
- 支持点击恢复历史消息

### 9.3 模型状态

建议在页面底部或顶部显示：

- DeepSeek 是否已配置
- 当前模型名称
- 当前检索模式：关键词 / 向量
- 当前知识片段数量

## 10. 分阶段路线图

### Phase 1: 安全与文档修复

- 移除脚本中的真实 API Key
- 恢复 `.env.example`
- 更新 README 的启动说明
- 确认 `.env`、日志、崩溃文件不被提交

### Phase 2: Claude Harness 文件结构

- 新增 `.claude/agents/`
- 新增 `.claude/skills/`
- 为 RAG、文档上传、工具调用、Trace、Evaluation 分别写 SKILL.md
- README 中增加 Harness Architecture 说明

### Phase 3: 后端 Harness Orchestrator

- 拆分 `ChatService`
- 新增 `HarnessOrchestrator`
- 新增 `ContextBuilder`
- 新增 `RetrievalAgent`
- 新增 `AnswerComposer`
- 新增更完整的 TraceRun 数据结构

### Phase 4: 检索质量提升

- 增加文档元数据
- 优化 chunk 管理
- 引入 Embedding 抽象
- 保留关键词检索 fallback

### Phase 5: Validation 与可观测性

- 扩展 Evaluation 指标
- 增加自动评测接口
- 增加 Trace 详情页
- 输出一次完整 Harness Run 的执行报告

## 11. 推荐优先级

建议按以下顺序推进：

1. 先处理密钥泄露风险。
2. 恢复 `.env.example` 和启动文档。
3. 新增 `.claude/agents` 与 `.claude/skills`，让项目真正体现 Harness 架构。
4. 将 `ChatService` 拆为 Orchestrator + Agent/Skill 风格。
5. 升级 Trace 与 Evaluation，让项目具备可解释、可验证的工程特点。

完成这些后，项目会从“知识库问答 Demo”升级为“基于 Harness 思想组织的 Agentic RAG 平台”。
