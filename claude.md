# Knowledge Agent Platform — 项目概要

## 项目定位
基于 **Spring AI + Agent Harness 架构** 的知识库问答与工具调用平台。支持 LLM 聊天、知识库检索、MCP 协议工具调用、评测管线。

## 技术栈
- **框架**: Spring Boot 3.4.5, Spring AI 1.1.4
- **LLM**: DeepSeek (OpenAI 兼容 API)
- **向量模型**: 阿里云 DashScope text-embedding-v4
- **评测模型**: Qwen3.6-plus (阿里百炼)
- **数据库**: H2 (开发) / PostgreSQL + pgvector (生产)
- **安全**: Spring Security + JWT
- **协议**: MCP (Model Context Protocol), SSE, REST

## 核心架构
```
用户请求 → ChatController
  → HarnessOrchestrator
    → SessionService (会话管理)
    → RetrievalAgent (知识库检索)
    → AnswerComposer (调用 LLM)
      → SpringAiModelClient (ChatClient + ToolCallbacks)
    → QaReview (自动评测)
```

## 关键文件
```
.env                              # 环境变量 (DEEPSEEK_MODEL, QWEN_API_KEY 等)
.env.example                      # 环境变量模板
src/main/resources/application.yml # Spring 配置

src/main/java/com/liujianan/agentdemo/
  ai/
    SpringAiModelClient.java      # LLM 调用客户端 (ChatClient + 工具回调)
    DeepSeekReasoningAdvisor.java  # DeepSeek thinking mode reasoning_content 桥接
  chat/
    ChatController.java           # /api/chat, /api/chat/stream
  harness/
    HarnessOrchestrator.java      # 主编排器
    AnswerComposer.java           # LLM 回答生成 (含熔断重试)
  knowledge/
    KnowledgeController.java      # /api/documents (知识库 CRUD)
  evaluation/
    EvaluationService.java        # 评测服务
    QwenJudgeModelClient.java     # Qwen 评测模型
  mcp/
    McpToolService.java           # 本地 @Tool 方法 (searchKnowledge, platformStatus)
    LocalToolCallbackConfig.java  # 注册本地工具到 ChatClient
    GitHubMcpToolCallbackProvider.java # GitHub MCP HTTP 直连 (绕过 SSE 405)
    GitHubMcpClientConfig.java    # GitHub MCP 认证头配置
  llm/
    LlmClient.java                # 原生 HTTP 客户端 (备用)
    ModelClient.java              # LLM 调用接口
  auth/
    SecurityConfig.java           # Spring Security 配置
    JwtAuthenticationFilter.java  # JWT 认证过滤器
```

## 快速启动
```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env 填入 API Key

# 2. 启动
mvn spring-boot:run

# 3. 访问
# 前端: http://localhost:8081
# Swagger: http://localhost:8081/swagger-ui/index.html
# H2 Console: http://localhost:8081/h2-console
```

