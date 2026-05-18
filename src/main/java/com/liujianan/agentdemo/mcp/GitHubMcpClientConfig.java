package com.liujianan.agentdemo.mcp;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitHubMcpClientConfig {

    @Bean
    public McpSyncHttpClientRequestCustomizer githubMcpAuthorizationCustomizer(
            @Value("${github.mcp.token:}") String githubMcpToken,
            @Value("${github.mcp.toolsets:}") String githubMcpToolsets,
            @Value("${github.mcp.readonly:}") String githubMcpReadonly) {
        return (requestBuilder, method, uri, body, context) -> {
            if (uri == null || uri.getHost() == null || !uri.getHost().equalsIgnoreCase("api.githubcopilot.com")) {
                return;
            }
            addHeaderIfPresent(requestBuilder, "Authorization", bearer(githubMcpToken));
            addHeaderIfPresent(requestBuilder, "X-MCP-Toolsets", githubMcpToolsets);
            addHeaderIfPresent(requestBuilder, "X-MCP-Readonly", githubMcpReadonly);
        };
    }

    private void addHeaderIfPresent(java.net.http.HttpRequest.Builder requestBuilder, String name, String value) {
        if (value != null && !value.isBlank()) {
            requestBuilder.header(name, value.trim());
        }
    }

    private String bearer(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String trimmed = token.trim();
        return trimmed.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length()) ? trimmed : "Bearer " + trimmed;
    }
}
