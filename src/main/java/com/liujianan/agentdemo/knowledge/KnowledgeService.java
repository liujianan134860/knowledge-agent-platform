package com.liujianan.agentdemo.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KnowledgeService {
    private final AtomicLong idGenerator = new AtomicLong(3);
    private final List<DocumentChunk> chunks = new CopyOnWriteArrayList<>();
    private final DocumentTextExtractor documentTextExtractor;

    public KnowledgeService(DocumentTextExtractor documentTextExtractor) {
        this.documentTextExtractor = documentTextExtractor;
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

    public DocumentUploadResponse upload(MultipartFile file, String title, List<String> tags) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file must not be empty");
        }
        String text = normalizeWhitespace(documentTextExtractor.extract(file));
        if (text.isBlank()) {
            throw new IllegalArgumentException("no text extracted from uploaded file");
        }
        String baseTitle = title == null || title.isBlank() ? cleanFilename(file.getOriginalFilename()) : title.trim();
        List<String> normalizedTags = tags == null ? List.of("upload") : tags.stream().filter(tag -> tag != null && !tag.isBlank()).map(String::trim).toList();
        List<String> parts = splitText(text, 900);
        List<DocumentChunk> created = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            DocumentChunk chunk = new DocumentChunk(
                    idGenerator.incrementAndGet(),
                    parts.size() == 1 ? baseTitle : baseTitle + " #" + (i + 1),
                    parts.get(i),
                    normalizedTags,
                    LocalDateTime.now()
            );
            chunks.add(chunk);
            created.add(chunk);
        }
        return new DocumentUploadResponse(file.getOriginalFilename(), file.getContentType(), text.length(), created.size(), created);
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

    private String normalizeWhitespace(String text) {
        return text == null ? "" : text.replace("\r", "\n").replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private List<String> splitText(String text, int maxChars) {
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\\n+");
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (current.length() + trimmed.length() + 1 > maxChars && current.length() > 0) {
                result.add(current.toString().trim());
                current.setLength(0);
            }
            if (trimmed.length() > maxChars) {
                for (int start = 0; start < trimmed.length(); start += maxChars) {
                    result.add(trimmed.substring(start, Math.min(start + maxChars, trimmed.length())));
                }
            } else {
                current.append(trimmed).append("\n");
            }
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result.isEmpty() ? List.of(text) : result;
    }

    private String cleanFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "Uploaded Document";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private record ScoredChunk(DocumentChunk chunk, int score) {
    }
}
