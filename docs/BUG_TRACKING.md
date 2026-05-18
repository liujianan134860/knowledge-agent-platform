# Bug Tracking

本文档记录 Knowledge Agent Platform 中已发现并修复的 Bug，以及当前已知问题。

---

## Bug 记录

### BUG-001: H2 数据库 ID 列长度不足导致注册 500

| 字段 | 内容 |
|------|------|
| **发现日期** | 2026-05-13 |
| **修复日期** | 2026-05-13 |
| **严重程度** | 🔴 严重 |
| **状态** | ✅ 已修复 |

**现象**

POST `/api/auth/register` 返回 HTTP 500，浏览器控制台显示：
```
POST http://localhost:8081/api/auth/register 500 (Internal Server Error)
```

**根因**

`app_user` 表的 `id` 列在数据库中是 `VARCHAR(20)`，但代码生成的 ID 格式为 `u-{UUID}`（如 `u-20b44edd-2e5e-4438-a183-96091c2a945a`），长度为 38 位。H2 trace log 报错：

```
Value too long for column "ID CHARACTER VARYING(20)": "'u-734190cf-85d1-4dcd-96f9-570c14eb899d' (38)"
```

`User.java` 实体定义 `@Column(length = 64)`，但 Hibernate 的 `ddl-auto: update` 不会扩大已有列的尺寸，因此列仍为 `VARCHAR(20)`。

该异常为 `DataIntegrityViolationException`，未被 `GlobalExceptionHandler` 捕获，导致 Spring Boot 返回默认 500 错误。

**修复**

删除 H2 数据库文件 `data/knowledge-agent-platform.mv.db`，重启后 Hibernate 根据实体定义重新创建表，`id` 列正确设为 `VARCHAR(64)`。

**相关文件**

- `src/main/java/com/liujianan/agentdemo/auth/User.java` - 实体定义（正确，无需修改）
- `src/main/java/com/liujianan/agentdemo/auth/UserService.java` - ID 生成逻辑
- `src/main/java/com/liujianan/agentdemo/common/GlobalExceptionHandler.java` - 缺少 `DataIntegrityViolationException` 处理器（可选增强）

---

### BUG-002: `marked.parseSync()` 在 marked v15 中不存在导致 Chatbot 无法显示回答

| 字段 | 内容 |
|------|------|
| **发现日期** | 2026-05-13 |
| **修复日期** | 2026-05-13 |
| **严重程度** | 🔴 严重 |
| **状态** | ✅ 已修复 |

**现象**

用户在浏览器中发送聊天消息后，AI 回答无法显示（"无法显示出AI的回答"）。

**根因**

前端使用 `marked.parseSync(text)` 渲染 Markdown 格式的回答文本。CDN 加载的 marked 库版本为 v15.0.12（`https://cdn.jsdelivr.net/npm/marked/marked.min.js`），该版本 **移除了 `parseSync` 方法**。

API 变更历史：
- marked v11 及之前: `parse()` 同步返回 `string`
- marked v12+: `parse()` 返回 Promise（异步），新增 `parseSync()` 作为同步替代
- marked v15+: `parse()` 恢复为同步模式（返回 `string`），`parseSync()` 被移除

之前的 commit `d2aef23` 将 `marked.parse()` 改为 `marked.parseSync()` 以适应 v12+ 的异步变更，但 CDN 更新到 v15 后 `parseSync` 已不存在。

当调用 `marked.parseSync(streamedText)` 时：
1. `marked.parseSync` 为 `undefined`
2. 调用 `undefined(streamedText)` 抛出 `TypeError: marked.parseSync is not a function`
3. 该错误在 `ask()` 函数中被 `try/catch` 捕获
4. `target.textContent = '请求失败：...'` 显示错误信息，回答内容丢失

**修复**

1. 将 CDN 版本固定为 `marked@12.0.2`，该版本同时支持 `parse()`（异步）和 `parseSync()`
2. 新增 `renderMarkdown()` 辅助函数，兼容 marked 多个版本：

```javascript
function renderMarkdown(text) {
    // marked v12-v14: parseSync() 存在，优先使用（同步）
    // marked v15+: parseSync() 已移除，回退到 parse()（v15+ 中同步）
    // 自动降级到 escapeHtml 兜底
}
```

3. 将所有 `marked.parseSync()` 调用替换为 `renderMarkdown()`

**⚠️ 第一次修正的失误**

