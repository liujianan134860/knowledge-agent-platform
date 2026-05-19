package com.liujianan.agentdemo.tool.builtin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import com.liujianan.agentdemo.tool.ToolInputParser;
import com.liujianan.agentdemo.tool.entity.PersonalMemory;
import com.liujianan.agentdemo.tool.service.MemoryService;

@Service
public class MemoryTool {
    private static final Logger log = LoggerFactory.getLogger(MemoryTool.class);
    private final MemoryService memoryService;

    public MemoryTool(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Tool(name = "memory.remember", description = "Store a personal memory (upsert by key). Input JSON: {\"key\":\"...\", \"value\":\"...\", \"category\":\"preference|fact|context\"}")
    public String remember(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String key = (String) params.get("key");
            if (key == null || key.isBlank()) return "Error: key is required";
            String value = (String) params.getOrDefault("value", "");
            String category = (String) params.getOrDefault("category", "");
            String userId = getCurrentUserId();
            PersonalMemory mem = memoryService.remember(userId, key, value, category);
            return String.format("Memory stored: %s = %s (category: %s)", mem.getKey(),
                    mem.getValue().length() > 100 ? mem.getValue().substring(0, 100) + "..." : mem.getValue(),
                    mem.getCategory() != null ? mem.getCategory() : "none");
        } catch (Exception e) {
            log.error("memory.remember failed", e);
            return "Error storing memory: " + e.getMessage();
        }
    }

    @Tool(name = "memory.recall", description = "Recall a personal memory by key or category. Input JSON: {\"key\":\"...\"} OR {\"category\":\"...\"}")
    public String recall(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String key = (String) params.get("key");
            String category = (String) params.get("category");
            String userId = getCurrentUserId();

            if (key != null && !key.isBlank()) {
                Optional<PersonalMemory> mem = memoryService.recall(userId, key);
                return mem.map(m -> "Memory: " + m.getKey() + " = " + m.getValue())
                        .orElse("No memory found for key: " + key);
            }
            if (category != null && !category.isBlank()) {
                List<PersonalMemory> mems = memoryService.recallByCategory(userId, category);
                if (mems.isEmpty()) return "No memories found for category: " + category;
                return mems.stream()
                        .map(m -> m.getKey() + " = " + m.getValue())
                        .collect(Collectors.joining("\n"));
            }
            return "Error: provide either 'key' or 'category'";
        } catch (Exception e) {
            log.error("memory.recall failed", e);
            return "Error recalling memory: " + e.getMessage();
        }
    }

    private String getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String p) return p;
        return "anonymous";
    }
}
