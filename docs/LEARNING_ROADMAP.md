# Knowledge Agent Platform — 学习路线图

> 本文档面向编程初学者，系统性地讲解本项目涉及的所有基础知识、工具和框架，并给出推荐的学习顺序。

---

## 一、项目整体认知

本项目是一个**知识库问答平台**，叫做 Knowledge Agent Platform（知识 Agent 平台）。

### 它能做什么？

1. **注册/登录** — 用户系统，每个用户有自己的会话和数据
2. **知识库管理** — 上传文档（Word/PDF/TXT），把内容拆成知识片段
3. **智能问答** — 用户提问 -> 从知识库搜索相关片段 -> 发给大模型(DeepSeek) -> 返回带来源引用的回答
4. **工具调用** — 内置计算器、HTTP 模拟等小工具
5. **运行时间线** — 展示一次问答的完整执行过程
6. **评测系统** — 自动检查回答质量（是否引用来源、包含关键词等）
7. **Agent 团队 & Skills** — 展示项目架构中的角色和能力定义

### 技术架构概览

```
┌─────────────────────────────────────────────┐
│           浏览器 (HTML/CSS/JS)                │  ← 前端
│           用户看到的页面                      │
│   聊天界面 / 知识库管理 / 时间线 / 评测面板    │
└──────────────────┬──────────────────────────┘
                   │ HTTP REST / SSE 流式
                   ▼
┌─────────────────────────────────────────────┐
│          Spring Boot (Java 17)               │  ← 后端
│                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ 认证模块  │  │ 问答模块  │  │ 知识库模块 │   │
│  │ Auth     │  │ Chat     │  │ Knowledge│   │
│  └──────────┘  └─────┬────┘  └──────────┘   │
│                      │                       │
│         ┌────────────▼────────────┐          │
│         │   HarnessOrchestrator   │          │  ← Agent 编排层
│         │   (Agent 编排引擎)       │          │
│         │                        │          │
│         │  ┌──────┐ ┌─────────┐  │          │
│         │  │检索   │ │ 回答组合 │  │          │
│         │  │Agent  │ │ Answer  │  │          │
│         │  └──────┘ │Composer │  │          │
│         │           └─────────┘  │          │
│         │  ┌──────┐ ┌─────────┐  │          │
│         │  │工具   │ │ QA 质检  │  │          │
│         │  │Agent  │ │ Review  │  │          │
│         │  └──────┘ └─────────┘  │          │
│         └────────────────────────┘          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ 工具模块  │  │ 评测模块  │  │ Trace   │   │
│  │ Tool     │  │Eval.     │  │ 追踪     │   │
│  └──────────┘  └──────────┘  └──────────┘   │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│    H2 数据库 / DeepSeek LLM API             │  ← 数据/AI 层
│    文件系统 (.claude/ 定义文件)               │
│    Agent 定义 / Skill 定义                   │
└─────────────────────────────────────────────┘
```

---

## 二、所需基础知识（按学习顺序）

### 1. Java 语言基础（约 2-4 周）

本项目使用 **Java 17**，这是最主流的后端开发语言之一。

**必须掌握：**

| 知识点 | 说明 | 在本项目中的使用 |
|--------|------|-----------------|
| 变量与数据类型 | `int`, `String`, `boolean` 等 | 定义请求参数、配置项 |
| 方法（函数） | 如何定义和调用方法 | 每个 Service 和 Controller 都有大量方法 |
| 类与对象 | Java 是面向对象语言 | 每个 `@Service`、`@RestController` 都是一个类 |
| 构造方法 | 用 `new` 创建对象时的初始化 | 依赖注入、创建实体对象 |
| 继承与接口 | `extends`, `implements` | `JpaRepository` 接口、异常处理 |
| 泛型 | `<T>` 类型参数 | `ApiResponse<T>`、`List<DocumentChunk>` |
| 集合框架 | `List`, `Map`, `Set` | 存储知识片段列表、工具注册表 |
| Stream API | `.map()`, `.filter()`, `.collect()` | 搜索排序、数据转换（大量使用） |
| Optional | 避免空指针 | 数据库查询结果包装 |
| Record 类 | Java 16+ 的新特性，`public record ...` | `ApiResponse`、`ChatRequest`、`ToolResult` 等 |
| Lambda 表达式 | `->` 箭头函数 | 工具调用 handler、Stream 操作 |
| 异常处理 | `try-catch`, `throws` | 全局异常处理、文件上传错误处理 |
| 注解 | `@Override`, `@Service` 等 | Spring 框架的核心机制 |

**初学者建议：**
- 先学基础语法：变量、循环、条件判断、方法
- 再学面向对象：类、继承、接口
- 最后学 Java 8+ 特性：Stream、Lambda、Optional、Record

**推荐资源：**
- 廖雪峰 Java 教程（中文免费）
- 《Head First Java》（入门经典）
- Oracle 官方 Java 17 文档

---

### 2. Git 版本控制（约 1 周）

**Git** 是代码版本管理工具，记录每次代码变更。

**必须掌握：**

