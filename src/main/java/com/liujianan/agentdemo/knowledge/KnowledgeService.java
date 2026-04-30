package com.liujianan.agentdemo.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class KnowledgeService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final DocumentChunkRepository documentRepository;
    private final DocumentTextExtractor documentTextExtractor;
    private final Optional<VectorKnowledgeService> vectorKnowledgeService;

    public KnowledgeService(DocumentChunkRepository documentRepository,
                            DocumentTextExtractor documentTextExtractor,
                            Optional<VectorKnowledgeService> vectorKnowledgeService) {
        this.documentRepository = documentRepository;
        this.documentTextExtractor = documentTextExtractor;
        this.vectorKnowledgeService = vectorKnowledgeService;
    }

    public List<DocumentChunk> list(String userId) {
        return documentRepository.findByUserId(userId);
    }

    @Transactional
    public DocumentChunk add(AddDocumentRequest request, String userId) {
        DocumentChunk chunk = new DocumentChunk(
                null, request.title(), request.content(),
                request.tags() == null ? List.of() : request.tags(),
                LocalDateTime.now(), userId
        );
        DocumentChunk saved = documentRepository.save(chunk);
        // Index in vector store if available
        vectorKnowledgeService.ifPresent(vks -> {
            try {
                vks.index(saved);
            } catch (Exception e) {
                log.warn("Failed to index chunk {} in vector store: {}", saved.getId(), e.getMessage());
            }
        });
        return saved;
    }

    @Transactional
    public DocumentUploadResponse upload(MultipartFile file, String title, List<String> tags, String userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file must not be empty");
        }
        String text = normalizeWhitespace(documentTextExtractor.extract(file));
        if (text.isBlank()) {
            throw new IllegalArgumentException("no text extracted from uploaded file");
        }
        String baseTitle = title == null || title.isBlank() ? cleanFilename(file.getOriginalFilename()) : title.trim();
        List<String> normalizedTags = tags == null ? List.of("upload") : tags.stream().filter(tag -> tag != null && !tag.isBlank()).map(String::trim).toList();
        List<String> parts = splitText(text, 1500);
        List<DocumentChunk> created = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            DocumentChunk chunk = new DocumentChunk(
                    null,
                    parts.size() == 1 ? baseTitle : baseTitle + " #" + (i + 1),
                    parts.get(i), normalizedTags, LocalDateTime.now(), userId
            );
            DocumentChunk saved = documentRepository.save(chunk);
            // Index in vector store if available
            vectorKnowledgeService.ifPresent(vks -> {
                try {
                    vks.index(saved);
                } catch (Exception e) {
                    log.warn("Failed to index chunk {} in vector store: {}", saved.getId(), e.getMessage());
                }
            });
            created.add(saved);
        }
        return new DocumentUploadResponse(file.getOriginalFilename(), file.getContentType(), text.length(), created.size(), created);
    }

    @Transactional
    public boolean delete(Long documentId, String userId) {
        if (documentId == null) return false;
        DocumentChunk chunk = documentRepository.findById(documentId).orElse(null);
        if (chunk == null || !chunk.getUserId().equals(userId)) return false;
        documentRepository.delete(chunk);
        // Remove from vector store if available
        vectorKnowledgeService.ifPresent(vks -> {
            try {
                vks.deleteByChunkId(documentId);
            } catch (Exception e) {
                log.warn("Failed to delete chunk {} from vector store: {}", documentId, e.getMessage());
            }
        });
        return true;
    }

    public List<DocumentChunk> search(String query, int topK, String userId) {
        if (topK <= 0 || topK > 10) {
            throw new IllegalArgumentException("topK must be between 1 and 10");
        }
        // Try vector search first if available
        if (vectorKnowledgeService.isPresent()) {
            try {
                List<org.springframework.ai.document.Document> vectorResults =
                        vectorKnowledgeService.get().search(query, topK, userId);
                if (!vectorResults.isEmpty()) {
                    log.debug("Vector search returned {} results for query '{}'", vectorResults.size(), query);
                    // Map back to DocumentChunks
                    List<Long> chunkIds = vectorResults.stream()
                            .map(doc -> {
                                try {
                                    return Long.valueOf(doc.getMetadata().getOrDefault("chunkId", "0").toString());
                                } catch (NumberFormatException e) {
                                    return 0L;
                                }
                            })
                            .filter(id -> id > 0)
                            .toList();
                    if (!chunkIds.isEmpty()) {
                        List<DocumentChunk> chunks = documentRepository.findAllById(chunkIds);
                        if (!chunks.isEmpty()) {
                            return chunks;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Vector search failed, falling back to keyword search: {}", e.getMessage());
            }
        }

        // Fallback to keyword search
        return keywordSearch(query, topK, userId);
    }

    private List<DocumentChunk> keywordSearch(String query, int topK, String userId) {
        String normalizedQuery = normalize(query);
        List<DocumentChunk> userChunks = documentRepository.findByUserId(userId);
        return userChunks.stream()
                .map(chunk -> new ScoredChunk(chunk, score(chunk, normalizedQuery)))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingInt(ScoredChunk::score).reversed())
                .limit(topK)
                .map(ScoredChunk::chunk)
                .toList();
    }

    private int score(DocumentChunk chunk, String normalizedQuery) {
        String text = normalize(chunk.getTitle() + " " + chunk.getContent() + " " + String.join(" ", chunk.getTags()));
        int score = 0;

        String[] tokens = normalizedQuery.split("\\s+");
        if (tokens.length > 1) {
            for (String token : tokens) {
                if (!token.isBlank() && text.contains(token)) {
                    score += 2;
                }
            }
        }

        if (normalizedQuery.length() >= 2) {
            for (int i = 0; i < normalizedQuery.length() - 1; i++) {
                String bigram = normalizedQuery.substring(i, i + 2);
                if (text.contains(bigram)) {
                    score++;
                }
            }
        }

        if (score == 0 && text.contains(normalizedQuery)) {
            score = 1;
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
        String[] sections = text.split("\\n(?=#{1,6}\\s)");
        if (sections.length > 1) {
            List<String> result = new ArrayList<>();
            for (String section : sections) {
                String trimmed = section.trim();
                if (!trimmed.isBlank()) {
                    result.addAll(splitByParagraphs(trimmed, maxChars));
                }
            }
            return result.isEmpty() ? List.of(text) : result;
        }
        return splitByParagraphs(text, maxChars);
    }

    private List<String> splitByParagraphs(String text, int maxChars) {
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\n+");
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isBlank()) continue;
            if (current.length() + trimmed.length() + 2 > maxChars && current.length() > 0) {
                result.add(current.toString().trim());
                current.setLength(0);
            }
            if (trimmed.length() > maxChars) {
                result.addAll(splitBySentences(trimmed, maxChars));
            } else {
                if (current.length() > 0) current.append("\n\n");
                current.append(trimmed);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private List<String> splitBySentences(String text, int maxChars) {
        List<String> result = new ArrayList<>();
        String[] sentences = text.split("(?<=[。！？.!?])\\s*");
        if (sentences.length <= 1) {
            return splitFixedWithOverlap(text, maxChars);
        }
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isBlank()) continue;
            if (current.length() + trimmed.length() > maxChars && current.length() > 0) {
                result.add(current.toString().trim());
                current.setLength(0);
            }
            if (trimmed.length() > maxChars) {
                if (current.length() > 0) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                }
                result.addAll(splitFixedWithOverlap(trimmed, maxChars));
            } else {
                current.append(trimmed);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private List<String> splitFixedWithOverlap(String text, int maxChars) {
        List<String> result = new ArrayList<>();
        int overlap = Math.max(80, maxChars / 10);
        int step = maxChars - overlap;
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(start + maxChars, text.length());
            if (end < text.length()) {
                int boundary = findWordBoundary(text, end, maxChars / 5);
                if (boundary > start + maxChars / 2) {
                    end = boundary;
                }
            }
            result.add(text.substring(start, end));
            if (end == text.length()) break;
        }
        return result;
    }

    private int findWordBoundary(String text, int target, int lookRange) {
        int searchEnd = Math.min(text.length(), target + lookRange);
        for (int i = target; i < searchEnd; i++) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?' || c == '\n') {
                return i + 1;
            }
        }
        for (int i = target; i < searchEnd; i++) {
            if (text.charAt(i) == ' ' || text.charAt(i) == '，' || text.charAt(i) == ',') {
                return i + 1;
            }
        }
        return target;
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
