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
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; margin: 48px; color: #1f2937; line-height: 1.6; }
                    h1 { color: #1f4e79; margin-bottom: 8px; }
                    .card { max-width: 760px; border: 1px solid #dbe3ea; border-radius: 10px; padding: 28px; background: #f8fafc; }
                    a { color: #1f4e79; font-weight: 600; text-decoration: none; }
                    code { background: #eef2f7; padding: 2px 6px; border-radius: 4px; }
                    ul { padding-left: 20px; }
                  </style>
                </head>
                <body>
                  <main class="card">
                    <h1>Knowledge Agent Platform</h1>
                    <p>基于 Agent Harness 思路的知识库问答与工具调用平台。</p>
                    <ul>
                      <li><a href="/swagger-ui/index.html">Swagger UI</a></li>
                      <li><a href="/api/documents">Documents API</a></li>
                      <li><a href="/api/tools">Tools API</a></li>
                      <li><a href="/api/traces">Trace API</a></li>
                      <li><a href="/api/evaluations">Evaluation API</a></li>
                    </ul>
                    <p>示例问答接口：<code>POST /api/chat</code></p>
                  </main>
                </body>
                </html>
                """;
    }
}