| 命令/概念 | 说明 |
|-----------|------|
| `git init` | 初始化仓库 |
| `git clone` | 下载远程仓库 |
| `git add` | 暂存修改 |
| `git commit` | 提交修改 |
| `git push / pull` | 推送/拉取到远程 |
| `git status` | 查看当前状态 |
| `git log` | 查看提交历史 |
| `git branch` | 分支管理 |

**在本项目中的使用：**
- 每次功能完成后 `git add` → `git commit`
- 用 `git log` 查看提交记录（参考 `eff43a0 Improve chat generation...` 等）
- 用 `.gitignore` 忽略不需要提交的文件

---

### 3. Maven 构建工具（约 3 天）

**Maven** 是 Java 项目的"管家"——管理依赖、编译、打包。

**核心概念：**

| 概念 | 说明 |
|------|------|
| `pom.xml` | 项目配置文件，声明依赖和插件 |
| 坐标 | `groupId:artifactId:version` 唯一定位一个库 |
| 仓库 | 存储依赖的地方（Maven Central） |
| 生命周期 | `clean` → `compile` → `test` → `package` |

**常用命令：**
```bash
mvn clean           # 删除编译产物
mvn compile         # 编译源码
mvn test            # 运行测试
mvn package         # 打包成 JAR
mvn spring-boot:run # 编译并运行（本项目最常用）
```

**在本项目中的使用：**
```xml
<!-- pom.xml 中声明了 11 个依赖，如： -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

---

### 4. Spring Boot 框架（约 2-4 周）

**Spring Boot** 是 Java 最流行的 Web 框架，"开箱即用"。

#### 4.1 核心概念

| 概念 | 类比 | 在本项目中的使用 |
|------|------|-----------------|
| IoC 容器 / DI | 框架帮你创建对象，不用自己 `new` | 所有 `@Service`、`@RestController` |
| `@RestController` | 标记一个类是 API 接口 | `ChatController`、`AuthController` 等 |
| `@Service` | 标记业务逻辑层 | `ChatService`、`EvaluationService` 等 |
| `@GetMapping` | 处理 GET 请求 | `GET /api/documents` |
| `@PostMapping` | 处理 POST 请求 | `POST /api/chat` |
| `@RequestMapping` | 设置 URL 前缀 | `@RequestMapping("/api/sessions")` |
| `@RequestBody` | 把请求的 JSON 转成 Java 对象 | 接收用户输入 |
| `@PathVariable` | 从 URL 中提取参数 | `DELETE /api/sessions/{id}` |
| `@RequestParam` | 从查询字符串提取参数 | `?query=RAG&topK=3` |
| `@Valid` | 自动验证请求参数 | 检查用户名长度、密码长度 |
| `@Component` | 通用组件标记 | `AuthInterceptor` |
| `@Configuration` | 配置类 | `WebMvcConfig` |

#### 4.2 Spring Data JPA（约 1-2 周）

**JPA** 是 Java 的数据库操作标准，**Spring Data JPA** 让它变得非常简单。

| 概念 | 说明 | 使用示例 |
|------|------|---------|
| `@Entity` | 标记一个类对应数据库表 | `User`、`ChatSession`、`DocumentChunk` |
| `@Id` | 主键 | `String id` |
| `@Column` | 列配置（长度、唯一性等） | `@Column(unique = true)` |
| `@Table` | 表名配置 | `@Table(name = "app_user")` |
| `JpaRepository` | 内置 CRUD 操作 | `findById()`, `save()`, `delete()` |
| 派生查询 | 方法名自动生成 SQL | `findByUsername()`, `findByUserIdOrderByCreatedAtDesc()` |
| `@Transactional` | 事务管理 | 注册、添加评测用例 |

**在本项目中的使用：**
```java
// 定义实体
@Entity
public class User {
    @Id private String id;
    @Column(unique = true) private String username;
    private String passwordHash;
    private LocalDateTime createdAt;
}

// 操作数据库
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

#### 4.3 Spring Security Crypto（约 2 天）

本项目使用 `spring-security-crypto` 中的 **BCrypt** 加密密码。

```java
PasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("用户密码");    // 加密
boolean ok = encoder.matches("密码", hash);  // 验证
```

#### 4.4 Spring 拦截器

```java
// 在请求到达 Controller 之前进行拦截
@Component
public class AuthInterceptor implements HandlerInterceptor {
    public boolean preHandle(HttpServletRequest request, ...) {
        // 验证 JWT Token，通过后在 request 中设置 userId
    }
}
```

---

### 5. 前端基础（约 3-4 周）

本项目的前端**全部写在 Java 代码里**（`HomeController.java` 的文本块中），但仍然是标准的 Web 前端技术。

#### 5.1 HTML（约 1 周）

HTML 是网页的"骨架"。

**本项目中用到的标签：**

