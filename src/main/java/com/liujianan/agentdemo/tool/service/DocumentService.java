package com.liujianan.agentdemo.tool.service;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.knowledge.DocumentChunkRepository;

@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final int MAX_SUMMARIZE_CHARS = 30000;

    private final DocumentChunkRepository documentChunkRepository;
    private final ObjectProvider<ChatModel> chatModelProvider;

    public DocumentService(DocumentChunkRepository documentChunkRepository,
                           ObjectProvider<ChatModel> chatModelProvider) {
        this.documentChunkRepository = documentChunkRepository;
        this.chatModelProvider = chatModelProvider;
    }

    public DocumentResult extractText(String documentId, String userId) {
        List<DocumentChunk> chunks = documentChunkRepository
                .findByUserIdAndDocumentIdOrderByChunkIndexAsc(userId, documentId);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }
        DocumentChunk first = chunks.get(0);
        StringBuilder sb = new StringBuilder();
        for (DocumentChunk chunk : chunks) {
            sb.append(chunk.getContent());
        }
        int pageCount = (int) chunks.stream()
                .map(DocumentChunk::getPageNumber)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        if (pageCount == 0) pageCount = 1;
        return new DocumentResult(
                first.getTitle(),
                first.getSourceType(),
                pageCount,
                sb.length(),
                sb.toString(),
                chunks.size()
        );
    }

    public String summarize(String text, String userId) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text is empty — nothing to summarize");
        }
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new IllegalStateException("ChatModel not available for summarization");
        }
        String truncated = text.length() > MAX_SUMMARIZE_CHARS
                ? text.substring(0, MAX_SUMMARIZE_CHARS) + "..."
                : text;
        String prompt = """
                You are a document summarization assistant. Summarize the following document in Chinese.
                Include these sections:
                - 文档概要 (1-2 sentences overview)
                - 核心要点 (3-6 key points as bullet list)
                - 关键细节 (important facts, figures, dates)
                - 总结 (one paragraph conclusion)

                Document:
                %s
                """.formatted(truncated);
        ChatResponse response = chatModel.call(new Prompt(new SystemMessage(prompt)));
        String result = response.getResult().getOutput().getText();
        return result != null ? result.trim() : "No summary generated.";
    }

    public record DocumentResult(String title, String sourceType, int pageCount, int charCount,
                                  String fullText, int chunkCount) {
    }
}
