# Knowledge Agent Platform 完整测试文档

## 概述

本文档覆盖本项目的全部功能测试场景，包含前端 UI 测试和后端 API 测试。测试分为以下模块：

1. **环境准备与启动**
2. **认证系统**（注册 / 登录 / 登出）
3. **会话管理**（新建 / 切换 / 删除会话）
4. **问答功能**（非流式 / 流式 / 上下文预览）
5. **知识库管理**（添加 / 上传 / 检索 / 删除）
6. **工具调用**
7. **运行时间线（Run Timeline）**
8. **Agent 团队与 Skills**
9. **评测系统（Evaluation / QA Review）**
10. **Trace 查看**
11. **边界与异常场景**

---

## 1. 环境准备与启动

### 前置条件

- JDK 17+
- Maven 3.6+
- DeepSeek API Key（配置在环境变量 `DEEPSEEK_API_KEY` 中）

### 启动步骤

```bash
# 可选：清理之前的构建和数据
rm -rf target data

# 编译并启动
mvn clean spring-boot:run
```

访问 `http://localhost:8081` 确认页面正常加载。

### 启动验证清单

| 检查项 | 方法 | 预期结果 |
|--------|------|----------|
| 页面加载 | 浏览器访问 http://localhost:8081 | 显示登录/注册弹窗 |
| JavaScript 无语法错误 | 浏览器 F12 → Console | 无红色错误 |
| 后端启动日志 | 终端日志 | 无 `ERROR`，显示 `Started KnowledgeAgentDemoApplication` |
| H2 控制台 | http://localhost:8081/h2-console | 可登录并查看数据库表 |

---

## 2. 认证系统测试

### 2.1 前端注册

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 打开页面 | 弹出登录/注册弹窗 |
| 2 | 点击「注册」标签 | 切换到注册表单 |
| 3 | 输入用户名 `testuser` | 输入正常 |
| 4 | 输入密码 `test123456` | 输入正常 |
| 5 | 点击「注册」按钮 | 注册成功，自动登录并跳转到主界面 |
| 6 | 验证右上角显示用户名 | 显示 `testuser` |
| 7 | 点击「退出」按钮 | 登出成功，返回登录弹窗 |

### 2.2 前端登录

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 使用已注册用户名 `testuser`、密码 `test123456` | 登录成功 |
| 2 | 输入错误密码 | 显示"用户名或密码错误" |
| 3 | 输入不存在的用户名 | 显示"用户名或密码错误" |
| 4 | 用户名空、密码少于6位后提交 | 显示表单验证错误 |

### 2.3 后端 API 测试

```bash
# 注册
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"apitest","password":"api123456"}'
# 预期: {"success":true,"data":{"token":"xxx","username":"apitest"}}

# 重复注册
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"apitest","password":"api123456"}'
# 预期: {"success":false,"message":"username already exists"}

# 登录
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"apitest","password":"api123456"}'
# 预期: {"success":true,"data":{"token":"xxx","username":"apitest"}}

# 错误密码
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"apitest","password":"wrongpassword"}'
# 预期: {"success":false,"message":"用户名或密码错误"}

# 参数验证
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"ab","password":"12"}'
# 预期: {"success":false,"message":"...个数必须在...和...之间..."}
```

---

## 3. 会话管理测试

### 3.1 前端操作

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 在聊天输入框中输入问题并发送（自动创建会话） | 左侧出现新会话条目 |
| 2 | 点击「新会话」按钮 | 清空聊天区，创建新会话 |
| 3 | 点击左侧已有会话条目 | 切换到该会话，显示历史消息 |
| 4 | 点击会话条目的 ✕ 按钮 | 删除会话，从列表中移除 |
| 5 | 删除正在查看的会话 | 自动切换到最近会话或空状态 |

### 3.2 后端 API 测试

```bash
TOKEN="<从登录响应中获取的 token>"

# 列出会话
curl http://localhost:8081/api/sessions \
  -H "Authorization: Bearer $TOKEN"
# 预期: {"success":true,"data":[...]}

# 获取具体会话
curl "http://localhost:8081/api/sessions/{id}" \
  -H "Authorization: Bearer $TOKEN"

# 删除会话
curl -X DELETE "http://localhost:8081/api/sessions/{id}" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 4. 问答功能测试

### 4.1 非流式问答

```bash
TOKEN="..."

# 最简单的问答（无知识库）
curl -X POST http://localhost:8081/api/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"","question":"你好，请做一个自我介绍"}'
# 预期: 返回 sessionId、回答内容、知识片段（可能为空）、token数、耗时