| 标签 | 用途 |
|------|------|
| `<div>` | 区块容器 |
| `<button>` | 按钮 |
| `<input>` | 输入框 |
| `<textarea>` | 多行输入 |
| `<section>` | 面板区域 |
| `<pre>` | 预格式化文本 |
| `<h1>`~`<h4>` | 标题 |
| `<p>` | 段落 |
| `<span>` | 行内容器 |
| `<style>` | CSS 样式 |
| `<script>` | JavaScript 代码 |
| `<link>` | 引入外部资源 |

#### 5.2 CSS（约 1-2 周）

CSS 是网页的"衣服"，控制样式和布局。

**本项目用的核心概念：**

| 概念 | 说明 | 使用示例 |
|------|------|---------|
| 选择器 | 选中元素 | `.class`, `#id`, `element`, `:hover` |
| Flexbox | 弹性布局 | `display: flex; justify-content: space-between;` |
| Grid | 网格布局 | `display: grid; grid-template-columns: 300px 1fr;` |
| 盒模型 | margin/padding/border | 控制元素间距和边框 |
| 颜色 | 十六进制/RGB | `#174a78`, `#f6f8fb` |
| 字体 | font-family, font-size | 中文友好字体 |
| 过渡 | transition | 悬停效果的平滑变化 |
| 圆角 | border-radius | `8px` 的圆角卡片 |
| 阴影 | box-shadow | 弹窗的阴影效果 |
| 媒体查询 | @media | 响应式布局（适配手机/平板） |

#### 5.3 JavaScript（约 2-3 周）

JavaScript 是网页的"行为"，让页面有交互。

**本项目中涉及的 JS 知识点：**

| 知识点 | 说明 | 使用示例 |
|--------|------|---------|
| 变量 | `let`, `const` | `let token = localStorage.getItem('auth_token')` |
| 函数 | `function()`, `async function()` | `async function login()`, `function showAuth()` |
| 箭头函数 | `() => {}` | `const $ = id => document.getElementById(id)` |
| DOM 操作 | 修改页面内容 | `$('chatList').innerHTML = ...` |
| 事件处理 | 用户交互 | `$('question').addEventListener('keydown', ...)` |
| Fetch API | 发起 HTTP 请求 | `fetch(url, {method, headers, body})` |
| async/await | 异步编程 | `var data = await api('/api/chat', ...)` |
| Promise | 异步操作 | `.catch(error => ...)` |
| JSON | 数据格式 | `JSON.stringify()`, `JSON.parse()` |
| localStorage | 浏览器本地存储 | 存储 Token |
| 模板字符串 | 反引号字符串 | 但本项目中用的是字符串拼接 `+` |
| 闭包 | 函数内返回函数 | `sources.forEach(function(src, idx) { ... })` |
| 数组方法 | `.map()`, `.filter()`, `.join()` | 构建 HTML 列表 |
| 正则表达式 | `/pattern/g` | 替换换行符 `/\n/g` |
| SSE | 服务器推送事件 | `EventSource` 或 fetch 流式读取 |

**特别提醒（来自本项目踩过的坑）：**

> Java 文本块中的 `\n` 会被 Java 解释为换行符，而不是 JavaScript 中的正则表达式。
> 如果要在 Java 文本块中写 JavaScript 的 `\n`，必须写成 `\\n`。

#### 5.4 Marked.js（前端库）

```html
<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
```
用于把 Markdown 文本渲染为 HTML（LLM 的回答通常是 Markdown 格式）。

---

### 6. REST API 设计（约 3 天）

REST 是前后端通信的"约定"。

**本项目 API 风格：**

```
GET    /api/sessions          # 列出会话
POST   /api/sessions          # 创建会话
DELETE /api/sessions/{id}     # 删除会话

POST   /api/chat              # 发起问答
POST   /api/chat/stream       # 流式问答 (SSE)

GET    /api/documents         # 列出知识片段
POST   /api/documents         # 添加知识片段
POST   /api/documents/upload  # 上传文件
DELETE /api/documents/{id}    # 删除片段

POST   /api/auth/register     # 注册
POST   /api/auth/login        # 登录

GET    /api/tools             # 列出工具
POST   /api/tools/{name}/invoke  # 调用工具

GET    /api/runs              # 运行时间线
POST   /api/evaluations       # 添加评测用例
POST   /api/evaluations/run   # 运行评测
POST   /api/qa/review         # QA 质检
```

**统一响应格式：**
```json
{
  "success": true,
  "message": "ok",
  "data": { ... }
}
```

---

### 7. 数据库基础（约 1 周）

#### 7.1 H2 数据库

H2 是一个**嵌入式的 Java 数据库**，无需安装，随应用启动。

- 项目启动时自动创建 `./data/knowledge-agent-platform.mv.db`
- 访问 `http://localhost:8081/h2-console` 可以查看数据
- JDBC URL: `jdbc:h2:file:./data/knowledge-agent-platform`

#### 7.2 JPA 实体 → 数据库表

| Java 实体类 | 数据库表 | 主要字段 |
|------------|---------|---------|
| `User` | `app_user` | id, username, password_hash, created_at |
| `ChatSession` | `chat_session` | id, title, user_id, created_at |
| `SessionMessage` | `session_message` | id, session_id, role, content, created_at |
| `DocumentChunk` | `document_chunk` | id, title, content, tags, user_id |
| `EvaluationCase` | `evaluation_case` | id, question, expected_keywords, feedback |
| `TraceEvent` | `trace_event` | id, session_id, stage, message, attributes |

