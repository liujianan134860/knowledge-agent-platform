# 登录/注册按钮失效根因分析工作日志

## 问题描述

点击"登录"或"注册"按钮无反应，浏览器控制台报 JavaScript 语法错误。所有页面交互功能均不可用。

## 根因分析

本问题的根本原因是一个 JavaScript 正则表达式字面量中的 `\n` 在 Java Text Block 中被错误解析为换行符，导致生成的 JavaScript 代码出现语法错误，使得整个 `<script>` 块无法被浏览器解析执行。

### 详细技术分析

Java Text Block 的转义规则与普通 Java 字符串一致：`\n` 会被处理为实际换行符（0x0A）。当 Java 源码中出现以下 JavaScript 代码时：

```java
sourcesText.replace(/\n/g, '<br>')
```

Java 编译器将 `\n` 替换为实际换行符，导致生成的 HTML 中的 JavaScript 变成：

```javascript
sourcesText.replace(/
/g, '<br>')
```

正则表达式字面量 `/\n/g` 被切分到两行，JavaScript 引擎无法识别，抛出 `SyntaxError: Invalid regular expression: missing /`。

由于这是一个**编译期语法错误**，整个 `<script>` 块被浏览器拒绝执行。这导致所有 JavaScript 函数（包括 `showAuth()`、`hideAuth()`、`showRegister()`、`login()`、`register()` 等）均未定义，表现即为按钮点击无反应。

### 影响范围

- `HomeController.java` 第 1392 行：`sourcesText.replace(/\n/g, '<br>')`

此处的 `\n` 被 Java Text Block 处理为换行符，破坏了 JavaScript 正则表达式的语法结构。

### 之前误判

之前将问题归因为 `classList.toggle('expanded')` 中单引号的转义问题，并尝试了多种修复方式（HTML 实体 `&apos;`、字符串拼接等）。但实际上该处的单引号在 Java Text Block 中不会被特殊处理——Java 中 `\'` 不是合法转义序列，Text Block 会原样输出 `'`。

## 修复方法

将 `HomeController.java` 第 1392 行的 `\n` 改为 `\\n`：

```java
// 修复前 (Java Text Block):
sourcesText.replace(/\n/g, '<br>')

// 修复后 (Java Text Block):
sourcesText.replace(/\\n/g, '<br>')
```

### 原理

- `\\n` 在 Java Text Block 中被转义为 `\n`（一个反斜杠后跟字母 n）
- 生成的 JavaScript 为 `sourcesText.replace(/\n/g, '<br>')`
- JavaScript 中的 `/\n/g` 是合法正则表达式，匹配换行符

### 验证方法

1. `node --check` 检查生成的 JavaScript 文件通过语法验证
2. `node -e "new Function(script)"` 确认 JavaScript 代码可被解析
3. `curl http://localhost:8081/ | grep` 确认 `/\n/g` 在输出中为合法的两个字符（反斜杠+n）

## 经验教训

1. Java Text Block 中 `\n`、`\t`、`\"` 等转义序列会被处理，即使是出现在 JavaScript 代码段中
2. 如果需要 JavaScript 中的 `\n`（正则或字符串），Java 源码必须写成 `\\n`
3. 单个 JavaScript 语法错误会使整个 `<script>` 块失效，导致所有功能不可用
4. 排查时应当先检查浏览器控制台是否有语法错误，而非仅关注运行时错误

## 相关文件

- `src/main/java/com/liujianan/agentdemo/home/HomeController.java` - 第 1392 行（修复处）
- `page.html` / `page_debug.html` - 由旧版本代码生成，同样存在该问题