# 指定 sessionId 继续对话
curl -X POST http://localhost:8081/api/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"<上一步返回的sessionId>","question":"追问问题"}'
# 预期: 同一个 sessionId，回答内容延续上文
```

### 4.2 流式问答（SSE）

```bash
curl -X POST http://localhost:8081/api/chat/stream \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"sessionId":"","question":"流式输出测试"}'
# 预期: 收到多个 event:
#   event: session -> sessionId
#   event: sources -> 片段数量
#   event: delta -> 逐段输出回答文本
#   event: done -> latencyMs=xxx
```

### 4.3 前端操作

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 在输入框输入文字，按 Enter | 显示用户气泡，LLM 流式回答 |
| 2 | 输入框为空时按 Enter | 无反应或提示 |
| 3 | Shift+Enter | 换行，不发送 |
| 4 | 回答中 Markdown 被正确渲染 | 标题、粗体、列表、代码块、链接正确显示 |
| 5 | 回答底部展示知识来源 | 显示来源编号和标题 |
| 6 | 点击来源编号或"查看上下文" | 弹窗展示上下文详情（System Prompt、历史、知识片段、最终消息） |

---

## 5. 知识库管理测试

### 5.1 手动添加知识片段

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击左侧「知识库」面板 | 显示现有知识片段列表 |
| 2 | 输入标题、内容、标签（逗号分隔） | 输入正常 |
| 3 | 点击「添加到知识库」 | 新增片段，列表刷新 |
| 4 | 点击片段右侧的 ✕ 按钮 | 删除片段 |

### 5.2 文件上传

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击「上传文档」面板 | 显示上传区域 |
| 2 | 选择 .txt / .md / .pdf / .docx 文件 | 文件名显示 |
| 3 | 点击「上传」 | 上传成功，显示分割后的片段数量和字符数 |
| 4 | 切换到「知识库」面板 | 可以看到新上传的片段 |

### 5.3 后端 API 测试

```bash
# 列出知识片段
curl http://localhost:8081/api/documents \
  -H "Authorization: Bearer $TOKEN"

# 手动添加
curl -X POST http://localhost:8081/api/documents \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"RAG简介","content":"RAG（Retrieval-Augmented Generation）是一种结合检索和生成的架构。","tags":["RAG","AI"]}'

# 文件上传
curl -X POST http://localhost:8081/api/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.txt" \
  -F "title=测试文件" \
  -F "tags=test,upload"

# 搜索
curl "http://localhost:8081/api/documents/search?query=RAG&topK=3" \
  -H "Authorization: Bearer $TOKEN"

# 删除
curl -X DELETE "http://localhost:8081/api/documents/{id}" \
  -H "Authorization: Bearer $TOKEN"
```

### 5.4 知识库增强问答测试

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 向知识库添加一段独特内容（如"月光公司的使命是让夜晚更明亮"） | 添加成功 |
| 2 | 在聊天中提问"月光公司的使命是什么？" | 回答引用了刚添加的知识片段，并带有 `[1]` 来源标记 |
| 3 | 验证来源编号可点击 | 展开显示知识片段详情 |
| 4 | 提问知识库不存在的内容 | LLM 说明未命中知识库，给出通用回答 |

---

## 6. 工具调用测试

### 6.1 前端操作

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击左侧「工具」面板 | 显示可用工具列表（当前为 `calculator` 和 `random_number`） |
| 2 | 选择工具 | 输入框标签更新为工具说明 |
| 3 | 填入参数，点击「调用工具」 | 显示工具返回结果 |
| 4 | 点击「填入样例」 | 自动填入示例参数 |

### 6.2 后端 API

```bash
# 列出工具
curl http://localhost:8081/api/tools \
  -H "Authorization: Bearer $TOKEN"

# 调用计算器
curl -X POST http://localhost:8081/api/tools/calculator/invoke \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"input":{"a":10,"b":20,"op":"add"}}'
# 预期: {"success":true,"data":{"result":30}}

# 调用随机数
curl -X POST http://localhost:8081/api/tools/random_number/invoke \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"input":{"min":1,"max":100}}'
# 预期: {"success":true,"data":{"value":42}} (值随机)

# 调用不存在的工具
curl -X POST http://localhost:8081/api/tools/nonexistent/invoke \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"input":{}}'
# 预期: {"success":false,"message":"...not found..."}
```

---

## 7. 运行时间线测试

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 发送一条问答 | 回答正常生成 |
| 2 | 点击左侧「运行时间线」面板 | 显示当前会话的执行阶段列表 |
| 3 | 验证阶段包含 `USER_INPUT`、`CONTEXT_BUILD` 等 | 显示正确 |
| 4 | 点击某个阶段卡片 | 展开显示详情（属性、耗时等） |
| 5 | 切换到其他会话 | 时间线更新为对应会话的数据 |
| 6 | 清空会话 | 时间线清空或显示空状态 |

后端 API：

```bash
curl "http://localhost:8081/api/runs?sessionId={sessionId}" \
  -H "Authorization: Bearer $TOKEN"