---

### 8. JWT 认证（约 3 天）

**JWT（JSON Web Token）** 是一种无状态的认证方式。

**流程：**
```
用户登录 → 服务器生成 JWT Token → 返回给浏览器
浏览器在 localStorage 中保存 Token
后续请求在 HTTP Header 中携带 Token
服务器验证 Token → 获取用户身份
```

**JWT 结构：**
```
header.payload.signature
（头部.载荷.签名）
```

**本项目使用的库：** `jjwt-api` + `jjwt-impl` + `jjwt-jackson`

---

### 9. 大模型 API 调用（约 3 天）

本项目调用 **DeepSeek API**（兼容 OpenAI 接口格式）。

```java
// 调用大模型的核心代码
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
headers.setBearerAuth(apiKey);

Map<String, Object> requestBody = Map.of(
    "model", "deepseek-chat",
    "messages", messages,
    "stream", true
);

// 发送 HTTP POST 请求到 https://api.deepseek.com/chat/completions
```

**关键概念：**

| 概念 | 说明 |
|------|------|
| System Prompt | 系统级提示词，定义 AI 的角色和行为 |
| User Message | 用户的问题 |
| Assistant Message | AI 的回答 |
| Temperature | 控制回答的随机性（0=固定，1=随机） |
| Max Tokens | 回答的最大长度 |
| Streaming | 逐个 token 返回，而不是等全部生成完 |

---

### 10. 辅助工具（约 3 天）

#### 10.1 curl

命令行 HTTP 请求工具，用于测试 API。

```bash
# GET 请求
curl http://localhost:8081/api/sessions -H "Authorization: Bearer xxx"

# POST 请求 + JSON
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 文件上传
curl -X POST http://localhost:8081/api/documents/upload \
  -F "file=@document.pdf"
```

#### 10.2 Swagger / OpenAPI

API 文档自动生成工具。

访问 `http://localhost:8081/swagger-ui/index.html` 可以看到所有接口的可交互文档。

#### 10.3 H2 Console

数据库管理界面，访问 `http://localhost:8081/h2-console`。

---

### 11. AI Agent 与 LLM 应用技能（约 4-6 周）

这是本项目的**核心特色**，也是最值得花时间学习的内容。项目中 "Agent" 指的是一个**有特定职责的 AI 组件**，多个 Agent 协作完成复杂任务。

#### 11.1 大语言模型（LLM）基础

| 概念 | 说明 | 在本项目中的使用 |
|------|------|-----------------|
| 什么是 LLM | 通过海量文本训练的大型神经网络，能理解和生成自然语言 | DeepSeek Chat 模型 |
| Token | LLM 的最小处理单位（一个中文词 ≈ 2-3 个 token） | 计算 prompt_tokens |
| 上下文窗口 | LLM 一次能"记住"的最大 token 数 | 本项目约 8K-32K tokens |
| Temperature | 控制输出的随机性（0=确定，1=创意） | 在 LlmClient 中配置 |
| System Prompt | 给 LLM 的系统级指令，设定角色和行为 | 定义 AI 助手的回答风格、引用规则 |
| User Prompt | 用户输入的问题 | 从聊天输入框传入 |
| Completion | LLM 生成的回答文本 | ChatResponse.answer |

**推荐学习：**
- OpenAI 官方文档 "Prompt Engineering" 指南
- DeepSeek 官方 API 文档
- 吴恩达《ChatGPT Prompt Engineering for Developers》免费课程

#### 11.2 RAG（检索增强生成）

**RAG = Retrieval + Augmented + Generation**，是本项目的核心工作模式。

```
用户提问："什么是 RAG 架构？"
    │
    ▼
┌─────────────────────────────────────┐
│ ① 检索 (Retrieval)                  │
│ 从知识库搜索相关片段                  │
│ → 找到"RAG是检索与生成结合的架构"      │
└─────────────┬───────────────────────┘
              │ 知识片段
              ▼
┌─────────────────────────────────────┐
│ ② 增强 (Augmented)                  │
│ 将知识片段拼入提示词                   │
│ "根据以下资料回答问题：[知识片段]"      │
└─────────────┬───────────────────────┘
              │ 完整 Prompt
              ▼
┌─────────────────────────────────────┐
│ ③ 生成 (Generation)                 │
│ LLM 基于资料生成带引用的回答           │
│ → "RAG 是... [1]"                   │
└─────────────────────────────────────┘
```

**关键知识点：**

| 知识点 | 本项目的实现 |
|--------|------------|
| 文本分块（Chunking） | 上传文档时按段落/大小切分为 DocumentChunk |
| 关键词搜索 | `KnowledgeService.search()` 使用分词 + 二元组匹配 |
| 向量搜索（进阶） | 本项目暂用关键词搜索，可升级为 Embedding 向量搜索 |
| 来源引用 | 回答中用 `[1]`, `[2]` 标记来源编号 |
| 上下文组装 | `AnswerComposer.compose()` 构建包含知识片段的 Prompt |

