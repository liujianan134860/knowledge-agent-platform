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
import com.liujianan.agentdemo.tool.entity.NoteItem;
import com.liujianan.agentdemo.tool.service.NoteService;

@Service
public class NoteTool {
    private static final Logger log = LoggerFactory.getLogger(NoteTool.class);
    private final NoteService noteService;

    public NoteTool(NoteService noteService) {
        this.noteService = noteService;
    }

    @Tool(name = "note.create", description = "Create a personal note. Input JSON: {\"title\":\"...\", \"content\":\"...\", \"tags\":\"tag1,tag2\"}")
    public String create(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String title = (String) params.get("title");
            if (title == null || title.isBlank()) return "Error: title is required";
            String content = (String) params.getOrDefault("content", "");
            String tags = (String) params.getOrDefault("tags", "");
            String userId = getCurrentUserId();
            NoteItem note = noteService.create(userId, title, content, tags);
            return String.format("Note created: #%d - %s", note.getId(), note.getTitle());
        } catch (Exception e) {
            log.error("note.create failed", e);
            return "Error creating note: " + e.getMessage();
        }
    }

    @Tool(name = "note.search", description = "Search personal notes by keyword. Input JSON: {\"keyword\":\"...\"}")
    public String search(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String keyword = (String) params.get("keyword");
            if (keyword == null || keyword.isBlank()) return "Error: keyword is required";
            String userId = getCurrentUserId();
            List<NoteItem> notes = noteService.search(userId, keyword);
            if (notes.isEmpty()) return "No notes found for keyword: " + keyword;
            return notes.stream()
                    .map(n -> String.format("#%d %s (tags: %s)", n.getId(), n.getTitle(), n.getTags() != null ? n.getTags() : ""))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("note.search failed", e);
            return "Error searching notes: " + e.getMessage();
        }
    }

    @Tool(name = "note.list", description = "List personal notes. Input JSON: {\"days\":7} for notes from last N days, or {} for all notes")
    public String list(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String userId = getCurrentUserId();
            List<NoteItem> notes;
            Object daysObj = params.get("days");
            if (daysObj != null) {
                int days = daysObj instanceof Number n ? n.intValue() : Integer.parseInt(daysObj.toString());
                LocalDateTime since = LocalDateTime.now().minusDays(days);
                notes = noteService.listSince(userId, since);
            } else {
                notes = noteService.list(userId);
            }
            if (notes.isEmpty()) return "No notes found.";
            return notes.stream()
                    .map(n -> String.format("#%d %s [%s] (tags: %s)", n.getId(), n.getTitle(),
                            n.getCreatedAt() != null ? n.getCreatedAt().toLocalDate().toString() : "?",
                            n.getTags() != null ? n.getTags() : ""))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("note.list failed", e);
            return "Error listing notes: " + e.getMessage();
        }
    }

    private String getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String p) return p;
        return "anonymous";
    }
}
