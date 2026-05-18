package com.liujianan.agentdemo.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Vector-based knowledge service using Spring AI VectorStore (pgvector).
 * Indexes DocumentChunks as embeddings and performs similarity search.
 */
@Service
@ConditionalOnBean(VectorStore.class)
public class VectorKnowledgeService {
    private static final Logger log = LoggerFactory.getLogger(VectorKnowledgeService.class);

    private final VectorStore vectorStore;

    public VectorKnowledgeService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        log.info("VectorKnowledgeService initialized with VectorStore: {}", vectorStore.getClass().getSimpleName());
    }

    /**
     * Index a DocumentChunk into the vector store.
     */
    public Document index(DocumentChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("chunkId", String.valueOf(chunk.getId()));
        metadata.put("documentId", chunk.getDocumentId() != null ? chunk.getDocumentId() : "");
        metadata.put("chunkIndex", chunk.getChunkIndex() == null ? "" : String.valueOf(chunk.getChunkIndex()));
        metadata.put("pageNumber", chunk.getPageNumber() == null ? "" : String.valueOf(chunk.getPageNumber()));
        metadata.put("startOffset", chunk.getStartOffset() == null ? "" : String.valueOf(chunk.getStartOffset()));
        metadata.put("endOffset", chunk.getEndOffset() == null ? "" : String.valueOf(chunk.getEndOffset()));
        metadata.put("sourceType", chunk.getSourceType() != null ? chunk.getSourceType() : "");
        metadata.put("sourceName", chunk.getSourceName() != null ? chunk.getSourceName() : "");
        metadata.put("title", chunk.getTitle() != null ? chunk.getTitle() : "");
        metadata.put("userId", chunk.getUserId() != null ? chunk.getUserId() : "");
        metadata.put("tags", chunk.getTags() != null ? String.join(",", chunk.getTags()) : "");
        Document document = new Document(
                chunk.getContent(),
                metadata
        );
        vectorStore.add(List.of(document));
        log.debug("Indexed chunk {}: {}...", chunk.getId(), truncate(chunk.getContent(), 60));
        return document;
    }

    /**
     * Search for similar documents by query.
     */
    public List<Document> search(String query, int topK, String userId) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(Math.min(topK, 20))
                .similarityThreshold(0.0)
                .filterExpression("userId == '" + userId + "'")
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        log.debug("Vector search for '{}' returned {} results (topK={})", truncate(query, 40), results.size(), topK);
        return results;
    }

    /**
     * Search without user filter.
     */
    public List<Document> searchAll(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(Math.min(topK, 20))
                .similarityThreshold(0.0)
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * Delete a document from vector store by chunk ID.
     */
    public void deleteByChunkId(Long chunkId) {
        List<Document> existing = searchAll(String.valueOf(chunkId), 10);
        List<String> docIds = existing.stream()
                .filter(doc -> chunkId.toString().equals(doc.getMetadata().get("chunkId")))
                .map(Document::getId)
                .collect(Collectors.toList());
        if (!docIds.isEmpty()) {
            vectorStore.delete(docIds);
            log.debug("Deleted {} vector documents for chunk {}", docIds.size(), chunkId);
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