#### 11.3 Agent 架构与编排

本项目参考了 **revfactory/harness** 的 Agent 架构思想，把一次问答拆成多个 Agent 协作完成：

```
                    ┌──────────────┐
                    │  用户输入问题  │
                    └──────┬───────┘
                           │
              ┌────────────▼────────────┐
              │   HarnessOrchestrator    │  ← 编排引擎
              │   (负责调度所有 Agent)    │
              └──┬─────┬──────┬─────────┘
                 │     │      │
    ┌────────────▼┐ ┌──▼───┐ ┌▼───────────┐
    │ Retrieval   │ │Answer│ │ QA Review  │
    │ Agent      │ │Agent │ │ Agent      │
    │            │ │      │ │            │
    │ 搜索知识库  │ │ 调用 │ │ 检查回答质量 │
    │ 返回片段   │ │ LLM  │ │ 打分/反馈   │
    └────────────┘ └──────┘ └────────────┘
```

| 概念 | 说明 | 类比 |
|------|------|------|
| Agent（代理） | 一个有明确职责的 AI 组件 | 公司里的"部门" |
| Orchestrator（编排器） | 调度多个 Agent 协作的引擎 | 项目经理 |
| Skill（技能） | Agent 完成工作所需的能力和知识 | 部门的"工作手册" |
| Pipeline（流水线） | Agent 的执行顺序和流程 | 工厂的"生产线" |
| Trace（追踪） | 记录每一步执行情况 | 项目"日志" |
| Tool（工具） | Agent 可以调用的外部功能（计算器、API 等） | 部门的"工具" |

**本项目的 Agent 定义文件（存储在 .claude/agents/ 目录）：**

```markdown
# 检索 Agent
负责从知识库中搜索与问题相关的知识片段
## Responsibilities
- 分析问题意图
- 执行关键词搜索
- 返回排序结果
```

#### 11.4 Prompt Engineering（提示词工程）

写好提示词是使用 LLM 的核心技能。本项目中有几个关键的提示词：

```java
// 有知识库时的 System Prompt
String sysPrompt = "你是一个知识库问答助手。请结合用户问题和给定知识片段生成回答，"
    + "优先依据知识库内容，不要只复述原文。回答默认使用中文，"
    + "使用 Markdown 格式排版，关键结论后用 [1]、[2] 引用来源编号。";

// 无知识库时的 System Prompt
String sysPrompt = "你是一个简洁、友好的中文问答助手。"
    + "当前没有检索到知识库片段时，可以先自然回应用户；"
    + "如果用户提出专业问题，请说明尚未命中知识库，并给出通用建议。";
```

**Prompt 设计原则：**

| 原则 | 说明 | 本项目的实践 |
|------|------|------------|
| 明确角色 | 告诉 AI "你是谁" | "你是一个知识库问答助手" |
| 指定格式 | 定义输出的结构和风格 | "使用 Markdown 格式" |
| 提供上下文 | 给 AI 足够的信息 | 拼入知识片段和对话历史 |
| 约束行为 | 防止 AI 幻觉 | "优先依据知识库内容" |
| Citation | 要求引用来源 | "用 [1]、[2] 引用来源编号" |
| Fallback | 无信息时的处理 | "说明尚未命中知识库，给出通用建议" |

#### 11.5 流式输出（Streaming / SSE）

LLM 生成回答需要时间（几秒到几十秒）。流式输出可以**一个字一个字地**显示回答，而不是等全部生成完再一次性显示。

**工作流程：**

```
用户请求 → 建立 SSE 连接
    │
    ├── event: session → "会话ID"
    ├── event: sources → "3"
    ├── event: delta → "R"
    ├── event: delta → "AG"
    ├── event: delta → " 是..."
    ├── ... (逐字推送)
    └── event: done → "latencyMs=5234"
```

**前端接收：**
```javascript
// fetch 流式读取
const reader = response.body.getReader();
while (true) {
    const { done, value } = await reader.read();
    // 解析 event stream，逐段显示回答
}
```

**技术要点：**
- SSE = Server-Sent Events（服务器推送事件）
- 与 WebSocket 的区别：SSE 是单向（服务器→浏览器），WebSocket 是双向
- 每次推送一个 `event:` 行 + `data:` 行

#### 11.6 工具调用（Tool Calling / Function Calling）

让 LLM 可以调用外部工具（计算器、API 等），扩展 AI 的能力。

**本项目注册的工具：**

| 工具名 | 功能 | 输入格式 |
|--------|------|---------|
| `echo` | 返回输入文本，用于连通性测试 | `"hello"` |
| `calculator` | 计算数学表达式 | `"12 + 30"` |
| `http_mock` | Mock HTTP 请求，返回预设响应 | `"GET /api/projects"` |

