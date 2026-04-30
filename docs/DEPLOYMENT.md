# Deployment Guide

本文档记录 Knowledge Agent Platform 的演示级上线流程。项目不需要购买域名，可直接使用 Render 自动分配的 `*.onrender.com` HTTPS 地址。

## 1. 部署前检查

确认仓库包含以下文件：

- `Dockerfile`
- `docker-compose.yml`
- `pom.xml`
- `src/main/resources/application.yml`

`application.yml` 中必须使用动态端口：

```yaml
server:
  port: ${PORT:8081}
```

Render 会自动注入 `PORT` 环境变量。如果写死 `8081`，平台可能无法正确探测服务。

## 2. Render 部署步骤

1. 打开 Render Dashboard: https://dashboard.render.com/
2. 使用 GitHub 登录。
3. 点击 `New +`。
4. 选择 `Web Service`。
5. 选择仓库：`liujianan134860/knowledge-agent-platform`。
6. 配置服务：
   - Name: `knowledge-agent-platform`
   - Runtime: `Docker`
   - Branch: `main`
   - Root Directory: 留空
   - Dockerfile Path: `./Dockerfile`
7. 点击 `Create Web Service`。
8. 等待 Build 和 Deploy 完成。

## 2.1 配置 DeepSeek API Key

在 Render 服务页面进入：

```text
Environment -> Add Environment Variable
```

添加：

```text
DEEPSEEK_API_KEY=你的 DeepSeek API Key
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_BASE_URL=https://api.deepseek.com
```

保存后执行：

```text
Manual Deploy -> Deploy latest commit
```

不要把 API Key 写入 GitHub 仓库、README、截图或前端页面。

部署完成后，Render 会生成类似下面的地址：

```text
https://knowledge-agent-platform.onrender.com
```

## 3. 验证地址

部署完成后依次访问：

- 首页：`/`（打开控制台后需先注册/登录）
- Swagger UI：`/swagger-ui/index.html`
- 文档列表：`/api/documents`
- 工具列表：`/api/tools`
- Agent 定义：`/api/agents`
- Skill 定义：`/api/skills`
- Trace 列表：`/api/traces`
- 评测样例：`/api/evaluations`

完整示例：

```text
https://knowledge-agent-platform.onrender.com/swagger-ui/index.html
https://knowledge-agent-platform.onrender.com/api/agents
https://knowledge-agent-platform.onrender.com/api/skills
```

## 4. 常见问题

### 4.1 Whitelabel Error Page 404

如果访问根路径 `/` 出现 404，通常说明项目没有配置首页。

当前项目已经提供：

```text
GET /
```

访问根路径会显示项目入口页。

### 4.2 第一次访问很慢

Render 免费实例可能会在长时间无访问后休眠。再次访问时需要冷启动，通常等待几十秒后刷新即可。

### 4.3 No open ports detected

确认 `application.yml` 使用：

```yaml
server:
  port: ${PORT:8081}
```

### 4.4 Swagger 页面打不开

确认访问路径为：

```text
/swagger-ui/index.html
```

不是 `/swagger-ui`。

## 5. 重新部署

每次 GitHub 仓库更新后，可以在 Render 服务页面执行：

```text
Manual Deploy -> Deploy latest commit
```

如果开启自动部署，Render 会在 `main` 分支更新后自动重新构建。
