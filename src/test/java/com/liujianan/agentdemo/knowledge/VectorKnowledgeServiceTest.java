package com.liujianan.agentdemo.knowledge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorKnowledgeServiceTest {

    @Mock
    private VectorStore vectorStore;

    private VectorKnowledgeService vectorKnowledgeService;

    @BeforeEach
    void setUp() {
        vectorKnowledgeService = new VectorKnowledgeService(vectorStore);
    }

    @Test
    void index_shouldConvertChunkToDocumentAndAddToVectorStore() {
        // Arrange
        DocumentChunk chunk = new DocumentChunk(
                42L, "Test Title", "This is the test content of the chunk.",
                List.of("tag1", "tag2"), LocalDateTime.now(), "user123"
        );

        // Act
        Document result = vectorKnowledgeService.index(chunk);

        // Assert
        assertNotNull(result);
        assertEquals("This is the test content of the chunk.", result.getText());
        assertEquals("42", result.getMetadata().get("chunkId"));
        assertEquals("Test Title", result.getMetadata().get("title"));
        assertEquals("user123", result.getMetadata().get("userId"));
        assertEquals("tag1,tag2", result.getMetadata().get("tags"));

        verify(vectorStore, times(1)).add(anyList());
    }

    @Test
    void index_shouldHandleNullFields() {
        // Arrange
        DocumentChunk chunk = new DocumentChunk(
                1L, null, "Content only.",
                null, LocalDateTime.now(), null
        );

        // Act
        Document result = vectorKnowledgeService.index(chunk);

        // Assert
        assertEquals("Content only.", result.getText());
        assertEquals("1", result.getMetadata().get("chunkId"));
        assertEquals("", result.getMetadata().get("title"));
        assertEquals("", result.getMetadata().get("userId"));
        assertEquals("", result.getMetadata().get("tags"));
    }

    @Test
    void search_shouldBuildCorrectSearchRequest() {
        // Arrange
        Document mockDoc1 = new Document("Content1", Map.of("chunkId", "1", "userId", "user1"));
        Document mockDoc2 = new Document("Content2", Map.of("chunkId", "2", "userId", "user1"));

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(mockDoc1, mockDoc2));

        // Act
        List<Document> results = vectorKnowledgeService.search("test query", 5, "user1");

        // Assert
        assertEquals(2, results.size());
        assertEquals("Content1", results.get(0).getText());
        assertEquals("Content2", results.get(1).getText());

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());

        SearchRequest captured = requestCaptor.getValue();
        assertTrue(captured.getQuery().contains("test query"));
        assertEquals(5, captured.getTopK());
    }

    @Test
    void search_shouldCapTopK() {
        // Arrange
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());

        // Act
        List<Document> results = vectorKnowledgeService.search("query", 50, "user1");

        // Assert
        assertTrue(results.isEmpty());
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        assertEquals(20, requestCaptor.getValue().getTopK());
    }

    @Test
    void searchAll_shouldNotUseUserFilter() {
        // Arrange
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("Content", Map.of())));

        // Act
        List<Document> results = vectorKnowledgeService.searchAll("query", 3);

        // Assert
        assertEquals(1, results.size());
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void deleteByChunkId_shouldDeleteMatchingDocuments() {
        // Arrange
        Document matchingDoc = new Document("doc-id-1", "Content",
                Map.of("chunkId", "123"));
        Document nonMatchingDoc = new Document("doc-id-2", "Other",
                Map.of("chunkId", "456"));

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(matchingDoc, nonMatchingDoc));

        // Act
        vectorKnowledgeService.deleteByChunkId(123L);

        // Assert
        verify(vectorStore).delete(List.of("doc-id-1"));
    }

    @Test
    void deleteByChunkId_whenNoMatchingDocuments_shouldNotDelete() {
        // Arrange
        Document nonMatchingDoc = new Document("doc-id-2", "Other",
                Map.of("chunkId", "999"));

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(nonMatchingDoc));

        // Act
        vectorKnowledgeService.deleteByChunkId(123L);

        // Assert
        verify(vectorStore, never()).delete(anyList());
    }
}
