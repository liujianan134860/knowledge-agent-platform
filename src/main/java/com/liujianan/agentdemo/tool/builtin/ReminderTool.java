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
import com.liujianan.agentdemo.tool.entity.ReminderItem;
import com.liujianan.agentdemo.tool.service.ReminderService;

@Service
public class ReminderTool {
    private static final Logger log = LoggerFactory.getLogger(ReminderTool.class);
    private final ReminderService reminderService;

    public ReminderTool(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Tool(name = "reminder.create", description = "Create a reminder. Input JSON: {\"title\":\"...\", \"remindAt\":\"2026-05-20T09:00:00\", \"note\":\"...\"}")
    public String create(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String title = (String) params.get("title");
            if (title == null || title.isBlank()) return "Error: title is required";
            String remindAtStr = (String) params.get("remindAt");
            if (remindAtStr == null) return "Error: remindAt is required (ISO format)";
            LocalDateTime remindAt = LocalDateTime.parse(remindAtStr);
            String note = (String) params.getOrDefault("note", "");
            String userId = getCurrentUserId();
            ReminderItem reminder = reminderService.create(userId, title, remindAt, note);
            return String.format("Reminder created: #%d - %s at %s", reminder.getId(), reminder.getTitle(), reminder.getRemindAt());
        } catch (Exception e) {
            log.error("reminder.create failed", e);
            return "Error creating reminder: " + e.getMessage();
        }
    }

    @Tool(name = "reminder.list", description = "List reminders. Input JSON: {\"status\":\"PENDING\"} (optional)")
    public String list(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String status = (String) params.getOrDefault("status", "PENDING");
            String userId = getCurrentUserId();
            List<ReminderItem> reminders = reminderService.list(userId, status);
            if (reminders.isEmpty()) return "No reminders found.";
            return reminders.stream()
                    .map(r -> String.format("#%d [%s] %s at %s", r.getId(), r.getStatus(), r.getTitle(), r.getRemindAt()))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("reminder.list failed", e);
            return "Error listing reminders: " + e.getMessage();
        }
    }

    private String getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String p) return p;
        return "anonymous";
    }
}
