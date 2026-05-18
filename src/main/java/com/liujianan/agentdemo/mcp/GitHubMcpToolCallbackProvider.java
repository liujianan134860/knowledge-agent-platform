package com.liujianan.agentdemo.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "github.mcp.http.enabled", havingValue = "true")
public class GitHubMcpToolCallbackProvider implements ToolCallbackProvider {

    private static final Logger log = LoggerFactory.getLogger(GitHubMcpToolCallbackProvider.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final HttpClient httpClient;
    private final String mcpUrl;
    private final String token;
    private final String toolsets;
    private final String readonly;
    private final List<ToolCallback> toolCallbacks;

    public GitHubMcpToolCallbackProvider(
            @Value("${GITHUB_MCP_URL:https://api.githubcopilot.com}") String mcpUrl,
            @Value("${GITHUB_MCP_ENDPOINT:/mcp/}") String mcpEndpoint,
            @Value("${GITHUB_MCP_TOKEN:}") String token,
            @Value("${GITHUB_MCP_TOOLSETS:repos,issues,pull_requests}") String toolsets,
            @Value("${GITHUB_MCP_READONLY:1}") String readonly) {
        this.mcpUrl = mcpUrl + mcpEndpoint;
        this.token = token;
        this.toolsets = toolsets;
        this.readonly = readonly;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.toolCallbacks = buildToolCallbacks();
        log.info("GitHub MCP HTTP provider: {} tool(s) from {}", toolCallbacks.size(), this.mcpUrl);
    }

    private List<ToolCallback> buildToolCallbacks() {
        if (token == null || token.isBlank()) { log.warn("GitHub MCP token not set"); return List.of(); }
        try {
            rpcCall("initialize", Map.of("protocolVersion", "2025-06-18",
                "capabilities", Map.of(), "clientInfo", Map.of("name", "kdp", "version", "1.0.0")));
            JsonNode toolsNode = mapper.readTree(rpcCall("tools/list", Map.of()))
                    .path("result").path("tools");
            if (!toolsNode.isArray() || toolsNode.isEmpty()) { log.warn("No tools from GitHub MCP"); return List.of(); }
            List<ToolCallback> cbs = new ArrayList<>();
            for (JsonNode t : toolsNode) {
                cbs.add(new GhTool(t.path("name").asText(),
                        t.path("description").asText("GitHub: " + t.path("name").asText()),
                        t.path("inputSchema").toString()));
            }
            return cbs;
        } catch (Exception e) { log.error("GitHub MCP discovery failed: {}", e.getMessage()); return List.of(); }
    }

    @Override public ToolCallback[] getToolCallbacks() { return toolCallbacks.toArray(ToolCallback[]::new); }

    String rpcCall(String method, Map<String, Object> params) throws Exception {
        Map<String, Object> rpcReq = new LinkedHashMap<>();
        rpcReq.put("jsonrpc", "2.0");
        rpcReq.put("id", 1);
        rpcReq.put("method", method);
        rpcReq.put("params", params);
        String body = mapper.writeValueAsString(rpcReq);
        var rb = HttpRequest.newBuilder().uri(URI.create(mcpUrl)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .header("Authorization", "Bearer " + token.trim())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (toolsets != null && !toolsets.isBlank()) rb.header("X-MCP-Toolsets", toolsets.trim());
        if (readonly != null && !readonly.isBlank()) rb.header("X-MCP-Readonly", readonly.trim());
        HttpResponse<String> resp = httpClient.send(rb.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            log.error("GitHub MCP rpcCall failed: {} {} body={}", method, resp.statusCode(), resp.body());
            throw new IllegalStateException("HTTP " + resp.statusCode() + ": " + resp.body());
        }
        return extractJsonFromSse(resp.body());
    }

    /**
     * GitHub MCP returns SSE (text/event-stream) responses even for POST.
     * Extract the JSON-RPC response from "data:" lines.
     */
    private String extractJsonFromSse(String body) {
        if (body == null || body.isBlank()) return body;
        // If it already starts with '{', it's pure JSON
        if (body.trim().startsWith("{")) return body;
        // Parse SSE: "data: {...}" or "event: ...\ndata: {...}"
        for (String line : body.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("data:")) {
                String data = trimmed.substring(5).trim();
                if (!data.isEmpty()) return data;
            }
        }
        return body;
    }

    private class GhTool implements ToolCallback {
        private final ToolDefinition def;
        GhTool(String n, String d, String s) { def = DefaultToolDefinition.builder().name(n).description(d).inputSchema(s).build(); }
        @Override public ToolDefinition getToolDefinition() { return def; }
        @Override public String call(String input) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> args = mapper.readValue(input, Map.class);
                String r = rpcCall("tools/call", Map.of("name", def.name(), "arguments", args));
                return mapper.readTree(r).path("result").path("content").toString();
            } catch (Exception e) { return "{\"error\":\"" + e.getMessage() + "\"}"; }
        }
    }
}