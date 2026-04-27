package com.liujianan.agentdemo.home;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Knowledge Agent Platform</title>
                  <style>
                    * { box-sizing: border-box; }
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif; margin: 0; color: #172033; background: #f4f7fb; line-height: 1.55; }
                    header { background: #1f4e79; color: white; padding: 28px 40px; }
                    header h1 { margin: 0 0 6px; font-size: 30px; }
                    header p { margin: 0; opacity: .9; }
                    main { max-width: 1180px; margin: 24px auto 48px; padding: 0 20px; display: grid; gap: 18px; grid-template-columns: 1.1fr .9fr; }
                    section { background: white; border: 1px solid #dbe3ea; border-radius: 12px; padding: 20px; box-shadow: 0 8px 20px rgba(31,78,121,.06); }
                    h2 { color: #1f4e79; font-size: 18px; margin: 0 0 14px; }
                    label { display: block; font-weight: 650; margin: 10px 0 6px; }
                    input, textarea, select { width: 100%; border: 1px solid #cbd5e1; border-radius: 8px; padding: 10px 12px; font: inherit; }
                    textarea { min-height: 92px; resize: vertical; }
                    button { border: 0; border-radius: 8px; background: #1f4e79; color: white; padding: 10px 14px; font-weight: 700; cursor: pointer; margin-top: 12px; }
                    button.secondary { background: #e8eef5; color: #1f4e79; margin-left: 8px; }
                    pre { background: #0f172a; color: #e2e8f0; padding: 14px; border-radius: 8px; overflow: auto; min-height: 92px; white-space: pre-wrap; }
                    .links a { display: inline-block; margin-right: 12px; color: #1f4e79; font-weight: 700; text-decoration: none; }
                    .full { grid-column: 1 / -1; }
                    .muted { color: #64748b; font-size: 13px; }
                    @media (max-width: 860px) { main { grid-template-columns: 1fr; } }
                  </style>
                </head>
                <body>
                  <header>
                    <h1>Knowledge Agent Platform</h1>
                    <p>基于 Agent Harness 思路的知识库问答与工具调用平台</p>
                  </header>
                  <main>
                    <section>
                      <h2>知识库问答</h2>
                      <label for="question">输入问题</label>
                      <textarea id="question">How does Agent Harness improve debugging?</textarea>
                      <button onclick="ask()">提交问题</button>
                      <button class="secondary" onclick="loadDocs()">刷新知识库</button>
                      <pre id="answer">等待提问...</pre>
                    </section>

                    <section>
                      <h2>添加知识片段</h2>
                      <label for="title">标题</label>
                      <input id="title" value="Agent Harness">
                      <label for="content">内容</label>
                      <textarea id="content">Harness separates model adapter, context builder, memory, tools, trace and evaluation so each step can be debugged independently.</textarea>
                      <label for="tags">标签，逗号分隔</label>
                      <input id="tags" value="agent,harness">
                      <button onclick="addDoc()">添加文档</button>
                      <pre id="docResult">可添加新的知识片段后再提问。</pre>
                    </section>

                    <section>
                      <h2>工具调用</h2>
                      <label for="tool">工具</label>
                      <select id="tool">
                        <option value="calculator">calculator</option>
                        <option value="echo">echo</option>
                        <option value="http_mock">http_mock</option>
                      </select>
                      <label for="toolInput">输入</label>
                      <input id="toolInput" value="12 + 30">
                      <button onclick="invokeTool()">调用工具</button>
                      <pre id="toolResult">等待调用...</pre>
                    </section>

                    <section>
                      <h2>调用轨迹</h2>
                      <button onclick="loadTraces()">刷新 Trace</button>
                      <pre id="traceResult">问答或工具调用后可查看 Trace。</pre>
                    </section>

                    <section class="full">
                      <h2>知识库与接口入口</h2>
                      <p class="links">
                        <a href="/swagger-ui/index.html">Swagger UI</a>
                        <a href="/api/documents">Documents JSON</a>
                        <a href="/api/tools">Tools JSON</a>
                        <a href="/api/traces">Trace JSON</a>
                        <a href="/api/evaluations">Evaluation JSON</a>
                      </p>
                      <pre id="docsResult">点击“刷新知识库”查看当前文档片段。</pre>
                      <p class="muted">说明：JSON 链接用于接口调试；上方表单提供可视化演示入口。</p>
                    </section>
                  </main>
                  <script>
                    const pretty = data => JSON.stringify(data, null, 2);
                    async function request(url, options) {
                      const res = await fetch(url, options);
                      const text = await res.text();
                      try { return JSON.parse(text); } catch { return text; }
                    }
                    async function ask() {
                      const data = await request('/api/chat', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({question: document.getElementById('question').value})
                      });
                      document.getElementById('answer').textContent = pretty(data);
                      loadTraces();
                    }
                    async function addDoc() {
                      const tags = document.getElementById('tags').value.split(',').map(s => s.trim()).filter(Boolean);
                      const data = await request('/api/documents', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({
                          title: document.getElementById('title').value,
                          content: document.getElementById('content').value,
                          tags
                        })
                      });
                      document.getElementById('docResult').textContent = pretty(data);
                      loadDocs();
                    }
                    async function invokeTool() {
                      const name = document.getElementById('tool').value;
                      const input = document.getElementById('toolInput').value;
                      const data = await request(`/api/tools/${name}/invoke`, {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({input})
                      });
                      document.getElementById('toolResult').textContent = pretty(data);
                      loadTraces();
                    }
                    async function loadTraces() {
                      const data = await request('/api/traces');
                      document.getElementById('traceResult').textContent = pretty(data);
                    }
                    async function loadDocs() {
                      const data = await request('/api/documents');
                      document.getElementById('docsResult').textContent = pretty(data);
                    }
                    loadDocs();
                  </script>
                </body>
                </html>
                """;
    }
}
