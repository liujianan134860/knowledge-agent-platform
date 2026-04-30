package com.liujianan.agentdemo.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liujianan.agentdemo.harness.SessionMessage;
import com.liujianan.agentdemo.knowledge.DocumentChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class LlmClient implements ModelClient {
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

    public String answer(String question, List<DocumentChunk> sources, List<SessionMessage> history) {
        if (!isConfigured()) {
            return fallback(question, sources);
        }
        try {
            String body = buildRequest(question, sources, history);
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

    public void answerStream(String question, List<DocumentChunk> sources, List<SessionMessage> history,
                             Consumer<String> onDelta,
                             Consumer<String> onDone,
                             Consumer<String> onError) {
        if (!isConfigured()) {
            String text = fallback(question, sources);
            for (int i = 0; i < text.length(); i += 3) {
                int end = Math.min(i + 3, text.length());
                onDelta.accept(text.substring(i, end));
                try { Thread.sleep(30); } catch (InterruptedException ignored) {}
            }
            onDone.accept(text);
            return;
        }
        try {
            String body = buildStreamRequest(question, sources, history);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<java.io.InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                StringBuilder full = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) break;
                            try {
                                JsonNode node = objectMapper.readTree(data);
                                JsonNode content = node.path("choices").path(0).path("delta").path("content");
                                if (content.isTextual() && !content.asText().isEmpty()) {
                                    String token = content.asText();
                                    full.append(token);
                                    onDelta.accept(token);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
                onDone.accept(full.toString());
            } else {
                onError.accept("HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            onError.accept(e.getMessage());
        }
    }

    private String buildStreamRequest(String question, List<DocumentChunk> sources, List<SessionMessage> history) throws Exception {
        String body = buildRequest(question, sources, history);
        JsonNode root = objectMapper.readTree(body);
        Map<String, Object> map = objectMapper.convertValue(root, Map.class);
        map.put("stream", true);
        return objectMapper.writeValueAsString(map);
    }

    private String buildRequest(String question, List<DocumentChunk> sources, List<SessionMessage> history) throws Exception {
        String context = buildContext(sources);
        String system = sources.isEmpty()
                ? "你是一个简洁、友好的中文问答助手。当前没有检索到知识库片段时，可以先自然回应用户；如果用户提出专业问题，请说明尚未命中知识库，并给出通用建议。回答使用 Markdown 格式，适当使用标题、列表、**加粗**等排版，使内容层次分明、易于阅读。"
                : "你是一个知识库问答助手。请结合用户问题和给定知识片段生成回答，优先依据知识库内容，不要只复述原文。回答默认使用中文，使用 Markdown 格式排版：\n- 用 ### 标题分段\n- 用 - 或 1. 创建列表\n- 用 **加粗** 突出重点\n- 用 > 引用原文\n- 关键结论后用 [1]、[2] 引用来源编号\n- 如有代码示例用 `` ` `` 包裹\n如果知识片段不足以完整回答，请明确说明缺少的信息。";
        String user = sources.isEmpty()
                ? "用户问题：\n" + question
                : "用户问题：\n" + question + "\n\n知识片段：\n" + context;

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));

        // Include recent conversation history (up to last 10 messages, excluding current question)
        if (history != null && history.size() > 1) {
            int end = history.size() - 1; // exclude current question
            int start = Math.max(0, end - 10);
            for (int i = start; i < end; i++) {
                SessionMessage msg = history.get(i);
                messages.add(Map.of("role", msg.role(), "content", msg.content()));
            }
        }
        messages.add(Map.of("role", "user", "content", user));

        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", messages
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
        if (question == null || question.isBlank()) return false;
        String normalized = question.trim().toLowerCase();
        return normalized.startsWith("你好")
                || normalized.startsWith("您好")
                || normalized.startsWith("hello")
                || normalized.startsWith("hi")
                || normalized.startsWith("hey")
                || normalized.startsWith("嗨")
                || normalized.equals("在吗")
                || normalized.equals("在不在");
    }

    private String extractContent(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        return content.isTextual() ? content.asText() : "";
    }
}
