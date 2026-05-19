package com.liujianan.agentdemo.tool.builtin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.knowledge.KnowledgeService;
import com.liujianan.agentdemo.tool.ToolInputParser;

@Service
public class KnowledgeSearchTool {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchTool.class);
    private final KnowledgeService knowledgeService;

    public KnowledgeSearchTool(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Tool(name = "knowledge.search", description = "Search the user's knowledge base. Input JSON: {\"query\":\"...\", \"topK\":3}")
    public String search(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String query = (String) params.get("query");
            if (query == null || query.isBlank()) return "Error: query is required";
            int topK = params.get("topK") instanceof Number n ? n.intValue() : 3;
            String userId = getCurrentUserId();
            List<DocumentChunk> results = knowledgeService.search(query, Math.min(topK, 10), userId);
            if (results.isEmpty()) return "No knowledge found for query: " + query;
            return results.stream()
                    .map(c -> String.format("[%s] %s", c.getTitle(),
                            c.getContent().length() > 200 ? c.getContent().substring(0, 200) + "..." : c.getContent()))
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.error("knowledge.search failed", e);
            return "Error searching knowledge: " + e.getMessage();
        }
    }

    private String getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String p) return p;
        return "anonymous";
    }
}
