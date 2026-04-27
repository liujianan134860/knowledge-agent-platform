package com.liujianan.agentdemo.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

@Service
public class LlmClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
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

    private String buildRequest(String question, List<DocumentChunk> sources) throws Exception {
        String context = buildContext(sources);
        String system = sources.isEmpty()
                ? "你是一个简洁、友好的中文问答助手。当前没有检索到知识库片段时，可以先自然回应用户；如果用户提出专业问题，请说明尚未命中知识库，并给出通用建议。"
                : "你是一个知识库问答助手。请优先基于给定知识片段回答问题；回答要清晰、简洁，并在适当位置引用来源编号，例如 [1]。";
        String user = sources.isEmpty()
                ? "用户问题：\n" + question
                : "用户问题：\n" + question + "\n\n知识片段：\n" + context;
        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)
                )
        );
        return objectMapper.writeValueAsString(payload);
    }

    private String buildContext(List<DocumentChunk> sources) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            DocumentChunk source = sources.get(i);
            context.append("[").append(i + 1).append("] ")
                    .append(source.title()).append(": ")
                    .append(source.content()).append("\n");
        }
        return context.toString();
    }

    private String fallback(String question, List<DocumentChunk> sources) {
        if (sources.isEmpty()) {
            if (isGreeting(question)) {
                return "你好，我是知识库问答助手。你可以在左侧上传 Word/PDF 文档或粘贴文本，然后向我提问；我会基于知识库内容回答并显示来源片段。";
            }
            return "知识库中没有检索到与“" + question + "”相关的片段。请先在左侧添加相关知识片段，或换一种更具体的问法。";
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

    private boolean isGreeting(String question) {
        String normalized = question == null ? "" : question.trim().toLowerCase();
        return normalized.matches("^(你好|您好|hello|hi|hey|嗨|在吗|在不在)[！!。.]?$");
    }

    private String extractContent(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        return content.isTextual() ? content.asText() : "";
    }
}
