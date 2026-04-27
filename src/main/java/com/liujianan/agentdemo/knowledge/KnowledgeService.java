package com.liujianan.agentdemo.knowledge;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KnowledgeService {
    private final AtomicLong idGenerator = new AtomicLong(2);
    private final List<DocumentChunk> chunks = new CopyOnWriteArrayList<>();

    public KnowledgeService() {
        LocalDateTime now = LocalDateTime.now();
        chunks.add(new DocumentChunk(1L, "Agent Harness", "Agent Harness separates model adapter, context builder, memory, tools, trace and evaluation so each step can be debugged independently.", List.of("agent", "harness"), now));
        chunks.add(new DocumentChunk(2L, "RAG Flow", "RAG retrieves source chunks, compresses context, builds a prompt, returns answer with citations, and records retrieval hit rate.", List.of("rag", "retrieval"), now));
        chunks.add(new DocumentChunk(3L, "Tool Calling", "Tool calling uses registered tools with parameter schema, timeout, permission scope, execution trace and fallback handling.", List.of("tool", "mcp"), now));
    }

    public List<DocumentChunk> list() {
        return new ArrayList<>(chunks);
    }

    public DocumentChunk add(AddDocumentRequest request) {
        DocumentChunk chunk = new DocumentChunk(
                idGenerator.incrementAndGet(),
                request.title(),
                request.content(),
                request.tags() == null ? List.of() : request.tags(),
                LocalDateTime.now()
        );
        chunks.add(chunk);
        return chunk;
    }

    public List<DocumentChunk> search(String query, int topK) {
        if (topK <= 0 || topK > 10) {
            throw new IllegalArgumentException("topK must be between 1 and 10");
        }
        String normalizedQuery = normalize(query);
        return chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, score(chunk, normalizedQuery)))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingInt(ScoredChunk::score).reversed())
                .limit(topK)
                .map(ScoredChunk::chunk)
                .toList();
    }

    private int score(DocumentChunk chunk, String normalizedQuery) {
        String text = normalize(chunk.title() + " " + chunk.content() + " " + String.join(" ", chunk.tags()));
        int score = 0;
        for (String token : normalizedQuery.split("\\s+")) {
            if (!token.isBlank() && text.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private record ScoredChunk(DocumentChunk chunk, int score) {
    }
}
