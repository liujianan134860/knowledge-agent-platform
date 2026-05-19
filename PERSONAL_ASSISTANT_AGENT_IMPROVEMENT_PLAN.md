# Personal Assistant Agent Improvement Plan

本文档面向 `Knowledge Agent Platform` 的下一阶段改造：从当前的知识库问答平台，演进为一个具备任务编排、ReAct 执行循环、Skill 能力包、内部工具和 MCP 扩展能力的个人日常助手。

目标不是一次性堆满所有工具，而是先建立一套稳定的 Agent 运行时协议，让后续工具、Skill、MCP Server 都能以一致方式接入。

## 1. 目标定位

项目当前已经具备：

- Spring Boot 3 后端服务
- 用户认证与 JWT
- 文档上传、解析、知识检索
- Chat / SSE 问答接口
- Harness 编排雏形
- Trace、Evaluation、QA Review
- Spring AI 与 MCP 基础依赖

下一阶段目标：

```text
用户目标
  -> 识别意图
  -> 选择 Skill
  -> 生成计划
  -> ReAct 循环执行
  -> 调用内部 Tool / MCP Tool
  -> 记录 Observation
  -> 输出结果或等待用户确认
  -> 保存任务、笔记、记忆和 Trace
```

最终形态应更像一个个人助手，而不是只回答问题的 RAG Demo。

## 2. 个人日常助手能力地图

个人助手应优先覆盖这些场景：

| 场景 | 用户示例 | 核心能力 |
| --- | --- | --- |
| 今日规划 | 帮我规划今天 | 读取任务、日程、偏好，生成计划 |
| 待办管理 | 帮我把找工作拆成任务 | 创建、拆解、排序、延期、完成任务 |
| 提醒事项 | 明晚 9 点提醒我复盘简历 | 创建、查询、完成、稍后提醒 |
| 个人知识记录 | 记一下我面试要重点讲 Agent Harness | 写入笔记、长期记忆、知识库 |
| 文档处理 | 帮我看一下这份简历 | 文件读取、文本抽取、总结、建议 |
| 调研分析 | 帮我调研几个成熟 Agent 项目 | 搜索、阅读、归纳、保存 |
| 周复盘 | 帮我总结这周完成了什么 | 聚合任务、笔记、日程和完成情况 |
| 邮件草稿 | 帮我写一封面试感谢邮件 | 生成草稿、等待确认后发送 |

## 3. 优先级总览

### P0: Agent 运行时基础

没有这一层，后续 Tool 和 Skill 会变成散落的 Service。

必须补充：

- `AgentDefinition`
- `AgentRun`
- `AgentStep`
- `AgentState`
- `ReActExecutor`
- `ToolRegistry`
- `ToolExecutor`
- `SkillRegistry`

建议目录：

```text
src/main/java/com/liujianan/agentdemo/agent
├── AgentDefinition.java
├── AgentRun.java
├── AgentStep.java
├── AgentState.java
├── AgentStepType.java
├── AgentRunStatus.java
├── ReActExecutor.java
├── AgentPlanner.java
└── AgentPolicy.java
```

### P1: 个人助手 MVP 工具

先补最能形成闭环的内部工具。

建议第一批 Tool：

```text
datetime.now
datetime.parse
task.create
task.list
task.update
task.complete
reminder.create
reminder.list
note.create
note.search
knowledge.search
memory.remember
memory.recall
document.extract_text
document.summarize
```

这批工具能支持：

- 今日计划
- 待办管理
- 提醒事项
- 个人知识记录
- 文档摘要
- 简单长期记忆

### P2: Skill 能力包

Skill 负责“如何完成一类任务”，Tool 负责“执行一个动作”。

建议第一批 Skill：

```text
daily-planning
task-breakdown
knowledge-capture
document-assistant
resume-coach
research
weekly-review
```

建议目录：

```text
src/main/resources/skills
├── daily-planning
│   ├── skill.yml
│   └── SKILL.md
├── task-breakdown
│   ├── skill.yml
│   └── SKILL.md
├── knowledge-capture
│   ├── skill.yml
│   └── SKILL.md
├── document-assistant
│   ├── skill.yml
│   └── SKILL.md
├── resume-coach
│   ├── skill.yml
│   └── SKILL.md
├── research
│   ├── skill.yml
│   └── SKILL.md
└── weekly-review
    ├── skill.yml
    └── SKILL.md
```

### P3: MCP 扩展

内部 Tool 跑通之后，再接 MCP。否则 Agent 容易直接绑定外部协议，后续难维护。

建议顺序：

```text
filesystem MCP
browser MCP
github MCP
calendar MCP
gmail/outlook MCP
notion MCP
slack/teams MCP
database MCP
```

统一设计：

```text
MCP Server
  -> McpToolDiscoveryService
  -> McpToolAdapter
  -> ToolRegistry
  -> ReActExecutor
```

