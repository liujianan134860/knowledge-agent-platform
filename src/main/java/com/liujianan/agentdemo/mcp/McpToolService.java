package com.liujianan.agentdemo.mcp;

import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.knowledge.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Tool Service exposing Spring AI @Tool-annotated methods.
 * These tools are discoverable by MCP clients (e.g., Claude Code) and
 * usable as tool callbacks by the internal ChatClient.
 */
@Service
public class McpToolService {
    private static final Logger log = LoggerFactory.getLogger(McpToolService.class);

    private final KnowledgeService knowledgeService;

    public McpToolService(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    /**
     * Resolve the current user ID from the Spring Security context.
     * Returns "anonymous" if no authentication is available.
     */
    private String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String userId) {
            return userId;
        }
        return "anonymous";
    }

    /**
     * Search the user's knowledge base for relevant chunks.
     */
    @Tool(description = "Search the user's private knowledge base for relevant document chunks. Use this when the user asks about their uploaded documents or specific knowledge they have stored.")
    public String searchKnowledge(String query) {
        String userId = getCurrentUserId();
        log.info("Tool called: searchKnowledge(query={}, userId={})", query, userId);
        try {
            List<DocumentChunk> results = knowledgeService.search(query, 5, userId);
            if (results.isEmpty()) {
                return "No relevant knowledge found in your knowledge base for: " + query;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(results.size()).append(" relevant chunks:\n\n");
            for (int i = 0; i < results.size(); i++) {
                DocumentChunk chunk = results.get(i);
                sb.append("[").append(i + 1).append("] ")
                        .append(chunk.getTitle()).append("\n")
                        .append(chunk.getContent()).append("\n\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("Knowledge search failed", e);
            return "Search failed: " + e.getMessage();
        }
    }

    /**
     * Get status information about the platform.
     */
    @Tool(description = "Get the current platform status including knowledge base document count for the current user.")
    public String platformStatus() {
        String userId = getCurrentUserId();
        log.info("Tool called: platformStatus(userId={})", userId);
        try {
            List<DocumentChunk> docs = knowledgeService.list(userId);
            return "Platform Status:\n"
                    + "- Knowledge chunks: " + docs.size()
                    + "\n- User ID: " + userId;
        } catch (Exception e) {
            return "Status check failed: " + e.getMessage();
        }
    }
}
