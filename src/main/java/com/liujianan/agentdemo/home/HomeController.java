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
                    body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif; color: #172033; background: #f3f6fb; }
                    header { padding: 28px 36px; background: linear-gradient(135deg, #174a78, #256b9f); color: white; }
                    header h1 { margin: 0 0 6px; font-size: 30px; }
                    header p { margin: 0; opacity: .92; }
                    main { max-width: 1240px; margin: 20px auto 48px; padding: 0 18px; display: grid; grid-template-columns: 290px minmax(420px, 1fr) 330px; gap: 16px; }
                    section { background: #fff; border: 1px solid #dbe5ef; border-radius: 12px; padding: 18px; box-shadow: 0 10px 24px rgba(23, 74, 120, .06); }
                    h2 { margin: 0 0 14px; color: #174a78; font-size: 18px; }
                    h3 { margin: 14px 0 8px; font-size: 15px; color: #243b53; }
                    label { display: block; margin: 10px 0 6px; font-weight: 700; color: #334155; }
                    input, textarea, select { width: 100%; border: 1px solid #cbd5e1; border-radius: 8px; padding: 10px 11px; font: inherit; background: white; }
                    textarea { min-height: 96px; resize: vertical; }
                    button { border: 0; border-radius: 8px; background: #174a78; color: white; padding: 10px 13px; font-weight: 800; cursor: pointer; }
                    button:hover { background: #123a5f; }
                    button.secondary { background: #e7eef6; color: #174a78; }
                    button.ghost { background: transparent; color: #174a78; padding: 6px 0; }
                    .row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-top: 12px; }
                    .stack { display: grid; gap: 10px; }
                    .muted { color: #64748b; font-size: 13px; }
                    .pill { display: inline-flex; align-items: center; border-radius: 999px; background: #e7eef6; color: #174a78; padding: 3px 8px; margin: 3px 4px 0 0; font-size: 12px; font-weight: 700; }
                    .doc, .trace, .source { border: 1px solid #e2e8f0; border-radius: 10px; padding: 10px; background: #fbfdff; margin-bottom: 8px; }
                    .doc-title { font-weight: 800; color: #174a78; }
                    .chatbox { min-height: 260px; max-height: 520px; overflow: auto; padding: 12px; border: 1px solid #dbe5ef; border-radius: 10px; background: #f8fbff; }
                    .msg { max-width: 88%; margin-bottom: 10px; padding: 10px 12px; border-radius: 12px; white-space: pre-wrap; }
                    .user { margin-left: auto; background: #174a78; color: white; }
                    .assistant { background: white; border: 1px solid #dbe5ef; }
                    .metric { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 10px; }
                    .metric div { background: #eef5fb; border-radius: 8px; padding: 8px; text-align: center; }
                    .metric strong { display: block; color: #174a78; }
                    pre { margin: 0; white-space: pre-wrap; word-break: break-word; font-size: 12px; }
                    .links a { color: #174a78; font-weight: 800; text-decoration: none; display: inline-block; margin: 0 12px 8px 0; }
                    .wide { grid-column: 1 / -1; }
                    @media (max-width: 980px) { main { grid-template-columns: 1fr; } .msg { max-width: 100%; } }
                  </style>
                </head>
                <body>
                  <header>
                    <h1>Knowledge Agent Platform</h1>
                    <p>知识库问答、工具调用、Trace 观测与轻量评测的 Agent Harness 演示平台</p>
                  </header>

                  <main>
                    <section>
                      <h2>知识库</h2>
                      <div class="row">
                        <button onclick="loadDocs()">刷新文档</button>
                        <button class="secondary" onclick="fillSample()">填充示例</button>
                      </div>
                      <div id="docList" class="stack" style="margin-top:14px;"></div>

                      <h3>添加知识片段</h3>
                      <label for="title">标题</label>
                      <input id="title" value="Agent Harness">
                      <label for="content">内容</label>
                      <textarea id="content">Harness separates model adapter, context builder, memory, tools, trace and evaluation so each step can be debugged independently.</textarea>
                      <label for="tags">标签，逗号分隔</label>
                      <input id="tags" value="agent,harness">
                      <button style="margin-top:10px;" onclick="addDoc()">添加到知识库</button>
                      <p id="docStatus" class="muted"></p>

                      <h3>上传文档</h3>
                      <label for="uploadTitle">文档标题</label>
                      <input id="uploadTitle" placeholder="默认使用文件名">
                      <label for="uploadTags">标签，逗号分隔</label>
                      <input id="uploadTags" value="upload,knowledge">
                      <label for="file">选择文件</label>
                      <input id="file" type="file" accept=".docx,.pdf,.txt,.md">
                      <button style="margin-top:10px;" onclick="uploadFile()">上传并解析</button>
                      <p class="muted">支持 Word .docx、PDF、TXT、Markdown。上传后会自动切分为知识片段。</p>
                      <div id="uploadResult" class="doc muted">等待上传...</div>
                    </section>

                    <section>
                      <h2>知识问答</h2>
                      <div id="chatbox" class="chatbox">
                        <div class="msg assistant">你好，可以先添加知识片段，然后向我提问。我会返回回答、来源片段和调用轨迹。</div>
                      </div>
                      <label for="question">问题</label>
                      <textarea id="question">How does Agent Harness improve debugging?</textarea>
                      <div class="row">
                        <button onclick="ask()">发送问题</button>
                        <button class="secondary" onclick="askStream()">流式演示</button>
                        <button class="secondary" onclick="clearChat()">清空对话</button>
                      </div>
                      <div class="metric">
                        <div><strong id="sourceCount">0</strong><span class="muted">sources</span></div>
                        <div><strong id="tokenCount">0</strong><span class="muted">tokens</span></div>
                        <div><strong id="latency">0ms</strong><span class="muted">latency</span></div>
                      </div>
                      <h3>来源片段</h3>
                      <div id="sources"></div>
                    </section>

                    <section>
                      <h2>工具与观测</h2>
                      <label for="tool">工具</label>
                      <select id="tool" onchange="updateToolInput()">
                        <option value="calculator">calculator</option>
                        <option value="echo">echo</option>
                        <option value="http_mock">http_mock</option>
                      </select>
                      <label for="toolInput">输入</label>
                      <input id="toolInput" value="12 + 30">
                      <button style="margin-top:10px;" onclick="invokeTool()">调用工具</button>
                      <h3>工具结果</h3>
                      <div id="toolResult" class="doc muted">等待调用...</div>

                      <h3>Trace</h3>
                      <div class="row">
                        <button class="secondary" onclick="loadTraces()">刷新 Trace</button>
                      </div>
                      <div id="traceList" style="margin-top:10px;"></div>
                    </section>

                    <section class="wide">
                      <h2>接口入口</h2>
                      <p class="links">
                        <a href="/swagger-ui/index.html">Swagger UI</a>
                        <a href="/api/documents">Documents JSON</a>
                        <a href="/api/tools">Tools JSON</a>
                        <a href="/api/traces">Trace JSON</a>
                        <a href="/api/evaluations">Evaluation JSON</a>
                      </p>
                      <p class="muted">上方界面用于普通用户演示；这些 JSON 链接用于接口调试和验证。</p>
                    </section>
                  </main>

                  <script>
                    let currentSessionId = null;
                    const $ = id => document.getElementById(id);
                    const escapeHtml = text => String(text ?? '').replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));
                    async function api(url, options) {
                      const res = await fetch(url, options);
                      const text = await res.text();
                      try { return JSON.parse(text); } catch { return {success:false, message:text}; }
                    }
                    function addMessage(role, text) {
                      const div = document.createElement('div');
                      div.className = 'msg ' + role;
                      div.textContent = text;
                      $('chatbox').appendChild(div);
                      $('chatbox').scrollTop = $('chatbox').scrollHeight;
                      return div;
                    }
                    function renderSources(sources = []) {
                      $('sources').innerHTML = sources.length ? sources.map(s => `
                        <div class="source">
                          <div class="doc-title">${escapeHtml(s.title)}</div>
                          <div>${escapeHtml(s.content)}</div>
                          <div>${(s.tags || []).map(t => `<span class="pill">${escapeHtml(t)}</span>`).join('')}</div>
                        </div>`).join('') : '<p class="muted">暂无来源片段</p>';
                    }
                    async function loadDocs() {
                      const data = await api('/api/documents');
                      const docs = data.data || [];
                      $('docList').innerHTML = docs.map(d => `
                        <div class="doc">
                          <div class="doc-title">${escapeHtml(d.title)}</div>
                          <div class="muted">${escapeHtml(d.content)}</div>
                          <div>${(d.tags || []).map(t => `<span class="pill">${escapeHtml(t)}</span>`).join('')}</div>
                        </div>`).join('');
                    }
                    async function addDoc() {
                      const tags = $('tags').value.split(',').map(s => s.trim()).filter(Boolean);
                      const data = await api('/api/documents', {
                        method: 'POST',
                        headers: {'Content-Type':'application/json'},
                        body: JSON.stringify({ title: $('title').value, content: $('content').value, tags })
                      });
                      $('docStatus').textContent = data.success ? '已添加知识片段。' : '添加失败：' + data.message;
                      loadDocs();
                    }
                    async function uploadFile() {
                      const file = $('file').files[0];
                      if (!file) {
                        $('uploadResult').textContent = '请先选择文件。';
                        return;
                      }
                      const form = new FormData();
                      form.append('file', file);
                      if ($('uploadTitle').value.trim()) form.append('title', $('uploadTitle').value.trim());
                      if ($('uploadTags').value.trim()) form.append('tags', $('uploadTags').value.trim());
                      const res = await fetch('/api/documents/upload', { method: 'POST', body: form });
                      const text = await res.text();
                      let data;
                      try { data = JSON.parse(text); } catch { data = {success:false, message:text}; }
                      if (data.success) {
                        $('uploadResult').innerHTML = `<strong>上传成功</strong><br>文件：${escapeHtml(data.data.filename)}<br>字符数：${data.data.characterCount}<br>知识片段：${data.data.chunkCount}`;
                        loadDocs();
                      } else {
                        $('uploadResult').textContent = '上传失败：' + (data.message || text);
                      }
                    }
                    async function ask() {
                      const question = $('question').value.trim();
                      if (!question) return;
                      addMessage('user', question);
                      const data = await api('/api/chat', {
                        method: 'POST',
                        headers: {'Content-Type':'application/json'},
                        body: JSON.stringify({ question, sessionId: currentSessionId })
                      });
                      if (data.success) {
                        currentSessionId = data.data.sessionId;
                        addMessage('assistant', data.data.answer);
                        renderSources(data.data.sources);
                        $('sourceCount').textContent = data.data.sources.length;
                        $('tokenCount').textContent = data.data.promptTokens;
                        $('latency').textContent = data.data.latencyMs + 'ms';
                        loadTraces();
                      } else {
                        addMessage('assistant', '请求失败：' + data.message);
                      }
                    }
                    async function askStream() {
                      const question = $('question').value.trim();
                      if (!question) return;
                      addMessage('user', question);
                      const target = addMessage('assistant', '');
                      const res = await fetch('/api/chat/stream', {
                        method: 'POST',
                        headers: {'Content-Type':'application/json'},
                        body: JSON.stringify({ question, sessionId: currentSessionId })
                      });
                      const text = await res.text();
                      target.textContent = text.replaceAll('event:', '\\n').replaceAll('data:', '').trim();
                      loadTraces();
                    }
                    async function invokeTool() {
                      const name = $('tool').value;
                      const input = $('toolInput').value;
                      const data = await api(`/api/tools/${name}/invoke`, {
                        method: 'POST',
                        headers: {'Content-Type':'application/json'},
                        body: JSON.stringify({ input })
                      });
                      $('toolResult').innerHTML = `<pre>${escapeHtml(JSON.stringify(data, null, 2))}</pre>`;
                      loadTraces();
                    }
                    async function loadTraces() {
                      const data = await api('/api/traces');
                      const traces = data.data || [];
                      $('traceList').innerHTML = traces.slice(0, 8).map(t => `
                        <div class="trace">
                          <div class="doc-title">${escapeHtml(t.stage)}</div>
                          <div>${escapeHtml(t.message)}</div>
                          <div class="muted">${escapeHtml(t.sessionId)} · ${escapeHtml(t.createdAt)}</div>
                        </div>`).join('') || '<p class="muted">暂无 Trace</p>';
                    }
                    function updateToolInput() {
                      const v = $('tool').value;
                      $('toolInput').value = v === 'calculator' ? '12 + 30' : v === 'http_mock' ? 'GET /api/projects' : 'hello agent';
                    }
                    function fillSample() {
                      $('title').value = 'Tool Calling';
                      $('content').value = 'Tool calling uses registered tools with parameter schema, timeout, permission scope, execution trace and fallback handling.';
                      $('tags').value = 'tool,mcp,trace';
                    }
                    function clearChat() {
                      $('chatbox').innerHTML = '<div class="msg assistant">对话已清空，可以继续提问。</div>';
                      $('sources').innerHTML = '';
                    }
                    loadDocs();
                    loadTraces();
                  </script>
                </body>
                </html>
                """;
    }
}
