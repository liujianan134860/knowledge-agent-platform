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
                  <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
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
                      grid-template-columns: 300px minmax(0, 1fr);
                    }
                    aside {
                      min-height: 0;
                      display: flex;
                      flex-direction: column;
                      border-right: 1px solid #d8e1ec;
                      background: #eef3f8;
                    }
                    .sessions {
                      padding: 14px 14px 8px;
                      border-bottom: 1px solid #d8e1ec;
                    }
                    .sessions h2 {
                      margin: 0 0 8px;
                      color: #174a78;
                      font-size: 14px;
                      font-weight: 800;
                      text-transform: uppercase;
                      letter-spacing: 0.5px;
                    }
                    .session-item {
                      position: relative;
                      border: 1px solid #d8e1ec;
                      border-radius: 8px;
                      padding: 10px 28px 10px 12px;
                      margin-bottom: 6px;
                      background: #ffffff;
                      cursor: pointer;
                      transition: border-color 0.15s;
                    }
                    .session-item:hover { border-color: #174a78; }
                    .session-item.active { border-color: #174a78; background: #eaf2fc; }
                    .session-item .sess-title {
                      font-size: 13px;
                      font-weight: 700;
                      color: #174a78;
                      overflow: hidden;
                      text-overflow: ellipsis;
                      white-space: nowrap;
                    }
                    .session-item .sess-meta {
                      font-size: 11px;
                      color: #64748b;
                      margin-top: 3px;
                    }
                    .session-item .sess-del {
                      position: absolute;
                      top: 6px;
                      right: 8px;
                      width: 20px;
                      height: 20px;
                      border: 0;
                      border-radius: 4px;
                      background: transparent;
                      color: #94a3b8;
                      font-size: 14px;
                      line-height: 1;
                      cursor: pointer;
                      display: none;
                      padding: 0;
                    }
                    .session-item:hover .sess-del { display: block; }
                    .session-item .sess-del:hover { background: #fee2e2; color: #dc2626; }
                    .doc-del {
                      border: 0;
                      border-radius: 4px;
                      background: transparent;
                      color: #94a3b8;
                      font-size: 13px;
                      cursor: pointer;
                      padding: 2px 6px;
                      float: right;
                    }
                    .doc-del:hover { background: #fee2e2; color: #dc2626; }
                    .session-empty {
                      font-size: 12px;
                      color: #94a3b8;
                      padding: 8px 0;
                    }
                    .nav {
                      padding: 8px 14px;
                      display: flex;
                      flex-wrap: wrap;
                      gap: 4px;
                      border-bottom: 1px solid #d8e1ec;
                    }
                    .nav button {
                      border: 0;
                      border-radius: 6px;
                      background: transparent;
                      color: #475569;
                      padding: 6px 10px;
                      font: inherit;
                      font-size: 12px;
                      font-weight: 700;
                      cursor: pointer;
                      white-space: nowrap;
                    }
                    .nav button:hover, .nav button.active { background: #dce9f6; color: #174a78; }
                    .side-scroll {
                      min-height: 0;
                      overflow: auto;
                      padding: 10px 14px 18px;
                    }
                    .panel {
                      display: none;
                      background: #ffffff;
                      border: 1px solid #d8e1ec;
                      border-radius: 8px;
                      padding: 12px;
                      margin-bottom: 10px;
                    }
                    .panel.active { display: block; }
                    h3 { margin: 0 0 8px; color: #174a78; font-size: 14px; }
                    h4 { margin: 12px 0 6px; color: #334155; font-size: 13px; }
                    label { display: block; margin: 8px 0 4px; color: #334155; font-size: 12px; font-weight: 700; }
                    input, textarea, select {
                      width: 100%;
                      border: 1px solid #c9d6e4;
                      border-radius: 6px;
                      padding: 8px 10px;
                      font: inherit;
                      font-size: 13px;
                      background: #ffffff;
                    }
                    textarea { min-height: 72px; resize: vertical; }
                    button.primary, button.secondary {
                      border: 0;
                      border-radius: 6px;
                      padding: 8px 13px;
                      font: inherit;
                      font-size: 13px;
                      font-weight: 700;
                      cursor: pointer;
                    }
                    button.primary { background: #174a78; color: #ffffff; }
                    button.primary:hover { background: #123a5f; }
                    button.primary:disabled { background: #94a3b8; cursor: default; }
                    button.secondary { background: #e7eef6; color: #174a78; }
                    button.secondary:hover { background: #dce9f6; }
                    .row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-top: 8px; }
                    .muted { color: #64748b; font-size: 12px; line-height: 1.5; }
                    .doc, .trace {
                      border: 1px solid #e1e9f2;
                      border-radius: 6px;
                      padding: 8px;
                      background: #fbfdff;
                      margin-bottom: 6px;
                    }
                    .doc-title { font-weight: 800; color: #174a78; margin-bottom: 2px; font-size: 13px; }
                    .pill {
                      display: inline-flex;
                      align-items: center;
                      border-radius: 999px;
                      background: #e7eef6;
                      color: #174a78;
                      padding: 2px 7px;
                      margin: 4px 3px 0 0;
                      font-size: 11px;
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
                      padding: 14px 24px;
                      border-bottom: 1px solid #e1e9f2;
                      display: flex;
                      align-items: center;
                      justify-content: space-between;
                      gap: 16px;
                    }
                    .chat-header h3 { margin: 0; font-size: 18px; }
                    .status { color: #64748b; font-size: 12px; }
                    .messages {
                      min-height: 0;
                      overflow-y: auto;
                      padding: 24px clamp(16px, 5vw, 64px);
                      background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
                    }
                    .message {
                      display: flex;
                      gap: 12px;
                      margin: 0 auto 16px;
                      max-width: 900px;
                      align-items: flex-start;
                    }
                    .message.user { flex-direction: row-reverse; }
                    .avatar {
                      width: 32px;
                      height: 32px;
                      border-radius: 8px;
                      display: grid;
                      place-items: center;
                      font-size: 12px;
                      font-weight: 900;
                      flex-shrink: 0;
                    }
                    .avatar.user { background: #174a78; color: #ffffff; }
                    .avatar.assistant { background: #e7eef6; color: #174a78; }
                    .bubble {
                      border: 1px solid #e1e9f2;
                      border-radius: 8px;
                      padding: 12px 18px;
                      background: #ffffff;
                      line-height: 1.75;
                      overflow-wrap: anywhere;
                      max-width: 720px;
                      font-size: 15px;
                      color: #1e293b;
                    }
                    .bubble p { margin: 0 0 10px; }
                    .bubble p:last-child { margin-bottom: 0; }
                    .bubble h1, .bubble h2, .bubble h3, .bubble h4 {
                      margin: 16px 0 8px;
                      font-weight: 700;
                      line-height: 1.35;
                      color: #174a78;
                    }
                    .bubble h1 { font-size: 1.4em; }
                    .bubble h2 { font-size: 1.2em; }
                    .bubble h3 { font-size: 1.05em; }
                    .bubble ul, .bubble ol { margin: 6px 0 10px; padding-left: 22px; }
                    .bubble li { margin-bottom: 4px; }
                    .bubble code {
                      background: #f1f5f9;
                      border: 1px solid #e2e8f0;
                      border-radius: 4px;
                      padding: 1px 6px;
                      font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
                      font-size: 0.9em;
                      color: #be185d;
                    }
                    .bubble pre {
                      background: #0f172a;
                      color: #e2e8f0;
                      border-radius: 8px;
                      padding: 14px 18px;
                      margin: 10px 0;
                      overflow-x: auto;
                      font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
                      font-size: 13px;
                      line-height: 1.55;
                    }
                    .bubble pre code {
                      background: none;
                      border: 0;
                      padding: 0;
                      color: inherit;
                      font-size: inherit;
                    }
                    .bubble blockquote {
                      border-left: 3px solid #174a78;
                      margin: 10px 0;
                      padding: 4px 14px;
                      background: #f8fafc;
                      color: #475569;
                    }
                    .bubble table {
                      border-collapse: collapse;
                      margin: 10px 0;
                      width: 100%;
                    }
                    .bubble th, .bubble td {
                      border: 1px solid #e2e8f0;
                      padding: 8px 12px;
                      text-align: left;
                      font-size: 14px;
                    }
                    .bubble th { background: #f1f5f9; font-weight: 700; }
                    .bubble strong { font-weight: 700; color: #0f172a; }
                    .bubble a { color: #2563eb; text-decoration: underline; }
                    .bubble hr { border: 0; border-top: 1px solid #e2e8f0; margin: 14px 0; }
                    .message.user .bubble { background: #174a78; color: #ffffff; border-color: #174a78; }
                    .message.user .bubble strong { color: #ffffff; }
                    .message.user .bubble code { background: rgba(255,255,255,0.15); border-color: rgba(255,255,255,0.2); color: #fbbf24; }
                    .message.user .bubble pre { background: rgba(0,0,0,0.3); color: #e2e8f0; }
                    .message.user .bubble a { color: #93c5fd; }
                    .message.user .bubble blockquote { border-color: rgba(255,255,255,0.4); background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.85); }
                    .message.user .bubble h1, .message.user .bubble h2, .message.user .bubble h3, .message.user .bubble h4 { color: #ffffff; }
                    .message.user .bubble th { background: rgba(255,255,255,0.1); }
                    .message.user .bubble th, .message.user .bubble td { border-color: rgba(255,255,255,0.2); }
                    .composer {
                      border-top: 1px solid #e1e9f2;
                      padding: 14px clamp(16px, 5vw, 64px) 16px;
                      background: #ffffff;
                    }
                    .composer-inner {
                      max-width: 900px;
                      margin: 0 auto;
                      display: grid;
                      grid-template-columns: minmax(0, 1fr) auto;
                      gap: 10px;
                      align-items: end;
                    }
                    .composer textarea {
                      min-height: 50px;
                      max-height: 140px;
                      resize: vertical;
                      font-size: 14px;
                    }
                    .links a {
                      display: inline-block;
                      color: #174a78;
                      font-weight: 700;
                      text-decoration: none;
                      margin: 3px 12px 3px 0;
                      font-size: 13px;
                    }
                    .links a:hover { text-decoration: underline; }
                    pre { margin: 0; white-space: pre-wrap; word-break: break-word; font-size: 12px; }
                    /* === Run Timeline === */
                    .run-stage {
                        border: 1px solid #e1e9f2;
                        border-radius: 8px;
                        margin-bottom: 8px;
                        overflow: hidden;
                    }
                    .run-stage.expanded { border-color: #174a78; }
                    .run-stage-header {
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        padding: 8px 12px;
                        cursor: pointer;
                        background: #f8faff;
                    }
                    .run-stage-header:hover { background: #eaf2fc; }
                    .run-stage-icon {
                        width: 28px; height: 28px;
                        border-radius: 6px;
                        display: grid; place-items: center;
                        font-size: 12px;
                        flex-shrink: 0;
                    }
                    .run-stage-icon.done { background: #d1fae5; color: #065f46; }
                    .run-stage-icon.wait { background: #fef3c7; color: #92400e; }
                    .run-stage-icon.fail { background: #fee2e2; color: #dc2626; }
                    .run-stage-name { font-weight: 700; font-size: 12px; color: #1e293b; flex: 1; }
                    .run-stage-dur { font-size: 11px; color: #94a3b8; }
                    .run-stage-arrow { color: #94a3b8; font-size: 10px; }
                    .run-stage-body {
                        display: none;
                        padding: 8px 12px 12px;
                        border-top: 1px solid #e1e9f2;
                        font-size: 12px;
                        color: #475569;
                        background: #ffffff;
                    }
                    .run-stage.expanded .run-stage-body { display: block; }
                    .run-stage-body pre { font-size: 11px; background: #f8fafc; padding: 6px 8px; border-radius: 4px; margin: 4px 0; }
                    .run-empty { color: #94a3b8; font-size: 13px; padding: 12px 0; text-align: center; }

                    /* === Agent Team === */
                    .agent-card {
                        border: 1px solid #e1e9f2;
                        border-radius: 8px;
                        margin-bottom: 8px;
                        overflow: hidden;
                    }
                    .agent-card.expanded { border-color: #174a78; }
                    .agent-header {
                        display: flex;
                        align-items: center;
                        gap: 10px;
                        padding: 10px 12px;
                        cursor: pointer;
                        background: #f8faff;
                    }
                    .agent-header:hover { background: #eaf2fc; }
                    .agent-avatar {
                        width: 36px; height: 36px;
                        border-radius: 8px;
                        display: grid; place-items: center;
                        font-size: 14px; font-weight: 800;
                        flex-shrink: 0;
                    }
                    .agent-avatar.color1 { background: #dbeafe; color: #1e40af; }
                    .agent-avatar.color2 { background: #d1fae5; color: #065f46; }
                    .agent-avatar.color3 { background: #fef3c7; color: #92400e; }
                    .agent-avatar.color4 { background: #f3e8ff; color: #6b21a8; }
                    .agent-info { flex: 1; }
                    .agent-name { font-weight: 800; font-size: 13px; color: #174a78; }
                    .agent-role { font-size: 11px; color: #64748b; margin-top: 2px; }
                    .agent-body { display: none; padding: 8px 12px 14px; border-top: 1px solid #e1e9f2; font-size: 12px; color: #475569; line-height: 1.6; }
                    .agent-card.expanded .agent-body { display: block; }
                    .agent-body h4 { margin: 8px 0 4px; font-size: 11px; color: #174a78; text-transform: uppercase; letter-spacing: 0.5px; }
                    .agent-body pre { font-size: 11px; background: #f8fafc; padding: 6px 8px; border-radius: 4px; margin: 4px 0; white-space: pre-wrap; }

                    /* === Skills === */
                    .skill-card {
                        border: 1px solid #e1e9f2;
                        border-radius: 8px;
                        padding: 10px 12px;
                        margin-bottom: 8px;
                        cursor: pointer;
                        background: #ffffff;
                    }
                    .skill-card:hover { border-color: #174a78; }
                    .skill-card.expanded { border-color: #174a78; background: #f8faff; }
                    .skill-name { font-weight: 800; font-size: 13px; color: #174a78; }
                    .skill-overview { font-size: 12px; color: #64748b; margin-top: 4px; }
                    .skill-body { display: none; margin-top: 8px; padding-top: 8px; border-top: 1px solid #e1e9f2; font-size: 12px; color: #475569; line-height: 1.6; max-height: 300px; overflow-y: auto; }
                    .skill-card.expanded .skill-body { display: block; }
                    .skill-body pre { font-size: 11px; background: #f8fafc; padding: 6px 8px; border-radius: 4px; margin: 4px 0; white-space: pre-wrap; }

                    /* === Evaluation Panel === */
                    .eval-case {
                        border: 1px solid #e1e9f2;
                        border-radius: 6px;
                        padding: 8px 10px;
                        margin-bottom: 6px;
                        background: #ffffff;
                    }
                    .eval-case .q { font-weight: 700; font-size: 12px; color: #174a78; }
                    .eval-case .meta { font-size: 11px; color: #64748b; margin-top: 2px; }
                    .eval-result {
                        border: 2px solid;
                        border-radius: 8px;
                        padding: 10px 12px;
                        margin: 8px 0;
                        font-size: 12px;
                        line-height: 1.6;
                    }
                    .eval-result.pass { border-color: #86efac; background: #f0fdf4; }
                    .eval-result.fail { border-color: #fca5a5; background: #fef2f2; }

                    /* === QA Review Button === */
                    .qa-review-bar {
                        margin: 8px auto 0;
                        max-width: 900px;
                        text-align: right;
                    }
                    .qa-review-bar button {
                        border: 1px solid #c9d6e4;
                        border-radius: 6px;
                        padding: 4px 10px;
                        font: inherit;
                        font-size: 11px;
                        font-weight: 700;
                        cursor: pointer;
                        background: #ffffff;
                        color: #475569;
                    }
                    .qa-review-bar button:hover { background: #eaf2fc; border-color: #174a78; color: #174a78; }
                    .qa-review-result {
                        margin: 4px auto 8px;
                        max-width: 900px;
                        background: #f8faff;
                        border: 1px solid #d8e1ec;
                        border-radius: 6px;
                        padding: 8px 12px;
                        font-size: 11px;
                        line-height: 1.6;
                        color: #334155;
                    }

                    /* === Source Expandable === */
                    .source-card {
                        display: inline-block;
                        border: 1px solid #d8e1ec;
                        border-radius: 6px;
                        padding: 3px 8px;
                        margin: 2px 1px;
                        font-size: 11px;
                        font-weight: 700;
                        color: #174a78;
                        cursor: pointer;
                        background: #f0f5fa;
                    }
                    .source-card:hover { background: #dce9f6; border-color: #174a78; }
                    .source-expanded {
                        display: none;
                        margin: 4px 0 8px 4px;
                        padding: 8px 10px;
                        background: #f8faff;
                        border: 1px solid #d8e1ec;
                        border-radius: 6px;
                        font-size: 12px;
                        line-height: 1.6;
                    }
                    .source-expanded.show { display: block; }
                    .source-expanded .src-title { font-weight: 700; color: #174a78; }
                    .source-expanded .src-meta { font-size: 11px; color: #94a3b8; }
                    .source-expanded .src-content { margin-top: 4px; color: #334155; font-size: 11px; }

                    /* === Context Preview === */
                    .context-overlay {
                        position: fixed; inset: 0;
                        background: rgba(0,0,0,0.4);
                        z-index: 1001;
                        display: none;
                        align-items: center;
                        justify-content: center;
                    }
                    .context-overlay.show { display: flex; }
                    .context-modal {
                        background: #ffffff;
                        border-radius: 12px;
                        width: 90vw;
                        max-width: 800px;
                        max-height: 80vh;
                        overflow-y: auto;
                        box-shadow: 0 8px 32px rgba(0,0,0,0.15);
                        padding: 20px 24px;
                    }
                    .context-modal h3 { margin: 0 0 12px; font-size: 16px; }
                    .context-modal .ctx-section {
                        margin-bottom: 16px;
                        border: 1px solid #e1e9f2;
                        border-radius: 6px;
                        overflow: hidden;
                    }
                    .context-modal .ctx-header {
                        padding: 6px 10px;
                        background: #f1f5f9;
                        font-weight: 700;
                        font-size: 12px;
                        color: #174a78;
                    }
                    .context-modal .ctx-body {
                        padding: 8px 10px;
                        font-size: 12px;
                        color: #334155;
                        white-space: pre-wrap;
                        max-height: 300px;
                        overflow-y: auto;
                    }
                    .context-modal .close-btn {
                        float: right;
                        border: 0;
                        background: transparent;
                        font-size: 20px;
                        cursor: pointer;
                        color: #94a3b8;
                    }
                    .context-modal .close-btn:hover { color: #dc2626; }

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
                  <div id="authOverlay" style="display:flex; position:fixed; inset:0; background:rgba(0,0,0,0.4); z-index:1000; align-items:center; justify-content:center;">
                    <div style="background:#fff; border-radius:12px; padding:32px; width:380px; max-width:90vw; box-shadow:0 8px 32px rgba(0,0,0,0.15);">
                      <h1 style="margin:0 0 20px; font-size:22px; text-align:center; color:#174a78;">Knowledge Agent Platform</h1>
                      <div id="loginForm">
                        <h2 style="margin:0 0 16px; font-size:16px; color:#334155;">Login</h2>
                        <input id="loginUsername" placeholder="Username" style="margin-bottom:8px;" autocomplete="username">
                        <input id="loginPassword" type="password" placeholder="Password" style="margin-bottom:12px;" autocomplete="current-password">
                        <button class="primary" onclick="login()" style="width:100%; margin-bottom:10px;">Login</button>
                        <p style="text-align:center; font-size:13px; color:#64748b; margin:0;">
                          No account? <a href="#" onclick="showRegister();return false;" style="color:#174a78;font-weight:700;">Register</a>
                        </p>
                      </div>
                      <div id="registerForm" style="display:none;">
                        <h2 style="margin:0 0 16px; font-size:16px; color:#334155;">Register</h2>
                        <input id="registerUsername" placeholder="Username" style="margin-bottom:8px;" autocomplete="username">
                        <input id="registerPassword" type="password" placeholder="Password (min 6 chars)" style="margin-bottom:12px;" autocomplete="new-password">
                        <button class="primary" onclick="register()" style="width:100%; margin-bottom:10px;">Register</button>
                        <p style="text-align:center; font-size:13px; color:#64748b; margin:0;">
                          Already have an account? <a href="#" onclick="showLogin();return false;" style="color:#174a78;font-weight:700;">Login</a>
                        </p>
                      </div>
                      <div id="authError" style="color:#dc2626; font-size:13px; margin-top:10px; text-align:center;"></div>
                    </div>
                  </div>
                  <!-- Context Preview Modal -->
                  <div id="contextPreview" class="context-overlay" onclick="if(event.target===this)closeContextPreview()">
                    <div class="context-modal">
                      <button class="close-btn" onclick="closeContextPreview()">&times;</button>
                      <h3>上下文预览</h3>
                      <div id="contextContent">
                        <p class="muted">暂无上下文数据。请先发送一条消息。</p>
                      </div>
                    </div>
                  </div>
                  <div class="app" style="display:none;">
                    <aside>
                      <div class="sessions" id="sessionsBlock">
                        <h2>对话历史</h2>
                        <div id="sessionList"><div class="session-empty">暂无对话记录</div></div>
                      </div>
                      <nav class="nav">
                        <button class="active" data-panel="knowledge" onclick="showPanel('knowledge')">知识库</button>
                        <button data-panel="upload" onclick="showPanel('upload')">上传文档</button>
                        <button data-panel="tools" onclick="showPanel('tools')">工具</button>
                        <button data-panel="run" onclick="showPanel('run')">Run</button>
                        <button data-panel="agents" onclick="showPanel('agents')">Agents</button>
                        <button data-panel="skills" onclick="showPanel('skills')">Skills</button>
                        <button data-panel="trace" onclick="showPanel('trace')">Trace</button>
                        <button data-panel="evaluation" onclick="showPanel('evaluation')">评测</button>
                        <button data-panel="links" onclick="showPanel('links')">接口</button>
                      </nav>
                      <div class="side-scroll">
                        <section id="panel-knowledge" class="panel active">
                          <h3>知识库管理</h3>
                          <div class="row">
                            <button class="secondary" onclick="loadDocs()">刷新列表</button>
                            <button class="secondary" onclick="fillSample()">填入示例</button>
                          </div>
                          <div id="docList" style="margin-top:8px;"></div>
                          <h4>添加文本片段</h4>
                          <label for="title">标题</label>
                          <input id="title" value="Agent Harness">
                          <label for="content">内容</label>
                          <textarea id="content">Harness separates model adapter, context builder, memory, tools, trace and evaluation so each step can be debugged independently.</textarea>
                          <label for="tags">标签（逗号分隔）</label>
                          <input id="tags" value="agent,harness">
                          <button class="primary" style="margin-top:8px;" onclick="addDoc()">添加到知识库</button>
                          <p id="docStatus" class="muted"></p>
                        </section>

                        <section id="panel-upload" class="panel">
                          <h3>上传文档</h3>
                          <label for="uploadTitle">文档标题</label>
                          <input id="uploadTitle" placeholder="默认使用文件名">
                          <label for="uploadTags">标签（逗号分隔）</label>
                          <input id="uploadTags" value="upload,knowledge">
                          <label for="file">选择文件</label>
                          <input id="file" type="file" accept=".docx,.pdf,.txt,.md">
                          <button class="primary" style="margin-top:8px;" onclick="uploadFile()">上传并解析</button>
                          <p class="muted" style="margin-top:6px;">支持 .docx / .pdf / .txt / .md，自动切分为知识片段</p>
                          <div id="uploadResult" class="doc muted">等待上传...</div>
                        </section>

                        <section id="panel-tools" class="panel">
                          <h3>工具调用</h3>
                          <label for="tool">选择工具</label>
                          <select id="tool" onchange="updateToolInput()">
                            <option value="calculator">calculator</option>
                            <option value="echo">echo</option>
                            <option value="http_mock">http_mock</option>
                          </select>
                          <label for="toolInput">输入</label>
                          <input id="toolInput" value="12 + 30">
                          <button class="primary" style="margin-top:8px;" onclick="invokeTool()">调用工具</button>
                          <h4>结果</h4>
                          <div id="toolResult" class="doc muted">等待调用...</div>
                        </section>

                        <section id="panel-run" class="panel">
                          <h3>执行时间线</h3>
                          <div class="row">
                            <button class="secondary" onclick="loadRunTimeline()">刷新</button>
                            <span class="muted" id="runSessionLabel">选择对话后查看</span>
                          </div>
                          <div id="runTimeline" class="run-empty">暂无执行记录。发送一条消息后会自动生成时间线。</div>
                        </section>

                        <section id="panel-agents" class="panel">
                          <h3>Agent 团队</h3>
                          <button class="secondary" onclick="loadAgents()">加载团队</button>
                          <div id="agentList" style="margin-top:8px;"></div>
                        </section>

                        <section id="panel-skills" class="panel">
                          <h3>Skills</h3>
                          <button class="secondary" onclick="loadSkills()">加载 Skills</button>
                          <div id="skillList" style="margin-top:8px;"></div>
                        </section>

                        <section id="panel-trace" class="panel">
                          <h3>Trace 追踪</h3>
                          <button class="secondary" onclick="loadTraces()">刷新 Trace</button>
                          <div id="traceList" style="margin-top:8px;"></div>
                        </section>

                        <section id="panel-evaluation" class="panel">
                          <h3>评测管理</h3>
                          <div class="row">
                            <button class="secondary" onclick="loadEvaluationCases()">刷新列表</button>
                            <button class="primary" style="font-size:11px;padding:4px 10px;" onclick="runAllEvaluations()">运行全部评测</button>
                          </div>
                          <div id="evalResults" style="margin-bottom:8px;"></div>
                          <h4>添加评测用例</h4>
                          <label for="evalQuestion">问题</label>
                          <input id="evalQuestion" placeholder="输入测试问题">
                          <label for="evalKeywords">期望关键词（逗号分隔）</label>
                          <input id="evalKeywords" placeholder="关键词1, 关键词2">
                          <label for="evalFeedback">期望回答参考</label>
                          <textarea id="evalFeedback" placeholder="可选的期望回答参考"></textarea>
                          <button class="primary" style="margin-top:8px;" onclick="addEvaluationCase()">添加用例</button>
                          <p id="evalStatus" class="muted"></p>
                          <div id="evalCaseList"></div>
                        </section>

                        <section id="panel-links" class="panel">
                          <h3>接口入口</h3>
                          <p class="links">
                            <a href="/swagger-ui/index.html" target="_blank">Swagger UI</a>
                            <a href="/api/documents" target="_blank">Documents</a>
                            <a href="/api/tools" target="_blank">Tools</a>
                            <a href="/api/traces" target="_blank">Trace</a>
                            <a href="/api/evaluations" target="_blank">Evaluation</a>
                          </p>
                        </section>
                      </div>
                    </aside>

                    <main class="chat">
                      <header class="chat-header">
                        <div>
                          <h3>知识问答</h3>
                          <div class="status" id="sessionStatus">未创建会话</div>
                        </div>
                        <div class="row" style="margin:0;">
                          <span class="muted" id="modelStatus" style="font-size:11px;"></span>
                          <button class="secondary" style="font-size:11px;padding:4px 8px;" onclick="showContextPreview()">查看上下文</button>
                          <button class="secondary" onclick="clearChat()">新对话</button>
                          <button class="secondary" onclick="logout()" style="margin-left:auto;">退出登录</button>
                        </div>
                      </header>

                      <section id="messages" class="messages">
                        <div class="message assistant">
                          <div class="avatar assistant">AI</div>
                          <div class="bubble">你好，可以先在左侧上传文档或粘贴文本，然后在这里提问。我会根据知识库检索结果生成回答，并显示来源片段。</div>
                        </div>
                      </section>

                      <footer class="composer">
                        <div class="composer-inner">
                          <textarea id="question" placeholder="输入问题，Enter 发送，Shift+Enter 换行"></textarea>
                          <button class="primary" onclick="ask()">发送</button>
                        </div>
                      </footer>
                    </main>
                  </div>

                  <script>
                    let currentSessionId = null;
                    let lastSources = [];
                    let lastAnswer = '';
                    let lastQuestion = '';
                    let token = localStorage.getItem('auth_token');
                    const $ = id => document.getElementById(id);
                    const escapeHtml = text => String(text ?? '').replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));

                    function showAuth() {
                      $('authOverlay').style.display = 'flex';
                      document.querySelector('.app').style.display = 'none';
                    }
                    function hideAuth() {
                      $('authOverlay').style.display = 'none';
                      document.querySelector('.app').style.display = 'grid';
                    }
                    function showRegister() {
                      $('loginForm').style.display = 'none';
                      $('registerForm').style.display = 'block';
                      $('authError').textContent = '';
                    }
                    function showLogin() {
                      $('loginForm').style.display = 'block';
                      $('registerForm').style.display = 'none';
                      $('authError').textContent = '';
                    }
                    async function login() {
                      var username = $('loginUsername').value;
                      var password = $('loginPassword').value;
                      if (!username || !password) { $('authError').textContent = 'Please fill in all fields'; return; }
                      var res = await fetch('/api/auth/login', {
                        method: 'POST',
                        headers: {'Content-Type':'application/json'},
                        body: JSON.stringify({username:username, password:password})
                      });
                      var data = await res.json();
                      if (data.success) {
                        token = data.data.token;
                        localStorage.setItem('auth_token', token);
                        hideAuth();
                        initApp();
                      } else {
                        $('authError').textContent = data.message;
                      }
                    }
                    async function register() {
                      var username = $('registerUsername').value;
                      var password = $('registerPassword').value;
                      if (!username || !password) { $('authError').textContent = 'Please fill in all fields'; return; }
                      if (password.length < 6) { $('authError').textContent = 'Password must be at least 6 characters'; return; }
                      var res = await fetch('/api/auth/register', {
                        method: 'POST',
                        headers: {'Content-Type':'application/json'},
                        body: JSON.stringify({username:username, password:password})
                      });
                      var data = await res.json();
                      if (data.success) {
                        token = data.data.token;
                        localStorage.setItem('auth_token', token);
                        hideAuth();
                        initApp();
                      } else {
                        $('authError').textContent = data.message;
                      }
                    }
                    function logout() {
                      localStorage.removeItem('auth_token');
                      token = null;
                      currentSessionId = null;
                      showAuth();
                      $('loginUsername').value = '';
                      $('loginPassword').value = '';
                    }
                    function initApp() {
                      loadDocs();
                      loadTraces();
                      loadSessions();
                      loadAgents();
                      loadSkills();
                      loadEvaluationCases();
                      checkModelStatus();
                    }

                    async function api(url, options) {
                      options = options || {};
                      options.headers = options.headers || {};
                      if (token) options.headers['Authorization'] = 'Bearer ' + token;
                      const res = await fetch(url, options);
                      if (res.status === 401 && url !== '/api/auth/login' && url !== '/api/auth/register') {
                        localStorage.removeItem('auth_token');
                        token = null;
                        showAuth();
                        return { success: false, message: 'unauthorized' };
                      }
                      const text = await res.text();
                      try { return JSON.parse(text); } catch (e) { return {success:false, message:text}; }
                    }

                    function showPanel(name) {
                      document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
                      document.querySelectorAll('.nav button').forEach(b => b.classList.remove('active'));
                      var panel = $('panel-' + name);
                      if (panel) panel.classList.add('active');
                      var btn = document.querySelector('[data-panel="' + name + '"]');
                      if (btn) btn.classList.add('active');
                      if (name === 'trace') loadTraces();
                      if (name === 'knowledge') loadDocs();
                      if (name === 'run') loadRunTimeline();
                      if (name === 'agents') loadAgents();
                      if (name === 'skills') loadSkills();
                      if (name === 'evaluation') loadEvaluationCases();
                    }

                    function renderBubble(el, text, isUser) {
                      if (isUser) {
                        el.textContent = text;
                      } else {
                        el.innerHTML = typeof marked !== 'undefined' ? marked.parse(text) : escapeHtml(text).split('\\n').join('<br>');
                      }
                    }
                    function addMessage(role, text, sources) {
                      var wrap = document.createElement('div');
                      wrap.className = 'message ' + role;
                      var isUser = role === 'user';
                      var avatarText = isUser ? '你' : 'AI';
                      wrap.innerHTML = '<div class="avatar ' + role + '">' + avatarText + '</div><div class="bubble"></div>';
                      var bubble = wrap.querySelector('.bubble');
                      renderBubble(bubble, text, isUser);
                      $('messages').appendChild(wrap);

                      // Add source cards for assistant messages
                      if (!isUser && sources && sources.length > 0) {
                        var srcBar = document.createElement('div');
                        srcBar.style.cssText = 'max-width:900px;margin:2px auto 0;text-align:right;';
                        sources.forEach(function(src, idx) {
                          var card = document.createElement('span');
                          card.className = 'source-card';
                          card.textContent = '[' + (idx + 1) + '] ' + (src.title || '来源');
                          card.onclick = function() { toggleSourceExpand(srcBar, idx, src); };
                          srcBar.appendChild(card);
                        });
                        wrap.parentNode.insertBefore(srcBar, wrap.nextSibling);
                        // Source expanded detail area
                        var expandArea = document.createElement('div');
                        expandArea.id = 'srcExpandArea_' + Date.now();
                        expandArea.style.cssText = 'max-width:900px;margin:0 auto;';
                        srcBar.parentNode.insertBefore(expandArea, srcBar.nextSibling);
                      }

                      // Add QA review button for assistant messages
                      if (!isUser) {
                        var qaBar = document.createElement('div');
                        qaBar.className = 'qa-review-bar';
                        qaBar.innerHTML = '<button onclick="performQaReview(this)">检查回答</button>';
                        wrap.parentNode.insertBefore(qaBar, wrap.nextSibling);
                      }

                      $('messages').scrollTop = $('messages').scrollHeight;
                      return bubble;
                    }

                    function toggleSourceExpand(bar, idx, src) {
                      var area = bar.nextElementSibling;
                      if (!area) return;
                      var existing = area.querySelector('.source-expanded');
                      if (existing) {
                        existing.classList.toggle('show');
                        return;
                      }
                      var div = document.createElement('div');
                      div.className = 'source-expanded show';
                      div.innerHTML = '<div class="src-title">' + escapeHtml(src.title || '未命名来源') + '</div>'
                        + '<div class="src-meta">Chunk ID: ' + (src.id || '-') + ' | 标签: ' + (src.tags ? src.tags.join(', ') : '-') + '</div>'
                        + '<div class="src-content">' + escapeHtml(src.content || '') + '</div>';
                      area.appendChild(div);
                    }

                    function timeAgo(ts) {
                      var d = new Date(ts);
                      var now = new Date();
                      var diff = now - d;
                      if (diff < 6e4) return '刚刚';
                      if (diff < 36e5) return Math.floor(diff / 6e4) + ' 分钟前';
                      if (diff < 864e5) return Math.floor(diff / 36e5) + ' 小时前';
                      return d.toLocaleDateString('zh-CN');
                    }

                    function firstUserQuestion(session) {
                      var msgs = session.messages || [];
                      for (var i = 0; i < msgs.length; i++) {
                        if (msgs[i].role === 'user') return msgs[i].content;
                      }
                      return '（空对话）';
                    }

                    async function loadSessions() {
                      var data = await api('/api/sessions');
                      var sessions = (data.data || []).slice(0, 3);
                      var list = $('sessionList');
                      if (!sessions.length) {
                        list.innerHTML = '<div class="session-empty">暂无对话记录</div>';
                        return;
                      }
                      list.innerHTML = sessions.map(function(s) {
                        var title = firstUserQuestion(s);
                        if (title.length > 18) title = title.substring(0, 18) + '...';
                        var activeClass = s.id === currentSessionId ? ' active' : '';
                        return '<div class="session-item' + activeClass + '" data-sid="' + s.id + '">'
                          + '<button class="sess-del" data-action="delete-session" data-sid="' + s.id + '">&times;</button>'
                          + '<div class="sess-title">' + escapeHtml(title) + '</div>'
                          + '<div class="sess-meta">' + (s.messages ? s.messages.length : 0) + ' 条消息 · ' + timeAgo(s.updatedAt) + '</div>'
                          + '</div>';
                      }).join('');
                    }

                    async function deleteSession(sessionId) {
                      if (!confirm('确定删除该对话记录？')) return;
                      await api('/api/sessions/' + encodeURIComponent(sessionId), { method: 'DELETE' });
                      if (currentSessionId === sessionId) {
                        currentSessionId = null;
                        $('messages').innerHTML = '<div class="message assistant"><div class="avatar assistant">AI</div><div class="bubble">对话已删除。开始新的对话吧。</div></div>';
                        $('sessionStatus').textContent = '未创建会话';
                      }
                      loadSessions();
                    }

                    async function deleteDocument(docId) {
                      if (!confirm('确定删除该知识片段？')) return;
                      await api('/api/documents/' + docId, { method: 'DELETE' });
                      loadDocs();
                    }

                    async function switchSession(sessionId) {
                      currentSessionId = sessionId;
                      lastSources = [];
                      lastAnswer = '';
                      lastQuestion = '';
                      $('sessionStatus').textContent = '会话：' + sessionId;
                      var data = await api('/api/sessions');
                      var sessions = data.data || [];
                      var session = sessions.find(function(s) { return s.id === sessionId; });
                      if (!session) return;
                      $('messages').innerHTML = '';
                      (session.messages || []).forEach(function(m) {
                        addMessage(m.role, m.content);
                      });
                      loadSessions();
                      loadRunTimeline(sessionId);
                    }

                    async function ask() {
                      var question = $('question').value.trim();
                      if (!question) return;
                      var btn = document.querySelector('.composer .primary');
                      $('question').value = '';
                      lastQuestion = question;
                      addMessage('user', question);
                      var srcPlaceholder = addMessage('assistant', '...', []);
                      var target = srcPlaceholder;
                      btn.disabled = true;
                      var currentSources = [];
                      try {
                        var streamHeaders = {'Content-Type':'application/json'};
                        if (token) streamHeaders['Authorization'] = 'Bearer ' + token;
                        var res = await fetch('/api/chat/stream', {
                          method: 'POST',
                          headers: streamHeaders,
                          body: JSON.stringify({ question: question, sessionId: currentSessionId })
                        });
                        if (!res.ok || !res.body) {
                          target.textContent = '请求失败：HTTP ' + res.status;
                          btn.disabled = false;
                          return;
                        }
                        target.innerHTML = '';
                        var streamedText = '';
                        var reader = res.body.getReader();
                        var decoder = new TextDecoder();
                        var buffer = '';
                        var eventType = '';
                        while (true) {
                          var chunk = await reader.read();
                          if (chunk.done) break;
                          buffer += decoder.decode(chunk.value, { stream: true });
                          var lines = buffer.split('\\n');
                          buffer = lines.pop();
                          for (var i = 0; i < lines.length; i++) {
                            var line = lines[i];
                            if (line.startsWith('event:')) {
                              eventType = line.substring(6).trim();
                            } else if (line.startsWith('data:')) {
                              var payload = line.substring(5).trim();
                              if (eventType === 'session') {
                                currentSessionId = payload;
                                $('sessionStatus').textContent = '会话：' + currentSessionId;
                              } else if (eventType === 'sources') {
                                // We track sources count; actual sources will be fetched from run API
                              } else if (eventType === 'delta') {
                                streamedText += payload;
                                target.innerHTML = typeof marked !== 'undefined' ? marked.parse(streamedText) : escapeHtml(streamedText).split('\\n').join('<br>');
                                $('messages').scrollTop = $('messages').scrollHeight;
                              } else if (eventType === 'done') {
                                // session completed
                              }
                            }
                          }
                        }
                        lastAnswer = streamedText;
                      } catch (err) {
                        target.textContent = '请求失败：' + err.message;
                      }
                      btn.disabled = false;
                      loadTraces();
                      loadSessions();
                      // Load sources and timeline
                      if (currentSessionId) {
                        loadRunTimeline(currentSessionId);
                      }
                    }

                    async function loadDocs() {
                      var data = await api('/api/documents');
                      var docs = data.data || [];
                      $('docList').innerHTML = docs.map(function(d) {
                        return '<div class="doc"><button class="doc-del" onclick="deleteDocument(' + d.id + ')" title="删除此片段">&times;</button>'
                          + '<div class="doc-title">' + escapeHtml(d.title) + '</div>'
                          + '<div class="muted">' + escapeHtml(d.content) + '</div>'
                          + '<div>' + (d.tags || []).map(function(t) { return '<span class="pill">' + escapeHtml(t) + '</span>'; }).join('') + '</div></div>';
                      }).join('');
                    }

                    async function addDoc() {
                      var tags = $('tags').value.split(/[,，]/).map(function(s) { return s.trim(); }).filter(Boolean);
                      var data = await api('/api/documents', {
                        method: 'POST',
                        headers: {'Content-Type':'application/json'},
                        body: JSON.stringify({ title: $('title').value, content: $('content').value, tags: tags })
                      });
                      $('docStatus').textContent = data.success ? '已添加知识片段。' : '添加失败：' + data.message;
                      loadDocs();
                    }

                    async function uploadFile() {
                      var file = $('file').files[0];
                      if (!file) { $('uploadResult').textContent = '请先选择文件。'; return; }
                      var form = new FormData();
                      form.append('file', file);
                      if ($('uploadTitle').value.trim()) form.append('title', $('uploadTitle').value.trim());
                      if ($('uploadTags').value.trim()) form.append('tags', $('uploadTags').value.trim());
                      var data = await api('/api/documents/upload', { method: 'POST', body: form });
                      if (data.success) {
                        $('uploadResult').innerHTML = '<strong>上传成功</strong><br>文件：' + escapeHtml(data.data.filename) + '<br>字符数：' + data.data.characterCount + '<br>知识片段：' + data.data.chunkCount;
                        loadDocs();
                      } else {
                        $('uploadResult').textContent = '上传失败：' + data.message;
                      }
                    }

                    async function invokeTool() {
                      var name = $('tool').value;
                      var input = $('toolInput').value;
                      var data = await api('/api/tools/' + name + '/invoke', {
                        method: 'POST',
                        headers: {'Content-Type':'application/json'},
                        body: JSON.stringify({ input: input })
                      });
                      $('toolResult').innerHTML = '<pre>' + escapeHtml(JSON.stringify(data, null, 2)) + '</pre>';
                      loadTraces();
                    }

                    async function loadTraces() {
                      var data = await api('/api/traces');
                      var traces = data.data || [];
                      $('traceList').innerHTML = traces.slice(0, 10).map(function(t) {
                        return '<div class="trace"><div class="doc-title">' + escapeHtml(t.stage) + '</div>'
                          + '<div>' + escapeHtml(t.message) + '</div>'
                          + '<div class="muted">' + escapeHtml(t.sessionId) + ' · ' + escapeHtml(t.createdAt) + '</div></div>';
                      }).join('') || '<p class="muted">暂无 Trace</p>';
                    }

                    function updateToolInput() {
                      var v = $('tool').value;
                      $('toolInput').value = v === 'calculator' ? '12 + 30' : v === 'http_mock' ? 'GET /api/projects' : 'hello agent';
                    }

                    function fillSample() {
                      showPanel('knowledge');
                      $('title').value = 'Tool Calling';
                      $('content').value = 'Tool calling uses registered tools with parameter schema, timeout, permission scope, execution trace and fallback handling.';
                      $('tags').value = 'tool,mcp,trace';
                      $('content').scrollIntoView({ behavior: 'smooth', block: 'center' });
                      $('content').focus();
                    }

                    function clearChat() {
                      currentSessionId = null;
                      lastSources = [];
                      lastAnswer = '';
                      lastQuestion = '';
                      $('messages').innerHTML = '<div class="message assistant"><div class="avatar assistant">AI</div><div class="bubble">新对话已开始。你可以继续提问，也可以先在左侧补充知识库。</div></div>';
                      $('sessionStatus').textContent = '未创建会话';
                      $('runTimeline').innerHTML = '<div class="run-empty">暂无执行记录。发送一条消息后会自动生成时间线。</div>';
                      loadSessions();
                    }

                    /* === Run Timeline === */
                    async function loadRunTimeline(sessionId) {
                      var sid = sessionId || currentSessionId;
                      if (!sid) {
                        $('runTimeline').innerHTML = '<div class="run-empty">请先选择一个对话。</div>';
                        return;
                      }
                      $('runSessionLabel').textContent = '会话: ' + sid;
                      var data = await api('/api/runs?sessionId=' + encodeURIComponent(sid));
                      var stages = data.data || [];
                      if (!stages.length) {
                        $('runTimeline').innerHTML = '<div class="run-empty">该对话暂无执行记录。</div>';
                        return;
                      }
                      $('runTimeline').innerHTML = stages.map(function(stage, idx) {
                        var icon = stage.stage === 'USER_INPUT' ? '&#9997;' : stage.stage === 'RETRIEVAL' ? '&#128269;' : stage.stage === 'CONTEXT_BUILD' ? '&#128221;' : stage.stage === 'ANSWER' ? '&#128240;' : stage.stage === 'QA_REVIEW' ? '&#9989;' : '&#9679;';
                        var statusClass = 'done';
                        var durText = stage.durationMs > 0 ? (stage.durationMs >= 1000 ? (stage.durationMs/1000).toFixed(1) + 's' : stage.durationMs + 'ms') : '';
                        return '<div class="run-stage" onclick="this.classList.toggle(' + "'expanded'" + ')">'
                          + '<div class="run-stage-header">'
                          + '<div class="run-stage-icon ' + statusClass + '">' + icon + '</div>'
                          + '<div class="run-stage-name">' + stage.stage + '</div>'
                          + '<span class="run-stage-arrow">&#9654;</span>'
                          + (durText ? '<span class="run-stage-dur">' + durText + '</span>' : '')
                          + '</div>'
                          + '<div class="run-stage-body">'
                          + '<div><strong>' + escapeHtml(stage.message || '') + '</strong></div>'
                          + '<div class="muted" style="margin-top:2px;">' + escapeHtml(stage.summary || '') + '</div>'
                          + (stage.attributes ? '<pre>' + escapeHtml(JSON.stringify(stage.attributes, null, 2)) + '</pre>' : '')
                          + '</div>'
                          + '</div>';
                      }).join('');
                    }

                    /* === Agent Team === */
                    async function loadAgents() {
                      var data = await api('/api/agents');
                      var agents = data.data || [];
                      var colors = ['color1','color2','color3','color4'];
                      $('agentList').innerHTML = agents.map(function(agent, idx) {
                        var initial = (agent.displayName || agent.name).charAt(0).toUpperCase();
                        return '<div class="agent-card" onclick="this.classList.toggle(' + "'expanded'" + ')">'
                          + '<div class="agent-header">'
                          + '<div class="agent-avatar ' + colors[idx % 4] + '">' + initial + '</div>'
                          + '<div class="agent-info">'
                          + '<div class="agent-name">' + escapeHtml(agent.displayName || agent.name) + '</div>'
                          + '<div class="agent-role">' + escapeHtml(agent.role || '') + '</div>'
                          + '</div>'
                          + '<span class="run-stage-arrow">&#9654;</span>'
                          + '</div>'
                          + '<div class="agent-body">'
                          + '<h4>职责</h4><pre>' + escapeHtml(agent.responsibilities || '无') + '</pre>'
                          + (agent.inputSummary ? '<h4>输入</h4><pre>' + escapeHtml(agent.inputSummary) + '</pre>' : '')
                          + (agent.outputSummary ? '<h4>输出</h4><pre>' + escapeHtml(agent.outputSummary) + '</pre>' : '')
                          + '</div>'
                          + '</div>';
                      }).join('') || '<p class="muted">未找到 Agent 定义。请确保 .claude/agents/ 目录存在。</p>';
                    }

                    /* === Skills === */
                    async function loadSkills() {
                      var data = await api('/api/skills');
                      var skills = data.data || [];
                      $('skillList').innerHTML = skills.map(function(skill) {
                        return '<div class="skill-card" onclick="this.classList.toggle(' + "'expanded'" + ')">'
                          + '<div class="skill-name">' + escapeHtml(skill.displayName || skill.name) + '</div>'
                          + (skill.overview ? '<div class="skill-overview">' + escapeHtml(skill.overview) + '</div>' : '')
                          + '<div class="skill-body"><pre>' + escapeHtml(skill.content || '') + '</pre></div>'
                          + '</div>';
                      }).join('') || '<p class="muted">未找到 Skill 定义。请确保 .claude/skills/ 目录存在。</p>';
                    }

                    /* === Evaluation === */
                    async function loadEvaluationCases() {
                      var data = await api('/api/evaluations');
                      var cases = data.data || [];
                      $('evalCaseList').innerHTML = cases.map(function(ec) {
                        var kw = (ec.expectedKeywords || []).join(', ');
                        return '<div class="eval-case">'
                          + '<div class="q">Q: ' + escapeHtml(ec.question) + '</div>'
                          + '<div class="meta">关键词: ' + escapeHtml(kw || '无') + ' | ' + timeAgo(ec.createdAt) + '</div>'
                          + '</div>';
                      }).join('') || '<p class="muted">暂无评测用例。添加一个开始评测。</p>';
                    }

                    async function addEvaluationCase() {
                      var question = $('evalQuestion').value.trim();
                      var keywords = $('evalKeywords').value.split(/[,，]/).map(function(s) { return s.trim(); }).filter(Boolean);
                      var feedback = $('evalFeedback').value.trim();
                      if (!question) { $('evalStatus').textContent = '请输入问题。'; return; }
                      var data = await api('/api/evaluations', {
                        method: 'POST',
                        headers: {'Content-Type':'application/json'},
                        body: JSON.stringify({ question: question, expectedKeywords: keywords, feedback: feedback })
                      });
                      $('evalStatus').textContent = data.success ? '✅ 评测用例已添加。' : '添加失败：' + data.message;
                      if (data.success) {
                        $('evalQuestion').value = '';
                        $('evalKeywords').value = '';
                        $('evalFeedback').value = '';
                        loadEvaluationCases();
                      }
                    }

                    async function runAllEvaluations() {
                      var casesData = await api('/api/evaluations');
                      var cases = casesData.data || [];
                      if (!cases.length) {
                        $('evalResults').innerHTML = '<div class="muted">暂无评测用例可运行。</div>';
                        return;
                      }
                      var answer = lastAnswer || '（无回答）';
                      var results = [];
                      for (var i = 0; i < cases.length; i++) {
                        var ec = cases[i];
                        var runData = await api('/api/evaluations/run', {
                          method: 'POST',
                          headers: {'Content-Type':'application/json'},
                          body: JSON.stringify({ caseId: ec.id, answer: answer })
                        });
                        if (runData.success) results.push(runData.data);
                      }
                      var passed = results.filter(function(r) { return r.score >= 0.6; }).length;
                      var total = results.length;
                      var avgScore = total > 0 ? results.reduce(function(s, r) { return s + r.score; }, 0) / total : 0;
                      var html = '<div class="eval-result ' + (avgScore >= 0.6 ? 'pass' : 'fail') + '">'
                        + '<strong>评测结果</strong> — 通过: ' + passed + '/' + total
                        + ' | 平均分: ' + (avgScore * 100).toFixed(0) + '/100'
                        + '</div>';
                      html += results.map(function(r) {
                        return '<div class="eval-case">'
                          + '<div class="q">Q: ' + escapeHtml(r.question) + '</div>'
                          + '<div class="meta">'
                          + (r.retrievalHit ? '✅命中' : '❌未命中') + ' | '
                          + (r.citationPresent ? '✅引用' : '❌无引用') + ' | '
                          + (r.keywordMatch ? '✅关键词' : '❌关键词') + ' | '
                          + '得分: ' + (r.score * 100).toFixed(0) + '/100'
                          + '</div>'
                          + '</div>';
                      }).join('');
                      $('evalResults').innerHTML = html;
                    }

                    /* === QA Review === */
                    async function performQaReview(btn) {
                      if (!lastAnswer && !$('messages').querySelector('.assistant:last-child .bubble')) {
                        btn.textContent = '暂无回答可检查';
                        return;
                      }
                      var answerText = lastAnswer || $('messages').querySelector('.assistant:last-child .bubble').textContent || '';
                      if (!answerText || answerText === '...') {
                        btn.textContent = '等待回答完成...';
                        return;
                      }
                      btn.disabled = true;
                      btn.textContent = '检查中...';
                      try {
                        var sourcesForReview = (lastSources || []).map(function(s) {
                          return { title: s.title || '', content: s.content || '', chunkId: String(s.id || ''), tags: Array.isArray(s.tags) ? s.tags.join(',') : '', score: 0 };
                        });
                        var reviewData = await api('/api/qa/review', {
                          method: 'POST',
                          headers: {'Content-Type':'application/json'},
                          body: JSON.stringify({
                            question: lastQuestion || '',
                            answer: answerText,
                            sources: sourcesForReview,
                            expectedKeywords: []
                          })
                        });
                        if (reviewData.success) {
                          var r = reviewData.data;
                          var resultDiv = document.createElement('div');
                          resultDiv.className = 'qa-review-result';
                          resultDiv.innerHTML = '<strong>回答质量检查</strong><br>' + (r.summary || '');
                          var parent = btn.parentNode;
                          var existing = parent.nextElementSibling;
                          if (existing && existing.className === 'qa-review-result') {
                            existing.replaceWith(resultDiv);
                          } else {
                            parent.parentNode.insertBefore(resultDiv, parent.nextSibling);
                          }
                          btn.textContent = '重新检查';
                        } else {
                          btn.textContent = '检查失败';
                        }
                      } catch (e) {
                        btn.textContent = '检查出错';
                      }
                      btn.disabled = false;
                    }

                    /* === Context Preview === */
                    function showContextPreview() {
                      if (!currentSessionId && !lastQuestion) {
                        alert('请先发送一条消息。');
                        return;
                      }
                      var q = lastQuestion || '（上次问题）';
                      var sourcesText = lastSources.length > 0
                        ? lastSources.map(function(s, i) { return '[' + (i+1) + '] ' + escapeHtml(s.title) + ': ' + escapeHtml(s.content); }).join('\\n\\n')
                        : '（无知识片段）';
                      var sysPromptHasSources = lastSources.length > 0;
                      var sysPrompt = sysPromptHasSources
                        ? '你是一个知识库问答助手。请结合用户问题和给定知识片段生成回答，优先依据知识库内容，不要只复述原文。回答默认使用中文，使用 Markdown 格式排版，关键结论后用 [1]、[2] 引用来源编号。'
                        : '你是一个简洁、友好的中文问答助手。当前没有检索到知识库片段时，可以先自然回应用户；如果用户提出专业问题，请说明尚未命中知识库，并给出通用建议。';
                      var finalMsg = '用户问题：\\n' + q;
                      if (lastSources.length > 0) {
                        finalMsg += '\\n\\n知识片段：\\n' + lastSources.map(function(s, i) { return '[' + (i+1) + '] ' + s.title + ': ' + s.content; }).join('\\n');
                      }
                      var html = ''
                        + '<div class="ctx-section"><div class="ctx-header">System Prompt</div><div class="ctx-body">' + escapeHtml(sysPrompt) + '</div></div>'
                        + '<div class="ctx-section"><div class="ctx-header">Recent History</div><div class="ctx-body">' + escapeHtml('用户: ' + q) + '</div></div>'
                        + '<div class="ctx-section"><div class="ctx-header">Retrieved Chunks (' + lastSources.length + ')</div><div class="ctx-body">' + sourcesText.replace(/\\n/g, '<br>') + '</div></div>'
                        + '<div class="ctx-section"><div class="ctx-header">Final User Message</div><div class="ctx-body">' + escapeHtml(finalMsg) + '</div></div>';
                      $('contextContent').innerHTML = html;
                      $('contextPreview').classList.add('show');
                    }

                    function closeContextPreview() {
                      $('contextPreview').classList.remove('show');
                    }

                    /* === Model Status === */
                    async function checkModelStatus() {
                      try {
                        var res = await fetch('/api/tools');
                        var modelEl = $('modelStatus');
                        modelEl.textContent = '🔵 DeepSeek';
                      } catch (e) {
                        $('modelStatus').textContent = '⚪ 离线模式';
                      }
                    }

                    if (token) {
                      hideAuth();
                      initApp();
                    } else {
                      showAuth();
                    }
                    $('sessionList').addEventListener('click', function(e) {
                      if (e.target.getAttribute('data-action') === 'delete-session') {
                        deleteSession(e.target.getAttribute('data-sid'));
                        return;
                      }
                      var item = e.target.closest('.session-item');
                      if (item) switchSession(item.getAttribute('data-sid'));
                    });
                    $('question').addEventListener('keydown', function(event) {
                      if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
                        event.preventDefault();
                        ask();
                      }
                    });
                    $('loginPassword').addEventListener('keydown', function(e) {
                      if (e.key === 'Enter') login();
                    });
                    $('registerPassword').addEventListener('keydown', function(e) {
                      if (e.key === 'Enter') register();
                    });
                  </script>
                </body>
                </html>
                """;
    }
}