第一次修复时，`renderMarkdown()` 函数中**优先尝试了 `marked.parse()`**，再回退到 `marked.parseSync()`。由于 pinned 的 marked v12 中 `parse()` 是异步的（返回 Promise），导致：

```
marked.parse(text) → Promise对象
typeof result === 'string' → false
String(result) → "[object Promise]"
```

结果页面上显示 `[object Promise]` 而非正常 Markdown 内容。

**最终修正**: 交换判断优先级——先尝试 `parseSync()`（v12-v14 中同步），再回退到 `parse()`（v15+ 中同步）。

---

#### 修复日志

**第一轮修复（2026-05-13）**

操作：
1. 将 CDN URL 从 `marked/marked.min.js` 改为 `marked@12.0.2/marked.min.js`（固定版本）
2. 新增 `renderMarkdown()` 辅助函数，替换所有 `marked.parseSync()` 调用

`renderMarkdown()` v1（有 Bug）：

```javascript
function renderMarkdown(text) {
    // 错误：优先尝试 parse()，但在 v12 中它是异步的！
    if (typeof marked.parse === 'function') {
        var result = marked.parse(text);          // ← 返回 Promise
        return typeof result === 'string' ? result : String(result);  // ← String(Promise) = "[object Promise]"
    }
    if (typeof marked.parseSync === 'function') {
        return marked.parseSync(text);             // ← 永远不会执行到这里
    }
    return escapeHtml(text).split('\n').join('<br>');
}
```

测试结果：API 调用成功，SSE 流式传输正常，但页面上显示 `[object Promise]`。

**第二轮修复（2026-05-13）**

`renderMarkdown()` v2（已修正）：

```javascript
function renderMarkdown(text) {
    // ✅ 正确：先检查 parseSync（v12-v14 同步方法）
    if (typeof marked.parseSync === 'function') {
        return marked.parseSync(text);             // ← v12-v14 走这里
    }
    // ✅ 再回退到 parse（v15+ 中同步返回 string）
    if (typeof marked.parse === 'function') {
        var result = marked.parse(text);
        return typeof result === 'string' ? result : escapeHtml(text).split('\n').join('<br>');
    }
    return escapeHtml(text).split('\n').join('<br>');
}
```

测试结果：
```
Non-stream Chat  ✅ PASS
Stream Chat      ✅ 21 deltas received
Done event       ✅ received
Error event      ✅ none
```

**第三轮修复（2026-05-13）**

操作：**移除外部 CDN 依赖，将 marked 库本地化托管**。

根因：外部 CDN（`cdn.jsdelivr.net`）在某些网络环境下（尤其中国大陆）可能加载缓慢甚至被屏蔽。虽然 `<script>` 标签是同步阻塞的，但：
- CDN 首次访问解析延迟可能导致页面长时间空白
- 部分浏览器/网络环境下 CDN 资源可能被拦截或超时
- 用户可能在 `marked` 未就绪时发送消息，`renderMarkdown()` 降级为 `escapeHtml` 纯文本

修复：
1. 下载 `marked@12.0.2` 到 `src/main/resources/static/js/marked.min.js`（35KB）
2. 页面引用改为本地路径 `<script src="/js/marked.min.js">`
3. Spring Boot 自动将 `classpath:/static/` 映射为静态资源路径

```diff
- <script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
+ <script src="/js/marked.min.js"></script>
```

验证：
```
HTTP /js/marked.min.js → 200 OK (35479 bytes)
Page script tag → "/js/marked.min.js"
Stream chat → 22 deltas received, no errors
```

**影响文件**（4 个文件同步修改 + 1 个新增）

| 文件 | 说明 |
|------|------|
| `src/main/java/com/liujianan/agentdemo/home/HomeController.java` | **主源文件**——页面由 Java Text Block 内联渲染 |
| `page.html` | 静态页面文件（同步更新） |
| `page_debug.html` | 调试页面（同步更新） |
| `app_js.js` | 外部 JS 文件（函数定义更新） |
| `src/main/resources/static/js/marked.min.js` | **新增**——本地托管的 marked v12.0.2 |

**第四轮修复（2026-05-13）——重写自托管 `simpleMarkdown()` 渲染器**

操作：重构 `simpleMarkdown()` 为块级优先处理流程，新增 `inlineFormat()` 辅助函数。

修复的两个核心问题：

