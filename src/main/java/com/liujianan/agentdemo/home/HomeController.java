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
                    html, body { height: 100%; }
                    body {
                      margin: 0;
                      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif;
                      color: #172033;
                      background: #f6f8fb;
                      overflow: hidden;
                    }
                    .app {
                      height: 100vh;
                      display: grid;
                      grid-template-columns: 320px minmax(0, 1fr);
                    }
                    aside {
                      min-height: 0;
                      display: flex;
                      flex-direction: column;
                      border-right: 1px solid #d8e1ec;
                      background: #eef3f8;
                    }
                    .brand {
                      padding: 22px 20px 16px;
                      border-bottom: 1px solid #d8e1ec;
                    }
                    .brand h1 { margin: 0; color: #174a78; font-size: 22px; line-height: 1.2; }
                    .brand p { margin: 8px 0 0; color: #64748b; font-size: 13px; line-height: 1.5; }
                    .nav {
                      padding: 14px 12px;
                      display: grid;
                      gap: 6px;
                    }
                    .nav button {
                      width: 100%;
                      border: 0;
                      border-radius: 8px;
                      background: transparent;
                      color: #243b53;
                      padding: 10px 12px;
                      font: inherit;
                      font-weight: 700;
                      text-align: left;
                      cursor: pointer;
                    }
                    .nav button:hover, .nav button.active { background: #dce9f6; color: #174a78; }
                    .side-scroll {
                      min-height: 0;
                      overflow: auto;
                      padding: 0 14px 18px;
                    }
                    .panel {
                      display: none;
                      background: #ffffff;
                      border: 1px solid #d8e1ec;
                      border-radius: 8px;
                      padding: 14px;
                      margin-bottom: 12px;
                    }
                    .panel.active { display: block; }
                    h2 { margin: 0 0 12px; color: #174a78; font-size: 17px; }
                    h3 { margin: 16px 0 8px; color: #334155; font-size: 14px; }
                    label { display: block; margin: 10px 0 6px; color: #334155; font-size: 13px; font-weight: 700; }
                    input, textarea, select {
                      width: 100%;
                      border: 1px solid #c9d6e4;
                      border-radius: 8px;
                      padding: 10px 11px;
                      font: inherit;
                      background: #ffffff;
                    }
                    textarea { min-height: 92px; resize: vertical; }
                    button.primary, button.secondary {
                      border: 0;
                      border-radius: 8px;
                      padding: 10px 13px;
                      font: inherit;
                      font-weight: 800;
                      cursor: pointer;
                    }
                    button.primary { background: #174a78; color: #ffffff; }
                    button.primary:hover { background: #123a5f; }
                    button.secondary { background: #e7eef6; color: #174a78; }
                    .row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-top: 12px; }
                    .muted { color: #64748b; font-size: 13px; line-height: 1.5; }
                    .doc, .trace, .source {
                      border: 1px solid #e1e9f2;
                      border-radius: 8px;
                      padding: 10px;
                      background: #fbfdff;
                      margin-bottom: 8px;
                    }
                    .doc-title { font-weight: 800; color: #174a78; margin-bottom: 4px; }
                    .pill {
                      display: inline-flex;
                      align-items: center;
                      border-radius: 999px;
                      background: #e7eef6;
                      color: #174a78;
                      padding: 3px 8px;
                      margin: 6px 4px 0 0;
                      font-size: 12px;
                      font-weight: 700;
                    }
                    .chat {
                      height: 100vh;
                      min-width: 0;
                      display: grid;
                      grid-template-rows: auto minmax(0, 1fr) auto;
                      background: #ffffff;
                    }
                    .chat-header {
                      padding: 18px 28px;
                      border-bottom: 1px solid #e1e9f2;
                      display: flex;
                      align-items: center;
                      justify-content: space-between;
                      gap: 16px;
                    }
                    .chat-header h2 { margin: 0; font-size: 20px; }
                    .status { color: #64748b; font-size: 13px; }
                    .messages {
                      min-height: 0;
                      overflow-y: auto;
                      padding: 28px clamp(18px, 5vw, 72px);
                      background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
                    }
                    .message {
                      display: grid;
                      grid-template-columns: 34px minmax(0, 760px);
                      gap: 12px;
                      margin: 0 auto 18px;
                      max-width: 900px;
                    }
                    .avatar {
                      width: 34px;
                      height: 34px;
                      border-radius: 8px;
                      display: grid;
                      place-items: center;
                      font-size: 13px;
                      font-weight: 900;
                    }
                    .avatar.user { background: #174a78; color: #ffffff; }
                    .avatar.assistant { background: #e7eef6; color: #174a78; }
                    .bubble {
                      border: 1px solid #e1e9f2;
                      border-radius: 8px;
                      padding: 12px 14px;
                      background: #ffffff;
                      line-height: 1.65;
                      white-space: pre-wrap;
                      overflow-wrap: anywhere;
                    }
                    .message.user .bubble { background: #174a78; color: #ffffff; border-color: #174a78; }
                    .composer {
                      border-top: 1px solid #e1e9f2;
                      padding: 16px clamp(18px, 5vw, 72px) 18px;
                      background: #ffffff;
                    }
                    .composer-inner {
                      max-width: 900px;
                      margin: 0 auto;
                      display: grid;
                      grid-template-columns: minmax(0, 1fr) auto auto;
                      gap: 10px;
                      align-items: end;
                    }
                    .composer textarea {
                      min-height: 54px;
                      max-height: 150px;
                      resize: vertical;
                    }
                    .metrics {
                      max-width: 900px;
                      margin: 10px auto 0;
                      display: flex;
                      gap: 12px;
                      color: #64748b;
                      font-size: 12px;
                      flex-wrap: wrap;
                    }
                    .links a {
                      display: block;
                      color: #174a78;
                      font-weight: 800;
                      text-decoration: none;
                      margin: 8px 0;
                    }
                    pre { margin: 0; white-space: pre-wrap; word-break: break-word; font-size: 12px; }
                    @media (max-width: 820px) {
                      body { overflow: auto; }
                      .app { min-height: 100vh; height: auto; grid-template-columns: 1fr; }
                      aside { min-height: auto; }
                      .side-scroll { max-height: 46vh; }
                      .chat { height: 100vh; }
                      .composer-inner { grid-template-columns: 1fr; }
                    }
                  </style>
                </head>
                <body>
                  <div class="app">
                    <aside>
                      <div class="brand">
                        <h1>Knowledge Agent Platform</h1>
                        <p>左侧管理知识库、文档上传、工具和 Trace；右侧只保留问答对话。</p>
                      </div>
                      <nav class="nav">
                        <button class="active" data-panel="knowledge" onclick="showPanel('knowledge')">知识库</button>
                        <button data-panel="upload" onclick="showPanel('upload')">上传文档</button>
                        <button data-panel="tools" onclick="showPanel('tools')">工具调用</button>
                        <button data-panel="trace" onclick="showPanel('trace')">Trace</button>
                        <button data-panel="links" onclick="showPanel('links')">接口入口</button>
                      </nav>
                      <div class="side-scroll">
                        <section id="panel-knowledge" class="panel active">
                          <h2>知识库</h2>
                          <div class="row">
                            <button class="secondary" onclick="loadDocs()">刷新</button>
                            <button class="secondary" onclick="fillSample()">示例</button>
                          </div>
                          <div id="docList" style="margin-top:12px;"></div>
                          <h3>粘贴文本</h3>
                          <label for="title">标题</label>
                          <input id="title" value="Agent Harness">
                          <label for="content">内容</label>
                          <textarea id="content">Harness separates model adapter, context builder, memory, tools, trace and evaluation so each step can be debugged independently.</textarea>
                          <label for="tags">标签，逗号分隔</label>
                          <input id="tags" value="agent,harness">
                          <button class="primary" style="margin-top:10px;" onclick="addDoc()">添加到知识库</button>
                          <p id="docStatus" class="muted"></p>
                        </section>

                        <section id="panel-upload" class="panel">
                          <h2>上传文档</h2>
                          <label for="uploadTitle">文档标题</label>
                          <input id="uploadTitle" placeholder="默认使用文件名">
                          <label for="uploadTags">标签，逗号分隔</label>
                          <input id="uploadTags" value="upload,knowledge">
                          <label for="file">选择文件</label>
                          <input id="file" type="file" accept=".docx,.pdf,.txt,.md">
                          <button class="primary" style="margin-top:10px;" onclick="uploadFile()">上传并解析</button>
                          <p class="muted">支持 Word .docx、PDF、TXT、Markdown。上传后会自动切分为知识片段。</p>
                          <div id="uploadResult" class="doc muted">等待上传...</div>
                        </section>

                        <section id="panel-tools" class="panel">
                          <h2>工具调用</h2>
                          <label for="tool">工具</label>
                          <select id="tool" onchange="updateToolInput()">
                            <option value="calculator">calculator</option>
                            <option value="echo">echo</option>
                            <option value="http_mock">http_mock</option>
                          </select>
                          <label for="toolInput">输入</label>
                          <input id="toolInput" value="12 + 30">
                          <button class="primary" style="margin-top:10px;" onclick="invokeTool()">调用工具</button>
                          <h3>结果</h3>
                          <div id="toolResult" class="doc muted">等待调用...</div>
                        </section>

                        <section id="panel-trace" class="panel">
                          <h2>Trace</h2>
                          <button class="secondary" onclick="loadTraces()">刷新 Trace</button>
                          <div id="traceList" style="margin-top:12px;"></div>
                        </section>

                        <section id="panel-links" class="panel">
                          <h2>接口入口</h2>
                          <p class="links">
                            <a href="/swagger-ui/index.html">Swagger UI</a>
                            <a href="/api/documents">Documents JSON</a>
                            <a href="/api/tools">Tools JSON</a>
                            <a href="/api/traces">Trace JSON</a>
                            <a href="/api/evaluations">Evaluation JSON</a>
                          </p>
                          <p class="muted">普通演示使用右侧问答和左侧操作面板；JSON 链接用于接口调试。</p>
                        </section>
                      </div>
                    </aside>

                    <main class="chat">
                      <header class="chat-header">
                        <div>
                          <h2>知识问答</h2>
                          <div class="status" id="sessionStatus">未创建会话</div>
                        </div>
                        <button class="secondary" onclick="clearChat()">新对话</button>
                      </header>

                      <section id="messages" class="messages">
                        <div class="message assistant">
                          <div class="avatar assistant">AI</div>
                          <div class="bubble">你好，可以先在左侧粘贴文本或上传文档，然后在这里提问。我会根据知识库检索结果生成回答，并显示来源片段。</div>
                        </div>
                      </section>

                      <footer class="composer">
                        <div class="composer-inner">
                          <textarea id="question" placeholder="输入你的问题，例如：什么是 Agent Harness？">How does Agent Harness improve debugging?</textarea>
                          <button class="primary" onclick="ask()">发送</button>
                          <button class="secondary" onclick="askStream()">流式</button>
                        </div>
                        <div class="metrics">
                          <span>来源：<strong id="sourceCount">0</strong></span>
                          <span>Token 估算：<strong id="tokenCount">0</strong></span>
                          <span>耗时：<strong id="latency">0ms</strong></span>
                        </div>
                        <div id="sources" class="metrics"></div>
                      </footer>
                    </main>
                  </div>

                  <script>
                    let currentSessionId = null;
                    const $ = id => document.getElementById(id);
                    const escapeHtml = text => String(text ?? '').replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));

                    async function api(url, options) {
                      const res = await fetch(url, options);
                      const text = await res.text();
                      try { return JSON.parse(text); } catch { return {success:false, message:text}; }
                    }

                    function showPanel(name) {
                      document.querySelectorAll('.panel').forEach(panel => panel.classList.remove('active'));
                      document.querySelectorAll('.nav button').forEach(button => button.classList.remove('active'));
                      $('panel-' + name).classList.add('active');
                      document.querySelector(`[data-panel="${name}"]`).classList.add('active');
                      if (name === 'trace') loadTraces();
                      if (name === 'knowledge') loadDocs();
                    }

                    function addMessage(role, text) {
                      const wrap = document.createElement('div');
                      wrap.className = 'message ' + role;
                      wrap.innerHTML = `<div class="avatar ${role}">${role === 'user' ? '你' : 'AI'}</div><div class="bubble"></div>`;
                      wrap.querySelector('.bubble').textContent = text;
                      $('messages').appendChild(wrap);
                      $('messages').scrollTop = $('messages').scrollHeight;
                      return wrap.querySelector('.bubble');
                    }

                    function renderSources(sources = []) {
                      $('sources').innerHTML = sources.length
                        ? sources.map((s, index) => `<span title="${escapeHtml(s.content)}">[${index + 1}] ${escapeHtml(s.title)}</span>`).join('')
                        : '<span>暂无来源片段</span>';
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
                      const data = await api('/api/documents/upload', { method: 'POST', body: form });
                      if (data.success) {
                        $('uploadResult').innerHTML = `<strong>上传成功</strong><br>文件：${escapeHtml(data.data.filename)}<br>字符数：${data.data.characterCount}<br>知识片段：${data.data.chunkCount}`;
                        loadDocs();
                      } else {
                        $('uploadResult').textContent = '上传失败：' + data.message;
                      }
                    }

                    async function ask() {
                      const question = $('question').value.trim();
                      if (!question) return;
                      $('question').value = '';
                      addMessage('user', question);
                      const pending = addMessage('assistant', '正在检索知识库并生成回答...');
                      const data = await api('/api/chat', {
                        method: 'POST',
                        headers: {'Content-Type':'application/json'},
                        body: JSON.stringify({ question, sessionId: currentSessionId })
                      });
                      if (data.success) {
                        currentSessionId = data.data.sessionId;
                        pending.textContent = data.data.answer;
                        renderSources(data.data.sources);
                        $('sourceCount').textContent = data.data.sources.length;
                        $('tokenCount').textContent = data.data.promptTokens;
                        $('latency').textContent = data.data.latencyMs + 'ms';
                        $('sessionStatus').textContent = '会话：' + currentSessionId;
                        loadTraces();
                      } else {
                        pending.textContent = '请求失败：' + data.message;
                      }
                      $('messages').scrollTop = $('messages').scrollHeight;
                    }

                    async function askStream() {
                      const question = $('question').value.trim();
                      if (!question) return;
                      $('question').value = '';
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
                      $('traceList').innerHTML = traces.slice(0, 10).map(t => `
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
                      currentSessionId = null;
                      $('messages').innerHTML = '<div class="message assistant"><div class="avatar assistant">AI</div><div class="bubble">新对话已开始。你可以继续提问，也可以先在左侧补充知识库。</div></div>';
                      $('sources').innerHTML = '';
                      $('sessionStatus').textContent = '未创建会话';
                      $('sourceCount').textContent = '0';
                      $('tokenCount').textContent = '0';
                      $('latency').textContent = '0ms';
                    }

                    loadDocs();
                    loadTraces();
                    $('question').addEventListener('keydown', event => {
                      if (event.key === 'Enter' && !event.shiftKey) {
                        event.preventDefault();
                        ask();
                      }
                    });
                  </script>
                </body>
                </html>
                """;
    }
}