# 预期: 返回有序的阶段列表，每段包含 stage/message/summary/attributes/durationMs
```

---

## 8. Agent 团队与 Skills 测试

### 8.1 准备测试数据

在项目根目录创建 `.claude/agents/` 和 `.claude/skills/` 目录及示例文件。

```bash
mkdir -p .claude/agents .claude/skills/rag-answer .claude/skills/document-ingestion
```

**`.claude/agents/retrieval-agent.md`：**
```markdown
# 检索 Agent
负责分析用户问题，从知识库中检索相关片段，并按相关性排序返回。
## Responsibilities
- 分析用户问题意图
- 执行关键词搜索
- 对检索结果排序
## Input
用户问题字符串
## Output
排序后的知识片段列表
```

**`.claude/skills/rag-answer/SKILL.md`：**
```markdown
# RAG 回答
## Overview
使用检索结果增强 LLM 回答质量的核心技能。
## Steps
1. 接收用户问题和检索结果
2. 构建包含上下文的提示词
3. 调用 LLM 生成回答
4. 添加来源引用
```

### 8.2 前端测试

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击左侧「Agents」面板 | 显示读取到的 Agent 卡片列表 |
| 2 | 点击 Agent 卡片 | 展开显示职责、输入、输出详情 |
| 3 | 点击左侧「Skills」面板 | 显示读取到的 Skill 卡片列表 |
| 4 | 点击 Skill 卡片 | 展开显示详细内容 |
| 5 | 删除 `.claude/agents/` 中的文件后刷新 | 显示"未找到 Agent 定义"的提示 |

### 8.3 后端 API

```bash
curl http://localhost:8081/api/agents \
  -H "Authorization: Bearer $TOKEN"
# 预期: 返回 agents 列表（如果存在 .claude/agents/ 目录）

curl http://localhost:8081/api/skills \
  -H "Authorization: Bearer $TOKEN"
# 预期: 返回 skills 列表（如果存在 .claude/skills/ 目录）
```

---

## 9. 评测系统测试

### 9.1 评测用例管理

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击左侧「评测」面板 | 显示评测管理界面 |
| 2 | 输入问题、期望关键词、反馈 | 输入正常 |
| 3 | 点击「添加评测用例」 | 用例出现在列表中 |

后端 API：

```bash
# 添加评测用例
curl -X POST http://localhost:8081/api/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"RAG是什么？","expectedKeywords":["检索","生成","知识库"],"feedback":""}'

# 列出评测用例
curl http://localhost:8081/api/evaluations \
  -H "Authorization: Bearer $TOKEN"
```

### 9.2 运行评测

```bash
# 运行单个评测
curl -X POST http://localhost:8081/api/evaluations/run \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"caseId":1,"answer":"RAG（Retrieval-Augmented Generation）是一种结合检索[1]和生成[2]的架构，通过从知识库中检索相关内容来增强大模型的回答质量。"}'
# 预期: 返回评分结果，包含 retrievalHit、citationPresent、keywordMatch、score
```

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 添加评测用例后，发送一条相关问答 | 答案生成 |
| 2 | 点击回答区域的「QA 评测」按钮 | 弹出评测结果弹窗 |
| 3 | 验证弹窗显示各项指标（命中知识库、引用来源、关键词匹配、评分） | 内容完整 |
| 4 | 点击「运行全部评测」 | 对所有评测用例执行评分，显示汇总（通过数/总数、平均分） |

### 9.3 QA Review 独立 API

```bash
curl -X POST http://localhost:8081/api/qa/review \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question":"RAG是什么？",
    "answer":"RAG是一种结合检索[1]和生成[2]的架构。",
    "sources":[{"title":"RAG论文","content":"RAG（Retrieval-Augmented Generation）"}],
    "expectedKeywords":["检索","生成","RAG"]
  }'
# 预期: 返回完整 QA 评测结果，包含分数和摘要
```

---

## 10. Trace 查看测试

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 发送一条问答 | 生成 Trace 事件 |
| 2 | 点击左侧「Trace」面板 | 显示最近的 Trace 事件列表 |
| 3 | 验证每行显示 stage、message 和时间 | 内容正确 |

后端 API：

```bash
curl "http://localhost:8081/api/traces?limit=10" \
  -H "Authorization: Bearer $TOKEN"