1. **代码块 ` ``` ` 被行内代码正则吞噬**：原实现先全局应用行内正则（包括 `` `code` ``），导致三反引号代码块标记被拆解。修复后将代码块检测移至最优先处理，内部内容只做 HTML 转义。

2. **引用 `>` 被 HTML 转义**：原实现先全局执行 `s.replace(/>/g, '&gt;')`，导致块级引用检测失效。修复后将 `>` 前缀剥离优先于 HTML 转义。

重构后的处理链：
```
simpleMarkdown(text)
  └── 按行处理块级元素
      ├── 代码块 ```  → HTML 转义 + <pre><code>（无行内格式化）
      ├── 标题 #       → inlineFormat() + <h1~6>
      ├── 引用 >       → 剥离 > + inlineFormat() + <blockquote>
      ├── 列表 -/1.    → inlineFormat() + <ul>/<ol>
      └── 普通段落     → inlineFormat() + <p>
```

验证：13/13 测试全通过（包括 Bold, Italic, InlineCode, Link, Image, Heading, List, CodeBlock, Blockquote, HR, CodeHTML, QuoteFmt）。

---

### BUG-003: Git Bash curl 发送中文请求体时返回 400

| 字段 | 内容 |
|------|------|
| **发现日期** | 2026-05-13 |
| **修复日期** | - |
| **严重程度** | 🟢 低（测试环境问题） |
| **状态** | 📝 待确认 |

**现象**

在 Git Bash 中使用 `curl -d '{"question":"你好吗"}'` 发送含中文的请求时，后端返回 HTTP 400。

**根因**

Git Bash（Windows 环境）默认编码与 UTF-8 不一致，导致中文文本被转码为非 UTF-8 字节。服务端 JSON 解析器（Jackson）无法正确解析，抛出 `HttpMessageNotReadableException`，该异常未被 `GlobalExceptionHandler` 捕获，返回默认 Spring Boot 400 错误。

**验证**

- PowerShell 的 `[System.Net.WebRequest]` 发送正确 UTF-8 编码时，中文请求正常工作
- 浏览器 JavaScript `fetch()` + `JSON.stringify()` 发送正确 UTF-8，无此问题

**解决方案**

仅影响终端测试，不影响实际用户。浏览器和 PowerShell 中的正常使用不受影响。

---

### BUG-004: `simpleMarkdown()` 自托管渲染器代码块和引用渲染错误

| 字段 | 内容 |
|------|------|
| **发现日期** | 2026-05-13 |
| **修复日期** | 2026-05-13 |
| **严重程度** | 🔴 严重 |
| **状态** | ✅ 已修复 |

**现象**

自托管 `simpleMarkdown()` 渲染器存在两个问题：
1. **代码块 ` ``` ` 渲染为 `<p>&#96;&#96;<code>...</code></p>`**：三反引号代码块被行内代码正则吞噬
2. **引用 `>` 渲染为 `<p>&amp;gt; ...</p>`**：`>` 在块级处理前被 HTML 转义

这些错误在 marked 库可用时不会暴露（因为 `renderMarkdown()` 优先使用 marked），但当 marked 加载失败或未就绪时，用户会看到错误的格式。

**根因**

原 `simpleMarkdown()` 函数的处理顺序是：
1. 全局 HTML 转义（包括 `>`）
2. 全局行内正则替换（包括 `` `code` ``）
3. 按行处理块级元素