Agent 只认识 `ToolRegistry`，不直接依赖具体 MCP Server。

### P4: 前端 Agent 控制台

当前静态页面可以逐步演进为 Agent Console。

建议增加：

- Agent / Skill 选择区
- 当前任务 Plan
- ReAct Step 时间线
- Tool Call 输入输出
- Observation 展示
- 需要审批的动作确认按钮
- Run / Trace / Evaluation 联动查看

### P5: 评测与安全

当 Agent 能执行工具后，评测和安全必须同步升级。

需要补充：

- Tool 调用成功率
- Tool 参数正确率
- ReAct 是否陷入循环
- 是否遵守审批策略
- 是否产生 unsupported claim
- 是否错误写入长期记忆
- 是否调用了越权工具

## 4. 内部 Tool 设计

建议新增 `tool` 包的核心抽象：

```text
src/main/java/com/liujianan/agentdemo/tool
├── ToolDefinition.java
├── ToolParameterSchema.java
├── ToolInvocationRequest.java
├── ToolInvocationResult.java
├── ToolExecutor.java
├── ToolRegistry.java
├── ToolPermissionPolicy.java
├── ToolRiskLevel.java
└── builtin
    ├── DateTimeTool.java
    ├── TaskTool.java
    ├── ReminderTool.java
    ├── NoteTool.java
    ├── MemoryTool.java
    ├── KnowledgeSearchTool.java
    └── DocumentTool.java
```

每个工具至少包含：

```text
name
description
inputSchema
outputSchema
timeout
riskLevel
requiredScopes
approvalRequired
```

风险分级：

| 风险 | 示例 | 策略 |
| --- | --- | --- |
| LOW | 查询时间、搜索知识库、读取任务 | 可直接执行 |
| MEDIUM | 创建任务、创建提醒、写入笔记 | 可配置是否确认 |
| HIGH | 删除文件、发送邮件、运行脚本、修改远程系统 | 必须用户确认 |

第一阶段建议不要开放裸 `shell.run`、`file.delete`、`email.send`。高风险动作应设计为 `*_request`，由用户确认后执行。

## 5. Skill 设计

建议新增 `skill` 包：

```text
src/main/java/com/liujianan/agentdemo/skill
├── SkillDefinition.java
├── SkillRegistry.java
├── SkillLoader.java
├── SkillSelector.java
└── SkillExecutionContext.java
```

`skill.yml` 示例：

```yaml
id: daily-planning
name: Daily Planning
description: Plan the user's day using tasks, reminders, calendar events, and personal preferences.
version: 1.0.0
triggers:
  - 帮我规划今天
  - 今天我该做什么
  - 生成今日计划
requiredTools:
  - datetime.now
  - task.list
  - reminder.create
  - memory.recall
riskLevel: low
```

`SKILL.md` 示例内容应包含：

- 适用场景
- 输入信息
- 执行步骤
- 可调用工具
- 输出格式
- 失败处理
- 安全边界

## 6. ReAct 执行循环

建议把 ReAct 作为运行时能力，而不是只写进 Prompt。

基础循环：

```text
1. 接收用户任务
2. 选择 AgentDefinition 与 Skill
3. 初始化 AgentRun 和 AgentState
4. 模型生成 Thought 和下一步 Action
5. 如果是 Tool Call，调用 ToolRegistry
6. 将结果写入 Observation
7. 保存 AgentStep 和 TraceEvent
8. 重复直到 Final Answer、超时、超步数或等待审批
```

`AgentStep` 建议字段：

```text
id
runId
stepIndex
type: THOUGHT | ACTION | OBSERVATION | FINAL | APPROVAL_REQUIRED | ERROR
thought
toolName
toolArguments
observation
status
startedAt
endedAt
errorMessage
```

初期可以限制：

- 最大步数：8-12
- 单工具超时：10-30 秒
- 高风险工具必须中断并等待用户确认
- 连续失败 2 次后进入失败总结

## 7. 数据模型建议

新增表：

```text
agent_run
agent_step
task_item
reminder_item
note_item
personal_memory
tool_invocation
skill_definition_cache
mcp_server_config
```

优先级最高的数据表：

```text
agent_run
agent_step
task_item
reminder_item
note_item
personal_memory
tool_invocation
```

这些表能支撑 MVP 个人助手闭环。

## 8. 分阶段实施计划

### 阶段 1: Agent Core 与 ReAct MVP

目标：让系统能执行一个多步任务，而不是只生成回答。

改动：

- 新增 `agent` 核心模型
- 新增 `ReActExecutor`
- 新增 `AgentRun` / `AgentStep` 持久化
- 将 `HarnessOrchestrator` 的部分职责迁移或包装到 Agent Runtime
- Trace 与 AgentStep 建立关联

验收标准：

