package com.liujianan.agentdemo.mcp;

import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.knowledge.KnowledgeService;
import com.liujianan.agentdemo.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Tool Service exposing Spring AI @Tool-annotated methods.
 * These tools are discoverable by MCP clients (e.g., Claude Code).
 */
@Service
public class McpToolService {
    private static final Logger log = LoggerFactory.getLogger(McpToolService.class);

    private final ToolRegistry toolRegistry;
    private final KnowledgeService knowledgeService;

    public McpToolService(ToolRegistry toolRegistry, KnowledgeService knowledgeService) {
        this.toolRegistry = toolRegistry;
        this.knowledgeService = knowledgeService;
    }

    /**
     * Calculate a simple arithmetic expression.
     * Supported operators: +, -, *, /
     * Input format: "12 + 30"
     */
    @Tool(description = "Calculate simple arithmetic expression. Input format: '12 + 30'")
    public String calculator(String input) {
        log.debug("MCP tool: calculator({})", input);
        try {
            return toolRegistry.invoke("calculator", input).output();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Echo the input text back (for connectivity testing).
     */
    @Tool(description = "Return the input text as-is for connectivity testing")
    public String echo(String input) {
        log.debug("MCP tool: echo({})", input);
        return toolRegistry.invoke("echo", input).output();
    }

    /**
     * Search the user's knowledge base for relevant chunks.
     */
    @Tool(description = "Search the knowledge base for relevant information. Returns up to 5 matching chunks.")
    public String searchKnowledge(String query, String userId) {
        log.debug("MCP tool: searchKnowledge(query={}, userId={})", query, userId);
        try {
            List<DocumentChunk> results = knowledgeService.search(query, 5, userId);
            if (results.isEmpty()) {
                return "No relevant knowledge found for: " + query;
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
     * Mock an HTTP API call for testing.
     */
    @Tool(description = "Mock an HTTP API call and return a deterministic response. Input format: 'GET /api/projects'")
    public String httpMock(String input) {
        log.debug("MCP tool: httpMock({})", input);
        return toolRegistry.invoke("http_mock", input).output();
    }

    /**
     * Get status information about the platform.
     */
    @Tool(description = "Get the current platform status including available tools and knowledge count")
    public String platformStatus(String userId) {
        try {
            List<DocumentChunk> docs = knowledgeService.list(userId);
            List<String> toolNames = toolRegistry.listTools().stream()
                    .map(t -> t.name())
                    .toList();
            return "Platform Status:\n"
                    + "- Knowledge chunks: " + docs.size() + "\n"
                    + "- Available tools: " + String.join(", ", toolNames);
        } catch (Exception e) {
            return "Status check failed: " + e.getMessage();
        }
    }
}