**工具注册代码**（`ToolRegistry.java`）：
```java
// 每个工具用 Map 注册一个名字 + 定义 + 处理函数
tools.put("calculator", new RegisteredTool(
    new ToolDefinition("calculator", "计算表达式", "12 + 30", ...),
    input -> calculate(input)  // Lambda 表达式作为处理函数
));
```

#### 11.7 回答质量评测（Evaluation / QA Review）

LLM 的回答需要自动化检查质量。本项目的评测系统从多个维度打分：

| 指标 | 权重 | 检查内容 |
|------|------|---------|
| 检索命中 | 30% | 是否从知识库找到了相关片段 |
| 来源引用 | 30% | 回答中是否包含 `[1]`、`[2]` 等引用标记 |
| 关键词匹配 | 40% | 回答是否包含期望的关键词 |
| 无来源断言 | 扣分项 | 是否出现了知识库中没有依据的断言 |

**评分公式：**
```java
double score = 0.0;
score += retrievalHit ? 0.3 : 0.0;      // 命中知识库 +0.3
score += citationPresent ? 0.3 : 0.0;   // 引用来源 +0.3
score += 0.4 * matchedRatio;            // 关键词匹配比例
if (hasUnsupportedClaims) score -= 0.2;  // 无来源断言 -0.2
```

#### 11.8 Agent 定义文件（.claude/ 目录）

本项目的架构信息存储在 `.claude/` 目录中，这是一种"自描述"的架构方式：

```
.claude/
├── agents/
│   └── retrieval-agent.md    ← 定义了"检索 Agent"的职责
└── skills/
    └── rag-answer/
        └── SKILL.md          ← 定义了"RAG 回答"这项技能
```

这些文件不仅是文档，还会被 `AgentController` 和 `SkillController` 读取并展示在页面上。

---

### 12. 文件解析库（了解即可）

| 库 | 用途 |
|----|------|
| Apache PDFBox | 解析 .pdf 文件 |
| Apache POI | 解析 .docx 文件 |
| 内置 | 解析 .txt 和 .md 文件 |

---

## 三、推荐学习路线（总计约 14-20 周）

### 第一阶段：编程基础（约 4 周）

```
第 1 周  ─  Java 基础语法
            ├── 变量、数据类型、运算符
            ├── 条件判断（if-else）
            ├── 循环（for, while）
            └── 数组

第 2 周  ─  Java 面向对象
            ├── 类和对象
            ├── 继承与多态
            ├── 接口与抽象类
            └── 异常处理

第 3 周  ─  Java 高级特性
            ├── 集合框架（List, Map, Set）
            ├── 泛型
            ├── Stream API
            └── Lambda 表达式

第 4 周  ─  Git + Maven
            ├── Git 基本操作
            ├── GitHub 远程仓库
            ├── Maven 项目结构
            └── pom.xml 配置
```

### 第二阶段：Web 后端（约 4 周）

```
第 5 周  ─  Spring Boot 入门
            ├── 依赖注入与 IoC
            ├── @RestController 与 @RequestMapping
            ├── @GetMapping / @PostMapping
            └── @RequestBody 与 @RequestParam

第 6 周  ─  Spring Data JPA
            ├── @Entity 与 @Id
            ├── JpaRepository 接口
            ├── 派生查询方法
            └── @Transactional

第 7 周  ─  REST API + JSON
            ├── RESTful 设计规范
            ├── JSON 序列化/反序列化
            └── 统一响应格式

第 8 周  ─  认证与安全
            ├── JWT 原理
            ├── BCrypt 密码加密
            ├── 拦截器（Interceptor）
            └── Token 验证流程
```

### 第三阶段：前端基础（约 4 周）

```
第 9 周  ─  HTML
            ├── 常用标签
            ├── 表单与输入
            └── 语义化

第 10 周 ─  CSS
            ├── 选择器
            ├── Flexbox 布局
            ├── Grid 布局
            └── 响应式设计

第 11 周 ─  JavaScript
            ├── 变量与函数
            ├── DOM 操作
            ├── 事件处理
            └── Fetch API

第 12 周 ─  JS 高级
            ├── async/await 异步编程
            ├── Promise
            ├── 正则表达式
            └── SSE（Server-Sent Events）
```

### 第四阶段：AI Agent 与 LLM（约 3-4 周）

```
第 13 周 ─  LLM 与大模型基础
            ├── 什么是大语言模型
            ├── Token / 上下文窗口 / Temperature
            ├── System Prompt vs User Prompt
            ├── DeepSeek / OpenAI API 调用方式
            └── 用 curl 直接调 API 体验一下

第 14 周 ─  RAG（检索增强生成）
            ├── RAG 的核心流程（检索→增强→生成）
            ├── 知识库分块策略
            ├── 关键词搜索 vs 向量搜索
            ├── 提示词组装与上下文构建
            └── 来源引用的实现

第 15 周 ─  Agent 架构与 Prompt Engineering
            ├── Agent 是什么（有职责的 AI 组件）
            ├── 编排器 Orchestrator 的作用
            ├── 多个 Agent 如何协作
            ├── Prompt 设计原则（角色/格式/约束/回退）
            └── 工具调用的原理与实践

第 16 周 ─  流式输出与质量评测
            ├── SSE 流式推送的原理
            ├── 前端接收流式数据
            ├── LLM 回答质量评分标准
            ├── 检索命中 / 引用检查 / 关键词匹配
            └── 自动化评测与 QA Review
```

