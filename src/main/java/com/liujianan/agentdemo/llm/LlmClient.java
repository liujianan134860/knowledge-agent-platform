package com.liujianan.agentdemo.llm;

import com.liujianan.agentdemo.knowledge.DocumentChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LlmClient {
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public LlmClient(@Value("${deepseek.api-key:}") String apiKey,
                     @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
                     @Value("${deepseek.model:deepseek-chat}") String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public String answer(String question, List<DocumentChunk> sources) {
        if (!isConfigured()) {
            return fallback(question, sources);
        }
        try {
            String body = buildRequest(question, sources);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String content = extractContent(response.body());
                if (!content.isBlank()) {
                    return content;
                }
            }
            return fallback(question, sources) + "\n\n[LLM call failed: HTTP " + response.statusCode() + "]";
        } catch (Exception exception) {
            return fallback(question, sources) + "\n\n[LLM call failed: " + exception.getMessage() + "]";
        }
    }

    private String buildRequest(String question, List<DocumentChunk> sources) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            DocumentChunk source = sources.get(i);
            context.append("[").append(i + 1).append("] ")
                    .append(source.title()).append(": ")
                    .append(source.content()).append("\n");
        }
        String system = """
                你是一个知识库问答助手。请只基于给定知识片段回答问题。
                如果知识片段不足以回答，请说明缺少哪些信息。
                回答要清晰、简洁，并在适当位置引用来源编号，例如 [1]。
                """;
        String user = "问题：\n" + question + "\n\n知识片段：\n" + (context.isEmpty() ? "无" : context);
        return """
                {
                  "model": "%s",
                  "temperature": 0.2,
                  "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                  ]
                }
                """.formatted(escape(model), escape(system), escape(user));
    }

    private String fallback(String question, List<DocumentChunk> sources) {
        if (sources.isEmpty()) {
            return "知识库中没有检索到与“" + question + "”相关的片段。请先添加相关知识片段，或换一种问法。";
        }
        StringBuilder answer = new StringBuilder("已检索到 ").append(sources.size()).append(" 个相关知识片段：\n");
        for (int i = 0; i < sources.size(); i++) {
            DocumentChunk source = sources.get(i);
            answer.append("\n[").append(i + 1).append("] ")
                    .append(source.title()).append("：")
                    .append(source.content());
        }
        answer.append("\n\n当前未配置 DeepSeek API Key，因此返回检索摘要。配置后将生成自然语言回答。");
        return answer.toString();
    }

    private String extractContent(String json) {
        Matcher matcher = CONTENT_PATTERN.matcher(json);
        if (!matcher.find()) {
            return "";
        }
        return unescape(matcher.group(1));
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescape(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                switch (next) {
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    default -> result.append(next);
                }
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }
}