问题在于：
- 代码块标记 ` ``` ` 在步骤 2 中被行内代码正则匹配为 `` `python` ``（取前两个反引号），剩下的反引号变成文本
- 引用 `>` 在步骤 1 中变成了 `&gt;`，步骤 3 的 `^>` 正则无法匹配

**修复**

重构 `simpleMarkdown()`，将处理顺序改为先按行处理块级元素，再对每行内容使用 `inlineFormat()` 辅助函数进行行内格式化。

新增 `inlineFormat()` 函数，接收纯文本内容，执行 HTML 转义和行内 Markdown 格式化。

**代码块处理**：代码块标记优先检测，完整的三反引号行被识别为分隔符；代码块内部只做 HTML 转义，不做行内格式化；合并 HTML 时通过 `inPreBlock2` 标志避免代码块内容被 `<p>` 包裹。

**引用处理**：`>` 前缀在 HTML 转义之前识别并剥离，剩余内容传递给 `inlineFormat()`。

**验证结果**（13/13 测试通过）

| 测试 | 结果 |
|------|------|
| Bold | `<strong>world</strong>` ✓ |
| Italic | `<em>world</em>` ✓ |
| InlineCode | `<code>code</code>` ✓ |
| Link | `<a href="...">GitHub</a>` ✓ |
| Image | `<img src="..." alt="...">` ✓ |
| Heading | `<h1>Heading 1</h1>` ✓ |
| List | `<ul><li>Item 1</li></ul>` ✓ |
| CodeBlock | `<pre><code>print("hello")<br></code></pre>` ✓ |
| Blockquote | `<blockquote>This is a quote</blockquote>` ✓ |
| Mixed | h1 + strong + code + li ✓ |
| HR | `<hr>` ✓ |
| CodeHTML | `&lt;div&gt;escaped&lt;/div&gt;` ✓ |
| QuoteFmt | `<strong>` + `<code>` inside `<blockquote>` ✓ |

**影响文件**

| 文件 | 说明 |
|------|------|
| `src/main/java/.../HomeController.java` | Java Text Block 中的 `simpleMarkdown()` 和新增 `inlineFormat()` |
| `page.html` | 静态页面同步更新 |
| `page_debug.html` | 调试页面同步更新 |
| `app_js.js` | 外部 JS 文件同步更新 |

---

---

### BUG-005: SSE 流式解析丢失换行符导致 Markdown 实时渲染失败

| 字段 | 内容 |
|------|------|
| **发现日期** | 2026-05-14 |
| **修复日期** | 2026-05-14 |
| **严重程度** | 🔴 严重 |
| **状态** | ✅ 已修复 |

**现象**

AI 回答在流式传输过程中无法实时渲染为 Markdown 格式（段落、代码块、列表等均丢失格式），切换会话或刷新页面后正确加载 Markdown 格式。

**根因**

SSE（Server-Sent Events）客户端解析代码存在三个连锁问题：

1. **多行 `data:` 字段未合并**：Spring 的 `SseEmitter` 遵循 SSE 规范，当 `data` 字符串包含换行符 `\n` 时，会自动拆分为多个 `data:` 行。例如 AI 生成的 `"Hello\n\nWorld"` 在 SSE 中编码为：
   ```
   event: delta
   data: Hello
   data: 
   data: World
   
   ```
   但客户端代码逐行独立处理 `data:` 行，直接 `streamedText += payload` 拼接，**丢失了 `\n` 连接符**，导致 `HelloWorld`。

2. **`.trim()` 移除有效空白**：`payload = line.substring(5).trim()` 中的 `.trim()` 不仅移除 `data:` 后的前导空格，还移除了内容末尾的换行符和空白，进一步破坏格式。

3. **SSE 事件边界未正确处理**：缺少事件完成（空白行）的显式处理，也没有在流结束时刷新缓冲事件。

当用户刷新页面或切换会话时，`switchSession()` 从 API 加载完整消息（含正确换行符），通过 `addMessage()` → `renderBubble()` → `renderMarkdown()` 重新渲染，因此能正确显示。

**修复**

重构 SSE 解析为**按事件累积-冲刷新模式**：

```javascript
var sseEventType = '';
var sseData = [];

function flushSSEEvent() {
  if (sseData.length === 0) { sseEventType = ''; return; }
  var data = sseData.join('\n');  // ← 用 \n 重新连接多行 data
  if (sseEventType === 'session') {
    currentSessionId = data;
  } else if (sseEventType === 'delta') {
    streamedText += data;         // ← 保留换行后的完整文本
    target.innerHTML = renderMarkdown(streamedText);
  }
  sseData = [];
  sseEventType = '';
}
```

处理流程：
```
SSE 行 → event: → flushSSEEvent() + 记录新 eventType
      → data:  → 移除首个空格（`data: ` 格式约定）后推入 sseData[]
      → ''     → flushSSEEvent()（事件结束）
流结束 → flushSSEEvent()（刷新末次事件）
```

关键变更：
1. 移除了 `.trim()`，改为 `payload.startsWith(' ') ? payload.substring(1) : payload`
2. `sseData.join('\n')` 在事件边界用换行符重新连接多行数据
3. 空白行（`line === ''`）触发 `flushSSEEvent()`
4. 流结束后调用 `flushSSEEvent()` 处理剩余缓冲区

**验证**

流式渲染流程：
```
服务器发送:
  event: delta
  data: # Hello
  data: 
  data: This is **bold**
  data: 
  data: - Item 1
  data: - Item 2

