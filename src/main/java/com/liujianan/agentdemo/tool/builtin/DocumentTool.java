package com.liujianan.agentdemo.tool.builtin;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.liujianan.agentdemo.tool.ToolInputParser;
import com.liujianan.agentdemo.tool.service.DocumentService;

@Service
public class DocumentTool {
    private static final Logger log = LoggerFactory.getLogger(DocumentTool.class);
    private final DocumentService documentService;

    public DocumentTool(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Tool(name = "document.extract_text",
            description = "Extract the full text of an uploaded document by documentId. " +
                    "Returns the complete document content with metadata. " +
                    "Input JSON: {\"documentId\":\"...\"}")
    public String extractText(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String documentId = (String) params.get("documentId");
            if (documentId == null || documentId.isBlank()) {
                return "Error: documentId is required";
            }
            String userId = getCurrentUserId();
            DocumentService.DocumentResult doc = documentService.extractText(documentId, userId);
            return String.format("""
                    Title: %s
                    Source: %s
                    Pages: %d
                    Chars: %d
                    Chunks: %d
                    --- Content Start ---
                    %s
                    --- Content End ---""",
                    doc.title(), doc.sourceType(), doc.pageCount(),
                    doc.charCount(), doc.chunkCount(), doc.fullText());
        } catch (Exception e) {
            log.error("document.extract_text failed", e);
            return "Error extracting document text: " + e.getMessage();
        }
    }

    @Tool(name = "document.summarize",
            description = "Summarize a document or text. Provide documentId to summarize an uploaded document, " +
                    "OR text to summarize arbitrary text directly. " +
                    "Input JSON: {\"documentId\":\"...\"} OR {\"text\":\"...\"}")
    public String summarize(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String documentId = (String) params.get("documentId");
            String text = (String) params.get("text");
            String userId = getCurrentUserId();

            String contentToSummarize;
            if (documentId != null && !documentId.isBlank()) {
                DocumentService.DocumentResult doc = documentService.extractText(documentId, userId);
                contentToSummarize = doc.fullText();
            } else if (text != null && !text.isBlank()) {
                contentToSummarize = text;
            } else {
                return "Error: provide either 'documentId' or 'text'";
            }

            String summary = documentService.summarize(contentToSummarize, userId);
            return "Summary:\n" + summary;
        } catch (Exception e) {
            log.error("document.summarize failed", e);
            return "Error summarizing: " + e.getMessage();
        }
    }

    private String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String p) return p;
        return "anonymous";
    }
}
