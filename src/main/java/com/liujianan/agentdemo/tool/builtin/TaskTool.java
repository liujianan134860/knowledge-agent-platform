package com.liujianan.agentdemo.tool.builtin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import com.liujianan.agentdemo.tool.ToolInputParser;
import com.liujianan.agentdemo.tool.entity.TaskItem;
import com.liujianan.agentdemo.tool.service.TaskService;

@Service
public class TaskTool {
    private static final Logger log = LoggerFactory.getLogger(TaskTool.class);
    private final TaskService taskService;

    public TaskTool(TaskService taskService) {
        this.taskService = taskService;
    }

    @Tool(name = "task.create", description = "Create a new task. Input JSON: {\"title\":\"...\", \"description\":\"...\", \"priority\":\"HIGH|MEDIUM|LOW\", \"dueDate\":\"2026-05-20T18:00:00\"}")
    public String create(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String title = (String) params.get("title");
            if (title == null || title.isBlank()) return "Error: title is required";
            String description = (String) params.getOrDefault("description", "");
            String priority = (String) params.getOrDefault("priority", "MEDIUM");
            String dueDateStr = (String) params.get("dueDate");
            LocalDateTime dueDate = dueDateStr != null ? LocalDateTime.parse(dueDateStr) : null;
            String userId = getCurrentUserId();
            TaskItem task = taskService.create(userId, title, description, priority, dueDate);
            return String.format("Task created: #%d - %s (priority: %s, status: %s)", task.getId(), task.getTitle(), task.getPriority(), task.getStatus());
        } catch (Exception e) {
            log.error("task.create failed", e);
            return "Error creating task: " + e.getMessage();
        }
    }

    @Tool(name = "task.list", description = "List tasks. Input JSON: {\"status\":\"PENDING|COMPLETED\"} (optional, omit for all)")
    public String list(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String status = (String) params.get("status");
            String userId = getCurrentUserId();
            List<TaskItem> tasks = taskService.list(userId, status);
            if (tasks.isEmpty()) return "No tasks found.";
            return tasks.stream()
                    .map(t -> String.format("#%d [%s] %s (priority: %s)%s",
                            t.getId(), t.getStatus(), t.getTitle(), t.getPriority(),
                            t.getDueDate() != null ? " due: " + t.getDueDate() : ""))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("task.list failed", e);
            return "Error listing tasks: " + e.getMessage();
        }
    }

    @Tool(name = "task.complete", description = "Mark a task as completed. Input JSON: {\"id\":1}")
    public String complete(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            Object idObj = params.get("id");
            if (idObj == null) return "Error: id is required";
            Long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            String userId = getCurrentUserId();
            TaskItem task = taskService.complete(userId, id);
            return String.format("Task #%d completed: %s", task.getId(), task.getTitle());
        } catch (Exception e) {
            log.error("task.complete failed", e);
            return "Error completing task: " + e.getMessage();
        }
    }

    private String getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String p) return p;
        return "anonymous";
    }
}