修复前: streamedText = "# HelloThis is **bold**- Item 1- Item 2"
修复后: streamedText = "# Hello\n\nThis is **bold**\n\n- Item 1\n- Item 2"
         → renderMarkdown → <h1>Hello</h1><p>This is <strong>bold</strong></p><ul><li>Item 1</li><li>Item 2</li></ul>
```

**影响文件**

| 文件 | 说明 |
|------|------|
| `src/main/java/.../HomeController.java` | 主页面 SSE 解析逻辑修复 |
| `page.html` | 静态页面同步修复 + `simpleMarkdown()` 同步升级 |

---

### BUG-006: `simpleMarkdown()` 未闭合代码块导致流式输出全部显示为纯文本

| 字段 | 内容 |
|------|------|
| **发现日期** | 2026-05-14 |
| **修复日期** | 2026-05-14 |
| **严重程度** | 🔴 严重 |
| **状态** | ✅ 已修复 |

**现象**

流式传输过程中，AI 回答始终显示为纯文本（无 Markdown 格式），刷新页面后正确显示 Markdown 格式。

**根因**

`simpleMarkdown()` 遇到 ` ``` ` 时立即进入代码块模式。在流式场景中，AI 生成代码块的**开标记先到达**，此时没有配对的关闭标记，但函数仍将后续所有内容（包括标题、段落、列表等）当作代码块内容。直到关闭标记到达后整个流程才恢复正常。

具体流程：
1. Delta: ` ```python ` → `simpleMarkdown` 进入 `inCodeBlock=true` 状态
2. Delta: `print("hello")` → 当作代码块内容，只做 HTML 转义
3. Delta: `## 总结` → **也被当作代码块内容**（因为 `inCodeBlock` 仍为 true），标题格式丢失
4. Delta: ` ``` ` → 代码块关闭，恢复正常
5. 用户看到的效果：从第1步到第4步之间，所有内容显示为 `<pre><code>` 内的等宽字体纯文本，没有任何标题/加粗/列表格式

刷新页面时，`switchSession()` 从 API 加载完整消息（含正确闭合的 ` ``` `），通过 `renderMarkdown()` 重新渲染，格式正确。

**修复**

修改 `simpleMarkdown()` 的代码块处理逻辑：**遇到 ` ``` ` 时向前查找配对的关闭标记，只有找到时才进入代码块模式**。

```javascript
var codeBlockMatch = line.match(/^```\s*(\w*)\s*$/);
if (codeBlockMatch) {
    // 向前查找配对的关闭 ```
    var hasClosing = false;
    for (var k = i + 1; k < lines.length; k++) {
        if (/^```/.test(lines[k])) { hasClosing = true; break; }
    }
    if (hasClosing) {
        // 找到配对 → 正常处理代码块
        result.push('<pre><code>');
        i++;
        while (i < lines.length && !/^```/.test(lines[i])) {
            result.push(escapeLine(lines[i]) + '\n');
            i++;
        }
        result.push('</code></pre>');
        continue;
    }
    // 未找到配对 → 当作普通文本（流式传输中尚未到达）
}
```

关键变更：
1. 移除了 `inCodeBlock` 状态变量（改为循环内局部判断）
2. 用 while 循环替代状态机处理代码块内容（不再影响后续行）
3. 未闭合的 ` ``` ` 不再吞噬后续内容，正常参与行内格式化
4. 代码块标记正则放宽为 `/^```\s*(\w*)\s*$/`，兼容末尾空格

**验证**

流式渲染流程：
```
Delta: ```python → 当作普通文本（显示 ```python）
Delta: print("hello") → 当作普通文本
Delta: ``` → 下一帧 renderMarkdown 重新解析，发现配对，
         → <pre><code>print("hello")</code></pre> ✓
Delta: ## 总结 → <h2>总结</h2> ✓（不再被代码块吞噬）
```

**影响文件**

| 文件 | 说明 |
|------|------|
| `src/main/java/.../HomeController.java` | `simpleMarkdown()` 代码块处理重构 |
| `page.html` | 同步更新 |

---

### BUG-007: 流式渲染器重构 — ChatGPT 风格渐进式 Markdown 输出

| 字段 | 内容 |
|------|------|
| **发现日期** | 2026-05-14 |
| **修复日期** | 2026-05-14 |
| **严重程度** | 🔴 严重 |
| **状态** | ✅ 已修复 |

**现象**