### 第五阶段：项目实战（约 3-4 周）

```
第 17 周 ─  跑通本项目
            ├── 配置 DeepSeek API Key 环境变量
            ├── mvn spring-boot:run 启动
            ├── 注册账号、上传文档、发起问答
            ├── 测试所有 API（参考 run_tests.sh）
            └── 理解代码结构

第 18 周 ─  动手修改（后端）
            ├── 添加一个新的工具（如天气查询）
            ├── 修改 System Prompt 改变回答风格
            ├── 添加一个新的 API 接口
            └── 修改知识库搜索逻辑

第 19 周 ─  动手修改（前端 + AI）
            ├── 修改聊天界面的样式
            ├── 添加一个新的面板
            ├── 修改评测指标和评分公式
            └── 添加更多 Agent 定义文件

第 20 周 ─  独立开发
            ├── 从 0 搭建 Spring Boot 项目
            ├── 设计数据库表
            ├── 编写 CRUD 接口
            ├── 集成 LLM API
            └── 部署到服务器
```

---

## 四、本项目核心代码解读

### 项目目录结构

```
knowledge-agent-platform/
├── pom.xml                          # Maven 项目配置
├── src/main/java/com/liujianan/agentdemo/
│   ├── KnowledgeAgentDemoApplication.java  # 启动类
│   ├── auth/                         # 认证模块
│   │   ├── AuthController.java       # 登录/注册 API
│   │   ├── AuthInterceptor.java      # JWT 拦截验证
│   │   ├── UserService.java          # 用户业务逻辑
│   │   └── JwtUtil.java              # JWT 工具类
│   ├── chat/                         # 问答模块
│   │   ├── ChatController.java       # 问答 API
│   │   └── ChatService.java          # 问答业务逻辑
│   ├── knowledge/                    # 知识库模块
│   │   ├── KnowledgeController.java  # 知识库 API
│   │   └── KnowledgeService.java     # 搜索/上传逻辑
│   ├── harness/                      # 运行编排模块
│   │   ├── HarnessOrchestrator.java  # 问答流程编排
│   │   ├── SessionService.java       # 会话管理
│   │   ├── TraceRecorder.java        # 事件追踪
│   │   └── AnswerComposer.java       # 回答生成
│   ├── evaluation/                   # 评测模块
│   │   ├── EvaluationController.java # 评测 API
│   │   ├── EvaluationService.java    # 评测逻辑
│   │   └── QaReviewController.java   # QA 质检 API
│   ├── tool/                         # 工具模块
│   │   ├── ToolController.java       # 工具 API
│   │   ├── ToolRegistry.java         # 工具注册中心
│   │   └── ToolDefinition.java       # 工具定义
│   ├── llm/                          # 大模型调用
│   │   └── LlmClient.java            # DeepSeek API 封装
│   ├── agent/                        # Agent 定义
│   │   └── AgentController.java      # 读取 .claude/agents/
│   ├── skill/                        # Skill 定义
│   │   └── SkillController.java      # 读取 .claude/skills/
│   └── common/                       # 公共组件
│       └── ApiResponse.java          # 统一响应格式
├── .claude/
│   ├── agents/                       # Agent 定义文件
│   └── skills/                       # Skill 定义文件
├── docs/                             # 文档
└── run_tests.sh                      # 自动化测试脚本
```

### 一次问答的完整流程（Agent 视角）

```
用户输入问题
    │
    ▼
┌────────────────────────────────────────────────┐
│ ChatController 接收 HTTP 请求                   │
│ (POST /api/chat 或 POST /api/chat/stream)      │
└──────────────────┬─────────────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────────────┐
│ HarnessOrchestrator (Agent 编排引擎)            │
│ 1. 收到问题 → 创建/获取会话                     │
│ 2. 调度各个 Agent 按顺序工作                     │
│ 3. 记录 Trace 和执行时间线                       │
└──────┬──────────┬──────────┬───────────────────┘
       │          │          │
       ▼          ▼          ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│ Agent 1  │ │ Agent 2  │ │ Agent 3  │
│ 检索 Agent│ │ 回答 Agent│ │ QA 质检  │
│          │ │          │ │ Agent    │
│ ① 保存   │ │ ③ 构建   │ │ ⑤ 评分   │
│    用户消息│ │    System │ │    检查  │
│          │ │   Prompt  │ │    引用  │
│ ② 从知识 │ │ ④ 调用   │ │ ⑥ 记录   │
│    库搜索 │ │   DeepSeek│ │    Trace │
└──────────┘ └──────────┘ └──────────┘
       │          │          │
       └──────────┴──────────┘
                  │
                  ▼
返回给用户（JSON 响应 或 SSE 流式推送）
```

