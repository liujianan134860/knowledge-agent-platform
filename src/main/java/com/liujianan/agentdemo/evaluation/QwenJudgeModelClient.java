package com.liujianan.agentdemo.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class QwenJudgeModelClient implements JudgeModelClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int timeoutSeconds;

    public QwenJudgeModelClient(ObjectMapper objectMapper,
                                @Value("${judge.qwen.api-key:}") String apiKey,
                                @Value("${judge.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
                                @Value("${judge.qwen.model:qwen3.6-plus}") String model,
                                @Value("${judge.qwen.timeout-seconds:30}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.model = model;
        this.timeoutSeconds = Math.max(5, timeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(10, this.timeoutSeconds)))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public String judge(String prompt) {
        if (!isConfigured()) {
            throw new IllegalStateException("Qwen judge api key is not configured");
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.0,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a strict LLM-as-judge evaluator. Return only score and reason."),
                            Map.of("role", "user", "content", prompt)
                    )
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Qwen judge call failed: HTTP " + response.statusCode());
            }
            JsonNode content = objectMapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new IllegalStateException("Qwen judge returned empty content");
            }
            return content.asText();
        } catch (Exception e) {
            throw new IllegalStateException("Qwen judge call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String modelName() {
        return model;
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