# 预期: 返回最近 N 条 Trace 事件
```

---

## 11. 边界与异常场景测试

### 11.1 认证边界

| 场景 | 操作 | 预期结果 |
|------|------|----------|
| 无 Token 访问 API | `curl` 不加 `Authorization` 头 | 返回 401 或 `success:false` |
| Token 过期 | 使用过期 Token | 返回认证错误 |
| 空用户名注册 | `{"username":"","password":"test123456"}` | 参数验证错误 |
| 用户名超长 | 31 个字符的用户名 | 参数验证错误 |
| 密码超短 | 少于 6 位 | 参数验证错误 |

### 11.2 问答边界

| 场景 | 操作 | 预期结果 |
|------|------|----------|
| 空问题 | `{"question":""}` | 参数验证错误 |
| 超长问题 | 数千字的问题 | 正常处理（LLM 有上下文限制） |
| 空会话 | 未登录直接调用 API | 401 |
| DeepSeek 不可用 | 设置错误的 API Key | 返回友好的错误提示 |
| 流式中断 | SSE 连接中断 | 服务端检测到 onError/onTimeout |

### 11.3 知识库边界

| 场景 | 操作 | 预期结果 |
|------|------|----------|
| 上传空文件 | 0 字节文件 | 报错提示 |
| 上传不支持格式 | `.exe`、`.zip` 等 | 报错"不支持的文件格式" |
| 上传超大文件 | > 20MB | Multipart 大小限制错误 |
| 搜索空字符串 | `query=` | 返回空结果或全部结果 |
| 删除不存在的片段 | `DELETE /documents/999999` | 返回 `false` |

### 11.4 前端边界

| 场景 | 操作 | 预期结果 |
|------|------|----------|
| 快速连续点击 | 快速多次点击"发送" | 防止重复发送或正确处理 |
| 标签页切换 | 发送问题后切换到其他面板再回来 | 内容不受影响 |
| 多窗口 | 同时在两个浏览器标签页登录同一用户 | 各自独立运作 |
| 刷新页面 | F5 刷新 | 保持登录状态（Token 在 localStorage） |
| 清除 localStorage | `localStorage.clear()` 后刷新 | 回到登录弹窗 |

---

## 12. 自动化测试建议

### 12.1 后端自动化测试

可以使用以下工具进行后端 API 的自动化测试：

```bash
#!/bin/bash
# test_all.sh - 后端 API 完整自动化测试
set -e

BASE="http://localhost:8081"
PASS=0
FAIL=0

check() {
  local desc="$1"
  local expected="$2"
  local actual="$3"
  if echo "$actual" | grep -q "$expected"; then
    echo "  ✅ $desc"
    ((PASS++))
  else
    echo "  ❌ $desc (expected: $expected)"
    echo "     got: $actual"
    ((FAIL++))
  fi
}

echo "=== 1. 注册 ==="
RESP=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"autotest","password":"auto123456"}')
check "注册成功" '"success":true' "$RESP"

TOKEN=$(echo "$RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo "=== 2. 知识库 ==="
RESP=$(curl -s -X POST "$BASE/api/documents" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"自动化测试","content":"这是自动化测试内容","tags":["test"]}')
check "添加知识片段" '"success":true' "$RESP"

echo "=== 3. 问答 ==="
RESP=$(curl -s -X POST "$BASE/api/chat" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"","question":"你好"}')
check "问答请求" '"success":true' "$RESP"

echo ""
echo "=== 测试完成 ==="
echo "通过: $PASS  失败: $FAIL"
```

### 12.2 持续集成建议

可在 GitHub Actions 中配置：

```yaml
name: API Test
on: [push]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: mvn clean package -DskipTests
      - run: java -jar target/*.jar &
      - run: sleep 30
      - run: bash test_all.sh
      - run: kill $(jobs -p)
```

---

## 13. 测试通过标准

| 模块 | 最低通过率 |
|------|-----------|
| 认证系统 | 100%（4/4 场景） |
| 会话管理 | 100%（3/3 场景） |
| 问答功能 | 90%（核心场景必须通过） |
| 知识库管理 | 100%（5/5 场景） |
| 工具调用 | 100%（3/3 场景） |
| 运行时间线 | 100%（3/3 场景） |
| Agent/Skills | 90%（依赖目录结构） |
| 评测系统 | 100%（4/4 场景） |
| 边界与异常 | 80%（非关键场景可容忍失败） |

---

## 14. 常见问题排查

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| 页面白屏/JS 错误 | Java Text Block 中转义序列被错误处理 | 检查 `node --check` 生成的 JavaScript 是否有语法错误 |
| 登录失败 | 后端未启动或 H2 数据库锁定 | `taskkill /F /IM java.exe` 后重启 |
| API 返回 401 | Token 过期或未正确传递 | 重新登录获取新 Token |
| 问答超时 | DeepSeek API 不可用或网络问题 | 检查 `DEEPSEEK_API_KEY` 和网络连接 |
| 上传文件报错 | 文件格式不支持或超过大小限制 | 检查文件格式（支持 .txt/.md/.pdf/.docx）和大小（<20MB） |
| 端口被占用 | 旧进程未退出 | `netstat -ano | findstr :8081` 找到 PID 后 `taskkill /F /PID xxx` |