**各 Agent 的具体代码位置：**

| Agent 角色 | 代码文件 | 核心方法 |
|-----------|---------|---------|
| 编排引擎 | `HarnessOrchestrator.java` | `answer()`, `answerStream()` |
| 检索 Agent | `KnowledgeService.java` | `search()` |
| 回答 Agent | `AnswerComposer.java` | `compose()`, `composeStream()` |
| QA 质检 Agent | `HarnessOrchestrator.java` | `performQaReview()` |
| 工具 Agent | `ToolRegistry.java` | `invoke()` |
| 追踪 Agent | `TraceRecorder.java` | `record()` |

---

## 五、学习资源推荐

### Java

| 资源 | 类型 | 语言 | 适合 |
|------|------|------|------|
| 廖雪峰 Java 教程 | 在线教程 | 中文 | 入门 |
| 《Head First Java》 | 书籍 | 中文/英文 | 入门 |
| 《Java 核心技术》 | 书籍 | 中文 | 进阶 |
| B 站 Spring Boot 教程 | 视频 | 中文 | 实战 |

### 前端

| 资源 | 类型 |
|------|------|
| MDN Web Docs（Mozilla） | 官方文档，最权威 |
| 阮一峰 JavaScript 教程 | 中文入门 |
| FreeCodeCamp | 交互式学习 |

### 编程工具

| 工具 | 说明 |
|------|------|
| IntelliJ IDEA | 最好用的 Java IDE（推荐社区版） |
| VS Code | 轻量级代码编辑器 |
| Postman | API 测试工具（比 curl 更可视化） |

### AI Agent 与 LLM

| 资源 | 类型 | 说明 |
|------|------|------|
| DeepSeek API 文档 | 官方文档 | 本项目使用的模型，中文友好 |
| OpenAI API 文档 | 官方文档 | 行业标准，DeepSeek 兼容 OpenAI 格式 |
| 吴恩达《Prompt Engineering for Developers》 | 免费课程 | 提示词工程入门必看 |
| 吴恩达《Building Systems with ChatGPT》 | 免费课程 | LLM 应用系统设计 |
| LangChain 官方教程 | 在线教程 | 最流行的 LLM 应用框架 |
| 《LLM 应用开发实战》 | 书籍 | 中文，适合入门 |
| Anthropic Claude 文档 | 官方文档 | Agent 架构和 Tool Use 的最佳实践 |
| revfactory/harness | GitHub 项目 | 本项目参考的 Agent 架构 |

### AI 辅助学习

| 工具 | 说明 |
|------|------|
| ChatGPT / Claude | 可以问概念、改 Bug、解释代码 |
| GitHub Copilot | IDE 中的 AI 编程助手 |
| Cursor | AI-first 代码编辑器 |
| Claude Code | 终端内的 AI 编程助手（本项目使用） |

---

## 六、常见问题（FAQ）

### Q: 学完这些要多久？
A: 全职学习约 3-4 个月，业余学习约 6-8 个月。关键是动手练习。

### Q: 需要先学数据库吗？
A: Spring Data JPA 帮你屏蔽了很多 SQL 细节，但最好了解基本概念（表、行、主键）。

### Q: 不会前端能学后端吗？
A: 可以，本项目前端已经写好了。你可以先专注于理解 Java + Spring Boot。

### Q: 遇到报错怎么办？
A: 三步走：
1. 看报错信息的前 3 行（第一行通常是原因）
2. 去搜索引擎复制报错信息
3. 问 AI 助手（ChatGPT/Claude）把报错信息贴给它

### Q: 看不懂代码怎么办？
A: 从小的模块开始看——先看 `ApiResponse.java`（只有 10 行），再看 `AuthController.java`（30 行），逐步扩大到更大的类。

### Q: AI Agent 和普通的 API 有什么区别？
A: 普通的 API 是"你问它答"——输入参数，返回结果。AI Agent 则是一个**有自主决策能力的组件**：
- 它会根据上下文判断怎么做更好
- 它可以调用工具获取外部信息
- 它可以自我检查回答质量
- 多个 Agent 可以像团队一样协作
本项目中，`HarnessOrchestrator` 就是"项目经理"，`RetrievalAgent` 是"资料员"，`AnswerComposer` 是"写手"。

### Q: 没有 DeepSeek API Key 能运行这个项目吗？
A: 可以启动项目并看到页面，但问答功能会报错。可以在 `application.yml` 中配一个 mock 实现来离线测试。

### Q: LLM 的回答有时不准（AI 幻觉），怎么办？
A: 本项目通过几种方式减少幻觉：
1. **RAG** — 强制 LLM 基于知识库内容回答
2. **来源引用** — 要求用 `[1]`、`[2]` 标注信息来源
3. **QA Review** — 自动检查是否有"无来源断言"
4. **System Prompt 约束** — 明确告诉 LLM "优先依据知识库，不要编造"

---

> 最后记住：**编程不是背出来的，是练出来的。** 每学一个概念，就在项目里找到对应的代码，动手修改一下，看看会有什么变化。这样学得最快。
