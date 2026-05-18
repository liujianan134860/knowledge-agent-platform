package com.liujianan.agentdemo.knowledge;

import com.liujianan.agentdemo.common.AiPlatformProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KnowledgeService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]|[a-zA-Z0-9]{2,}");

    private final DocumentChunkRepository documentRepository;
    private final DocumentTextExtractor documentTextExtractor;
    private final Optional<VectorKnowledgeService> vectorKnowledgeService;
    private final Optional<RerankClient> rerankClient;
    private final AiPlatformProperties aiPlatformProperties;

    public KnowledgeService(DocumentChunkRepository documentRepository,
                            DocumentTextExtractor documentTextExtractor,
                            Optional<VectorKnowledgeService> vectorKnowledgeService,
                            Optional<RerankClient> rerankClient,
                            AiPlatformProperties aiPlatformProperties) {
        this.documentRepository = documentRepository;
        this.documentTextExtractor = documentTextExtractor;
        this.vectorKnowledgeService = vectorKnowledgeService;
        this.rerankClient = rerankClient;
        this.aiPlatformProperties = aiPlatformProperties;
    }

    public List<DocumentChunk> list(String userId) {
        return documentRepository.findByUserId(userId);
    }

    public Page<DocumentChunk> list(String userId, Pageable pageable) {
        return documentRepository.findByUserId(userId, pageable);
    }

    @Transactional
    public DocumentChunk add(AddDocumentRequest request, String userId) {
        DocumentChunk chunk = new DocumentChunk(
                null, request.title(), request.content(),
                request.tags() == null ? List.of() : request.tags(),
                LocalDateTime.now(), userId,
                "manual-" + UUID.randomUUID(), 0, null, 0,
                request.content() == null ? 0 : request.content().length(), "manual", "manual"
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
        DocumentTextExtractor.ExtractedDocument extracted = documentTextExtractor.extractWithMetadata(file);
        String text = normalizeWhitespace(extracted.text());
        if (text.isBlank()) {
            throw new IllegalArgumentException("no text extracted from uploaded file");
        }
        String baseTitle = title == null || title.isBlank() ? cleanFilename(file.getOriginalFilename()) : title.trim();
        String sourceName = cleanFilename(file.getOriginalFilename());
        String documentId = "doc-" + UUID.randomUUID();
        List<String> normalizedTags = tags == null ? List.of("upload") : tags.stream().filter(tag -> tag != null && !tag.isBlank()).map(String::trim).toList();
        List<ChunkCandidate> parts = splitTextWithOffsets(text, 1500);
        List<DocumentChunk> created = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            ChunkCandidate part = parts.get(i);
            DocumentChunk chunk = new DocumentChunk(
                    null,
                    parts.size() == 1 ? baseTitle : baseTitle + " #" + (i + 1),
                    part.content(), normalizedTags, LocalDateTime.now(), userId,
                    documentId, i, extracted.pageForOffset(part.startOffset()),
                    part.startOffset(), part.endOffset(), extracted.sourceType(), sourceName
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
        int candidateLimit = candidateLimit(topK);
        List<DocumentChunk> keywordResults = keywordSearch(query, candidateLimit, userId);

        // Hybrid search: prefer vector recall when available, then fill with keyword results.
        List<DocumentChunk> mergedCandidates = keywordResults;
        if (vectorKnowledgeService.isPresent()) {
            try {
                List<org.springframework.ai.document.Document> vectorResults =
                        vectorKnowledgeService.get().search(query, candidateLimit, userId);
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
                            Map<Long, DocumentChunk> merged = new LinkedHashMap<>();
                            chunks.stream()
                                    .filter(chunk -> userId.equals(chunk.getUserId()))
                                    .forEach(chunk -> merged.put(chunk.getId(), chunk));
                            keywordResults.forEach(chunk -> merged.putIfAbsent(chunk.getId(), chunk));
                            mergedCandidates = merged.values().stream().limit(candidateLimit).toList();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Vector search failed, falling back to keyword search: {}", e.getMessage());
            }
        }

        return rerank(query, mergedCandidates, topK);
    }

    private int candidateLimit(int topK) {
        int multiplier = Math.max(1, aiPlatformProperties.getRerankCandidateMultiplier());
        return Math.max(topK, Math.min(20, topK * multiplier));
    }

    private List<DocumentChunk> rerank(String query, List<DocumentChunk> candidates, int topK) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        if (aiPlatformProperties.isRerankEnabled() && rerankClient.isPresent() && rerankClient.get().isConfigured()
                && candidates.size() > 1) {
            try {
                List<RerankResult> reranked = rerankClient.get().rerank(query, candidates, topK);
                if (!reranked.isEmpty()) {
                    log.debug("Reranked {} candidates with {}", candidates.size(), rerankClient.get().modelName());
                    return reranked.stream().map(RerankResult::chunk).toList();
                }
            } catch (Exception e) {
                log.warn("Rerank failed, using hybrid retrieval order: {}", e.getMessage());
            }
        }
        return candidates.stream().limit(topK).toList();
    }

    private List<DocumentChunk> keywordSearch(String query, int topK, String userId) {
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        List<DocumentChunk> userChunks = documentRepository.findByUserId(userId);
        Map<Long, List<String>> documentTokens = new HashMap<>();
        Map<String, Integer> documentFrequency = new HashMap<>();
        int totalTokenCount = 0;

        for (DocumentChunk chunk : userChunks) {
            List<String> tokens = tokenize(chunk.getTitle() + " " + chunk.getContent() + " " + String.join(" ", chunk.getTags()));
            documentTokens.put(chunk.getId(), tokens);
            totalTokenCount += tokens.size();
            new HashSet<>(tokens).forEach(token -> documentFrequency.merge(token, 1, Integer::sum));
        }

        double avgDocumentLength = userChunks.isEmpty() ? 1.0 : Math.max(1.0, (double) totalTokenCount / userChunks.size());
        return userChunks.stream()
                .map(chunk -> new ScoredChunk(chunk, bm25Score(chunk, documentTokens.getOrDefault(chunk.getId(), List.of()),
                        queryTokens, documentFrequency, userChunks.size(), avgDocumentLength)))
                .filter(scored -> scored.score() > 0.0)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .map(ScoredChunk::chunk)
                .toList();
    }

    private double bm25Score(DocumentChunk chunk, List<String> documentTokens, List<String> queryTokens,
                             Map<String, Integer> documentFrequency, int documentCount, double avgDocumentLength) {
        if (documentTokens.isEmpty() || documentCount == 0) {
            return 0.0;
        }
        Map<String, Integer> termFrequency = new HashMap<>();
        documentTokens.forEach(token -> termFrequency.merge(token, 1, Integer::sum));

        double k1 = 1.5;
        double b = 0.75;
        double score = 0.0;
        for (String token : queryTokens) {
            int tf = termFrequency.getOrDefault(token, 0);
            if (tf == 0) {
                continue;
            }
            int df = documentFrequency.getOrDefault(token, 0);
            double idf = Math.log(1.0 + (documentCount - df + 0.5) / (df + 0.5));
            double denominator = tf + k1 * (1.0 - b + b * documentTokens.size() / avgDocumentLength);
            score += idf * (tf * (k1 + 1.0)) / denominator;
        }

        String normalizedTitle = normalize(chunk.getTitle());
        for (String token : queryTokens) {
            if (normalizedTitle.contains(token)) {
                score += 0.5;
            }
        }
        return score;
    }

    private List<String> tokenize(String text) {
        String normalized = normalize(text);
        Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        for (int i = 0; i < normalized.length() - 1; i++) {
            char first = normalized.charAt(i);
            char second = normalized.charAt(i + 1);
            if (isCjk(first) && isCjk(second)) {
                tokens.add("" + first + second);
            }
        }
        return tokens;
    }

    private boolean isCjk(char c) {
        return c >= '\u4e00' && c <= '\u9fff';
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

    private List<ChunkCandidate> splitTextWithOffsets(String text, int maxChars) {
        List<String> parts = splitText(text, maxChars);
        List<ChunkCandidate> chunks = new ArrayList<>();
        int cursor = 0;
        for (String part : parts) {
            int start = text.indexOf(part, cursor);
            if (start < 0) {
                start = Math.min(cursor, text.length());
            }
            int end = Math.min(start + part.length(), text.length());
            chunks.add(new ChunkCandidate(part, start, end));
            cursor = Math.max(start + 1, end);
        }
        return chunks;
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

    private record ScoredChunk(DocumentChunk chunk, double score) {
    }

    private record ChunkCandidate(String content, int startOffset, int endOffset) {
    }
}