- 用户输入“帮我规划今天”
- 系统能创建一个 `AgentRun`
- 至少生成 2 个以上 `AgentStep`
- 每一步可在 Trace 中查看
- 最终输出可读计划

### 阶段 2: ToolRegistry 与个人工具 MVP

目标：让 Agent 能调用统一工具，而不是直接调用 Service。

改动：

- 新增 `ToolDefinition` / `ToolRegistry` / `ToolExecutor`
- 实现 `datetime`、`task`、`reminder`、`note`、`memory` 工具
- `knowledge.search` 适配现有 `KnowledgeService`
- 工具调用写入 `tool_invocation`

验收标准：

- Agent 可以查询当前时间
- Agent 可以创建任务
- Agent 可以创建提醒
- Agent 可以记录笔记或记忆
- 所有工具调用有审计和 Trace

### 阶段 3: SkillRegistry 与第一批 Skill

目标：让系统根据任务选择能力包，而不是只靠一个大 Prompt。

改动：

- 新增 `SkillDefinition` / `SkillLoader` / `SkillRegistry`
- 加载 `skill.yml` 和 `SKILL.md`
- 实现 `daily-planning`、`task-breakdown`、`knowledge-capture`
- Prompt 构造时注入选中 Skill 的说明和工具限制

验收标准：

- “帮我规划今天”命中 `daily-planning`
- “帮我把准备面试拆成任务”命中 `task-breakdown`
- “记一下……”命中 `knowledge-capture`
- Skill 所需工具能被限制到本次运行范围内

### 阶段 4: 文档与简历助手

目标：结合项目现有文档解析优势，强化个人场景。

改动：

- 实现 `document-assistant`
- 实现 `resume-coach`
- 补充 `document.extract_text`、`document.summarize`
- 可选增加 `resume.analyze`、`resume.optimize_suggestion`

验收标准：

- 用户上传简历后可获得结构化修改建议
- 可从文档中提取待办
- 可将文档摘要写入笔记

### 阶段 5: MCP Tool Adapter

目标：把外部 MCP Server 接入统一工具层。

改动：

- 新增 `McpServerRegistry`
- 新增 `McpToolDiscoveryService`
- 新增 `McpToolAdapter`
- 新增 `McpToolExecutor`
- 先接 filesystem / browser / github 三类 MCP

验收标准：

- MCP 工具可以显示在 `ToolRegistry`
- Agent 可以像调用内部工具一样调用 MCP 工具
- MCP 工具同样受权限、审批、Trace 管理

### 阶段 6: Agent Console 与评测升级

目标：让用户能看懂 Agent 在做什么，也能评测它做得好不好。

改动：

- 前端展示 Plan / Steps / Tool Calls / Observations
- 增加审批交互
- Evaluation 增加工具调用质量指标
- QA Review 增加 ReAct 轨迹检查

验收标准：

- 前端可以查看一次任务完整轨迹
- 高风险动作会暂停等待确认
- 评测能发现错误工具调用、循环执行和无依据结论

## 9. 推荐改动优先级清单

### 立即做

1. 新增 `ToolDefinition`、`ToolRegistry`、`ToolExecutor`
2. 新增 `AgentRun`、`AgentStep`、`ReActExecutor`
3. 实现 `datetime.now`、`task.create`、`task.list`
4. 实现 `note.create`、`memory.remember`、`knowledge.search`
5. 新增 `daily-planning` 和 `task-breakdown` 两个 Skill

### 第二批做

1. 实现 `reminder.create`、`reminder.list`
2. 实现 `document.extract_text`、`document.summarize`
3. 新增 `knowledge-capture`、`document-assistant`、`resume-coach`
4. 前端展示 AgentStep 时间线
5. Tool 调用写入审计和 Trace

### 第三批做

1. 增加 MCP Tool Adapter
2. 接入 filesystem / browser / github MCP
3. 增加审批策略
4. 增加 weekly-review、research Skill
5. 扩展 Evaluation，评估工具调用与 ReAct 轨迹

### 暂缓做

这些能力价值高，但不适合作为第一阶段：

- 邮件直接发送
- 本地 shell 自动执行
- 自动删除或修改文件
- 多 Agent 并行协作
- 复杂 DAG 工作流
- 全自动外部账号操作

建议等 Tool 权限、审批和 Trace 稳定后再做。

## 10. MVP 示例

第一阶段可验证的完整场景：

用户输入：

```text
帮我规划今天，我主要想推进 Agent 项目和准备简历。
```

期望执行：

```text
1. SkillSelector 选择 daily-planning
2. ReActExecutor 创建 AgentRun
3. 调用 datetime.now
4. 调用 task.list
5. 调用 memory.recall
6. 生成今日计划
7. 调用 task.create 创建必要任务
8. 调用 reminder.create 创建关键提醒
9. 输出最终计划
10. 保存 AgentStep、ToolInvocation、TraceEvent
```

该场景跑通后，项目就具备了个人日常助手的基本闭环。

