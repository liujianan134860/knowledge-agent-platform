package com.liujianan.agentdemo.knowledge;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class AliyunBailianRerankClient implements RerankClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int timeoutSeconds;

    public AliyunBailianRerankClient(ObjectMapper objectMapper,
                                     @Value("${rerank.bailian.api-key:}") String apiKey,
                                     @Value("${rerank.bailian.base-url:https://dashscope.aliyuncs.com/compatible-api/v1}") String baseUrl,
                                     @Value("${rerank.bailian.model:qwen3-rerank}") String model,
                                     @Value("${rerank.bailian.timeout-seconds:30}") int timeoutSeconds) {
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
    public List<RerankResult> rerank(String query, List<DocumentChunk> candidates, int topK) {
        if (!isConfigured()) {
            throw new IllegalStateException("Bailian rerank api key is not configured");
        }
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        try {
            List<String> documents = candidates.stream()
                    .map(chunk -> (chunk.title() == null ? "" : chunk.title()) + "\n" + chunk.content())
                    .toList();
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "query", query,
                    "documents", documents,
                    "top_n", Math.min(topK, candidates.size()),
                    "return_documents", false,
                    "instruct", "Given a user question, rank the documents by usefulness for answering the question."
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/reranks"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Bailian rerank call failed: HTTP " + response.statusCode());
            }
            return parseResponse(response.body(), candidates, topK);
        } catch (Exception e) {
            throw new IllegalStateException("Bailian rerank call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String modelName() {
        return model;
    }

    private List<RerankResult> parseResponse(String body, List<DocumentChunk> candidates, int topK) throws Exception {
        JsonNode results = objectMapper.readTree(body).path("results");
        List<RerankResult> reranked = new ArrayList<>();
        if (results.isArray()) {
            for (JsonNode item : results) {
                int index = item.path("index").asInt(-1);
                if (index >= 0 && index < candidates.size()) {
                    double score = item.path("relevance_score").asDouble(item.path("score").asDouble(0.0));
                    reranked.add(new RerankResult(candidates.get(index), score, model));
                }
            }
        }
        return reranked.stream()
                .sorted(Comparator.comparingDouble(RerankResult::score).reversed())
                .limit(topK)
                .toList();
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://dashscope.aliyuncs.com/compatible-api/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