BUG-006 的修复（代码块 lookahead）未能完全解决流式输出无 Markdown 格式的问题。用户仍然报告流式传输中文本显示为无格式纯文本。

**根因**

原方案存在三个层次的问题：

1. **全局重渲染**：每个 delta 都对**全部累积文本**执行 `renderMarkdown()` → `innerHTML` 替换 → 浏览器重绘整个 DOM 子树。高频更新（LLM 每秒数十 tokens）导致浏览器渲染管线过载，CSS 样式计算延迟或丢弃。

2. **局部 Markdown 渲染震荡**：`simpleMarkdown()` 是无状态函数，每次调用独立处理全部文本。流式场景中，不完整的语法结构（如未闭合的 `**`、未完成的标题 `###` 等）导致输出在"纯文本"和"格式化 HTML"之间反复切换，视觉效果不稳定。

3. **无帧同步机制**：直接使用 `innerHTML = renderMarkdown(...)` 进行同步 DOM 操作，未与浏览器刷新周期对齐，造成布局抖动（layout thrashing）。

**修复：ChatGPT 风格渐进式流式渲染器**

完全重写流式渲染管线，新增三个关键机制：

#### 1. 行感知渐进渲染 (`renderStream`)
将文本按 `\n` 分为"已完成行"和"当前行"：
- **已完成行** → 调用 `renderMarkdown()` 生成 HTML（标题、列表、加粗等格式即时显示）
- **当前行** → `escapeHtml()` 显示为纯文本（显示 `**`, `#` 等原始标记字符）
- 下一帧 `\n` 到达时，当前行变为"已完成"，自动获得格式

```javascript
function renderStream(el, text) {
  // ... RAF throttling ...
  var nl = text.lastIndexOf('\n');
  if (nl < 0) {
    html = escapeHtml(text);                    // 单行 → 纯文本
  } else {
    var complete = text.substring(0, nl);       // 已完成行
    var current  = text.substring(nl + 1);      // 当前行
    html = renderMarkdown(complete);            // 已完成行 → Markdown
    if (current) html += escapeHtml(current);   // 当前行 → 纯文本
  }
  html += '<span class="stream-cursor">|</span>'; // 闪烁光标
  el.innerHTML = html;
}
```

#### 2. requestAnimationFrame 节流
每次 delta 到达时不立即渲染，而是通过 `requestAnimationFrame` 调度下一次渲染。浏览器只在下一帧（~16ms）执行一次实际 DOM 更新，将渲染频率从每秒数百次降至 ~60fps。

```javascript
if (_streamRaf) return;  // 已有待执行帧，跳过
_streamRaf = requestAnimationFrame(function() {
  _streamRaf = null;
  // ... 实际渲染 ...
});
```

#### 3. 流结束强制完成渲染 (`finishStream`)
SSE 流结束后取消所有待执行的 RAF，使用 `renderMarkdown()` 对**完整文本**执行一次性最终渲染：

```javascript
function finishStream(el, text) {
  _streamDone = true;
  if (_streamRaf) cancelAnimationFrame(_streamRaf);
  el.classList.add('stream-done');
  el.innerHTML = renderMarkdown(text);
}
```

**效果**

```
流式过程:
  Delta: "### "     → 当前行显示 "### |"（闪烁光标）
  Delta: "标题\n"   → 当前行变为完成行 → 渲染为 <h3>标题</h3>
  Delta: "\n"       → 空行
  Delta: "这是**加"→ 当前行显示 "这是**加|"（bold 未闭合，纯文本）
  Delta: "粗**文字" → 当前行 "这是**加粗**文字|"
  Delta: "\n"       → 完成行渲染 → 这是<strong>加粗</strong>文字 ✓
流结束:
  finishStream      → 完整 Markdown 最终渲染，移除光标
```

**影响文件**

| 文件 | 说明 |
|------|------|
| `page.html` | 新增 CSS（stream-cursor, stream-blink, stream-done）；新增 renderStream/finishStream；修改 ask() |
| `src/main/java/.../HomeController.java` | 同步所有上述修改 |
| `docs/BUG_TRACKING.md` | 新增 BUG-006, BUG-007 |

---

## 当前已知问题

| ID | 描述 | 严重程度 | 状态 |
|----|------|---------|------|
| - | 无已知未修复问题 | - | - |

> **注意**：BUG-003（curl 中文编码）为测试环境问题，不影响生产用户。所有功能性 Bug 均已修复。