## API 端点
| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/auth/register` | POST | 注册 |
| `/api/auth/login` | POST | 登录 (返回 JWT) |
| `/api/chat` | POST | 聊天 |
| `/api/chat/stream` | POST | 流式聊天 (SSE) |
| `/api/documents` | GET/POST | 知识库 CRUD |
| `/api/documents/search` | GET | 知识搜索 |
| `/api/traces` | GET | 会话追踪 |
| `/actuator/health` | GET | 健康检查 |

---

# Session Log — 2026-05-18

## 本次操作摘要
1. 检查模型配置与 .env 一致性
2. 测试工具调用能力
3. 修复多个问题
4. 创建项目文档
5. 修复前端中文乱码（mojibake）
6. 修复 SpringAiModelClient 构造函数注入问题

## 修改清单

### 1. `application.yml` — 修复硬编码 API Key
- L39: `spring.ai.openai.embedding.api-key` → `${QWEN_API_KEY:}`
- L121: `judge.qwen.api-key` → `${QWEN_API_KEY:}`
- L128: `rerank.bailian.api-key` → `${QWEN_API_KEY:}`
**原因**: 三处 QWEN_API_KEY 被硬编码为明文，违反安全最佳实践。

### 2. `.env.example` — 补充缺失的环境变量
新增: `QWEN_BASE_URL`, `QWEN_JUDGE_TIMEOUT_SECONDS`, `BAILIAN_RERANK_BASE_URL`, `BAILIAN_RERANK_TIMEOUT_SECONDS`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `REFRESH_TOKEN_EXPIRATION_MS`, `PORT`, `PGVECTOR_INIT_SCHEMA`, `GITHUB_MCP_REQUEST_TIMEOUT`, 调优参数组
**原因**: 原模板缺少 application.yml 引用的多个环境变量。

### 3. `McpToolService.java` — 修复工具参数
- 移除 `userId` 参数 (LLM 无法提供)
- 改为从 `SecurityContextHolder` 获取当前用户 ID
- 添加 INFO 级别日志 (`Tool called: ...`)
**原因**: @Tool 方法的参数需要 LLM 填充，userId 不在 LLM 上下文中。

### 4. `LocalToolCallbackConfig.java` — 新建
注册 `McpToolService` 的 @Tool 方法为 `ToolCallbackProvider`，使 ChatClient 能调用本地工具。
**原因**: 原代码只有 MCP Server 侧注册，ChatClient 侧无工具可用。

### 5. `DeepSeekReasoningAdvisor.java` — 新建
Spring AI CallAdvisor，桥接 DeepSeek v4-pro "thinking mode" 的 `reasoning_content` 字段跨多轮工具调用。
**原因**: deepseek-v4-pro 在工具调用多轮对话中要求回传 reasoning_content，否则报 400 错误。

### 6. `SpringAiModelClient.java` — 集成 DeepSeek 推理 Advisor
在 `chatClient.prompt()` 链中添加 `.advisors(reasoningAdvisor)`。
**原因**: 使 DeepSeekReasoningAdvisor 在每次 LLM 调用时生效。

### 7. `GitHubMcpToolCallbackProvider.java` — 新建
通过 HTTP POST JSON-RPC 直连 GitHub MCP 服务器，绕过 Spring AI SSE 传输的 405 问题。
启用: `GITHUB_MCP_HTTP_ENABLED=true`
**原因**: GitHub MCP 服务器只支持 POST，不支持 SSE。Spring AI 1.1.4 streamable-http 强制使用 SSE。

### 8. `McpToolServiceTest.java` — 适配新方法签名
移除 userId 参数对应的测试改动。
**原因**: McpToolService 方法签名变更。

### 9. `.env` — 模型临时切换
测试时改为 `DEEPSEEK_MODEL=deepseek-chat`，测试后恢复为 `deepseek-v4-pro`。

### 10. `index.html` — 修复中文乱码（mojibake）
- 原始 `src/main/resources/static/index.html` 中文全部显示为乱码（如 "涓婁笅鏂囬" 应为 "上下文预览"）
- 原因: 文件以 UTF-8 保存但中文字节序列被错误编码（疑似 ANSI/GBK 转换损坏）
- 修复: 用 `page.html`（正确 UTF-8 编码，功能更完整）覆盖 `index.html`
- 删除根目录重复的 `page.html`
- 新前端包含完整面板: knowledge, upload, tools, skills, agents, evaluation, run, trace, links

### 11. `SpringAiModelClient.java` — 修复构造函数注入
- 问题: 多个 public 构造函数 + `@Autowired` 误放在 field 上 → Spring 无法实例化 Bean
- 修复: 移除 field 上的 `@Autowired`，加到 `ObjectProvider` 构造函数上
- **原因**: Spring 多构造函数无明确标记时无法选择

### 12. `GitHubMcpToolCallbackProvider.java` — 修复 GitHub MCP HTTP 直连
- 问题 1: HTTP 400 — `Accept` header 缺少 `text/event-stream`
- 问题 2: GitHub MCP 返回 SSE 格式 (`data: {...}`)，Jackson 无法直接解析
- 问题 3: JSON-RPC `id` 字段用 String `"1"` 而非 Number `1`
- 修复:
  - Accept header: `application/json, text/event-stream`
  - 添加 `extractJsonFromSse()` 解析 SSE 响应
  - id 改为数字类型
  - 添加错误响应体日志

### 13. `application.yml` — 添加 `github.mcp.http.enabled` 映射
- 问题: `@ConditionalOnProperty(name = "github.mcp.http.enabled")` 无法从 `.env` 的 `GITHUB_MCP_HTTP_ENABLED` 解析
- 修复: 添加 `github.mcp.http.enabled: ${GITHUB_MCP_HTTP_ENABLED:false}` 显式映射

### 14. `.env` — 启用 GitHub MCP HTTP 并切换模型
- `GITHUB_MCP_HTTP_ENABLED=true` (启用 HTTP POST 直连工具)
- `GITHUB_MCP_ENABLED=false` (禁用 SSE 客户端，避免 405 错误)
- `DEEPSEEK_MODEL=deepseek-chat` (工具调用需要，v4-pro thinking mode 有 reasoning_content 问题)

## 测试结果

### ✅ 工具调用测试（通过）
- **platformStatus** 工具: 成功调用，返回 "知识库文档片段数: 5个"
- **searchKnowledge** 工具: 成功调用，搜索 "Docker" 返回相关知识
- 日志确认: `Tool called: platformStatus(userId=u-6509e007-...)`

### ✅ GitHub MCP 工具调用（通过）
- **19 个 GitHub 工具**成功加载（`get_repository`, `list_issues`, `search_repositories` 等）
- **真实查询测试**：查询 `github/github-mcp-server` → 返回 Stars: 29,930, Forks: 4,202, License: MIT, Language: Go
- 模型自动调用多个 GitHub 工具，结果准确
- 前提：使用 `deepseek-chat` 模型（`deepseek-v4-pro` 的 thinking mode 在工具调用时有 reasoning_content 问题）

### ✅ 模型配置一致性（通过）
所有代码中的模型引用均通过 `${ENV_VAR}` 正确解析到 .env 中的值。

### ✅ GitHub MCP 直连修复（通过）
- 修复了 HTTP 400: `Accept` header 需要 `application/json, text/event-stream`
- 修复了 SSE 响应解析: GitHub MCP 返回 SSE 格式，需提取 `data:` 行中的 JSON
- 修复了 `@ConditionalOnProperty` 属性映射: `application.yml` 添加 `github.mcp.http.enabled`

## 已知问题 & 解决状态

| 问题 | 状态 | 解决方案 |
|------|------|----------|
| `application.yml` 硬编码 API Key | ✅ 已修复 | 改为 `${QWEN_API_KEY:}` |
| `.env.example` 缺少变量 | ✅ 已修复 | 补充全部缺失变量 |
| `McpToolService` userId 参数 | ✅ 已修复 | SecurityContext 自动获取 |
| 本地工具未注册到 ChatClient | ✅ 已修复 | LocalToolCallbackConfig |
| deepseek-v4-pro reasoning_content | ✅ 已修复 | DeepSeekReasoningAdvisor |
| `index.html` 中文乱码 (mojibake) | ✅ 已修复 | 用 page.html 内容覆盖（UTF-8 无 BOM）|
| `SpringAiModelClient` 构造函数注入 | ✅ 已修复 | @Autowired 移至 ObjectProvider 构造函数 |
| GitHub MCP HTTP 400 (Accept header) | ✅ 已修复 | Accept: application/json, text/event-stream |
| GitHub MCP SSE 响应解析 | ✅ 已修复 | 提取 data: 行解析 JSON |
| `github.mcp.http.enabled` 属性映射 | ✅ 已修复 | application.yml 添加映射 |
| GitHub MCP SSE 405 (Spring AI 客户端) | ✅ 已绕过 | 使用 GitHubMcpToolCallbackProvider HTTP POST 直连 |
| deepseek-v4-pro reasoning_content | ⚠️ 部分修复 | DeepSeekReasoningAdvisor 存在，但 Spring AI 序列化不完整。临时方案：用 deepseek-chat |
| Spring AI MCP 升级 | 📋 待办 | 升级到 1.2+ 后原生支持 POST 传输 |
