package com.liujianan.agentdemo.knowledge;

import com.liujianan.agentdemo.common.AiPlatformProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private DocumentChunkRepository documentRepository;

    @Mock
    private DocumentTextExtractor documentTextExtractor;

    @Mock
    private VectorKnowledgeService vectorKnowledgeService;

    @Mock
    private RerankClient rerankClient;

    private AiPlatformProperties aiPlatformProperties;
    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        aiPlatformProperties = new AiPlatformProperties();
        knowledgeService = new KnowledgeService(documentRepository, documentTextExtractor,
                Optional.of(vectorKnowledgeService), Optional.empty(), aiPlatformProperties);
    }

    @Test
    void add_shouldSaveChunkAndIndexInVectorStore() {
        AddDocumentRequest request = new AddDocumentRequest("Test Title", "Test content", List.of("tag1"));

        DocumentChunk saved = new DocumentChunk(1L, "Test Title", "Test content",
                List.of("tag1"), LocalDateTime.now(), "user1");

        when(documentRepository.save(any(DocumentChunk.class))).thenReturn(saved);

        DocumentChunk result = knowledgeService.add(request, "user1");

        assertEquals(1L, result.getId());
        assertEquals("Test Title", result.getTitle());
        verify(documentRepository).save(any(DocumentChunk.class));
        // Verify vector store indexing was called
        verify(vectorKnowledgeService).index(eq(saved));
    }

    @Test
    void add_whenVectorStoreThrows_shouldStillSaveChunk() {
        AddDocumentRequest request = new AddDocumentRequest("Title", "Content", List.of());

        DocumentChunk saved = new DocumentChunk(1L, "Title", "Content",
                List.of(), LocalDateTime.now(), "user1");

        when(documentRepository.save(any(DocumentChunk.class))).thenReturn(saved);
        doThrow(new RuntimeException("Vector store unavailable")).when(vectorKnowledgeService).index(any());

        // Should not throw - vector indexing is best-effort
        DocumentChunk result = knowledgeService.add(request, "user1");

        assertNotNull(result);
        verify(documentRepository).save(any(DocumentChunk.class));
        verify(vectorKnowledgeService).index(eq(saved));
    }

    @Test
    void delete_shouldRemoveChunkAndDeleteFromVectorStore() {
        DocumentChunk chunk = new DocumentChunk(1L, "Title", "Content",
                List.of(), LocalDateTime.now(), "user1");

        when(documentRepository.findById(1L)).thenReturn(Optional.of(chunk));

        boolean result = knowledgeService.delete(1L, "user1");

        assertTrue(result);
        verify(documentRepository).delete(chunk);
        verify(vectorKnowledgeService).deleteByChunkId(1L);
    }

    @Test
    void delete_whenChunkNotFound_shouldReturnFalse() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = knowledgeService.delete(999L, "user1");

        assertFalse(result);
        verify(documentRepository, never()).delete(any());
        verify(vectorKnowledgeService, never()).deleteByChunkId(anyLong());
    }

    @Test
    void search_whenVectorStoreAvailable_shouldTryVectorFirst() {
        org.springframework.ai.document.Document vectorDoc = new org.springframework.ai.document.Document(
                "Content 1", Map.of("chunkId", "1", "userId", "user1"));

        when(vectorKnowledgeService.search("query", 12, "user1"))
                .thenReturn(List.of(vectorDoc));

        DocumentChunk dbChunk = new DocumentChunk(1L, "Title", "Content 1",
                List.of(), LocalDateTime.now(), "user1");
        when(documentRepository.findAllById(List.of(1L))).thenReturn(List.of(dbChunk));

        List<DocumentChunk> results = knowledgeService.search("query", 3, "user1");

        assertEquals(1, results.size());
        assertEquals("Content 1", results.get(0).getContent());
        verify(vectorKnowledgeService).search("query", 12, "user1");
        verify(documentRepository).findAllById(List.of(1L));
    }

    @Test
    void search_whenVectorSearchFails_shouldFallbackToKeyword() {
        when(vectorKnowledgeService.search("query", 12, "user1"))
                .thenThrow(new RuntimeException("Vector search failed"));

        when(documentRepository.findByUserId("user1")).thenReturn(List.of(
                new DocumentChunk(1L, "Match", "query content here", List.of(), LocalDateTime.now(), "user1")
        ));

        List<DocumentChunk> results = knowledgeService.search("query", 3, "user1");

        assertFalse(results.isEmpty());
        verify(vectorKnowledgeService).search("query", 12, "user1");
        verify(documentRepository).findByUserId("user1");
    }

    @Test
    void search_whenVectorStoreNotPresent_shouldSkipVectorSearch() {
        KnowledgeService ksWithoutVector = new KnowledgeService(documentRepository, documentTextExtractor,
                Optional.empty(), Optional.empty(), aiPlatformProperties);

        when(documentRepository.findByUserId("user1")).thenReturn(List.of(
                new DocumentChunk(1L, "Title", "Some content", List.of(), LocalDateTime.now(), "user1")
        ));

        List<DocumentChunk> results = ksWithoutVector.search("content", 3, "user1");

        assertFalse(results.isEmpty());
        verify(vectorKnowledgeService, never()).search(anyString(), anyInt(), anyString());
    }

    @Test
    void search_keywordFallback_shouldRankByBm25Score() {
        KnowledgeService ksWithoutVector = new KnowledgeService(documentRepository, documentTextExtractor,
                Optional.empty(), Optional.empty(), aiPlatformProperties);

        DocumentChunk weak = new DocumentChunk(1L, "Spring overview",
                "Spring is a Java framework.", List.of(), LocalDateTime.now(), "user1");
        DocumentChunk strong = new DocumentChunk(2L, "Dependency injection",
                "Dependency injection is a core Spring feature. Dependency injection improves testability.",
                List.of("spring"), LocalDateTime.now(), "user1");
        when(documentRepository.findByUserId("user1")).thenReturn(List.of(weak, strong));

        List<DocumentChunk> results = ksWithoutVector.search("dependency injection", 2, "user1");

        assertEquals(2L, results.get(0).getId());
    }

    @Test
    void search_whenRerankConfigured_shouldRerankMergedCandidates() {
        KnowledgeService ksWithRerank = new KnowledgeService(documentRepository, documentTextExtractor,
                Optional.empty(), Optional.of(rerankClient), aiPlatformProperties);
        DocumentChunk first = new DocumentChunk(1L, "Generic", "Spring dependency overview.",
                List.of(), LocalDateTime.now(), "user1");
        DocumentChunk second = new DocumentChunk(2L, "Precise", "Dependency injection in Spring.",
                List.of(), LocalDateTime.now(), "user1");
        when(documentRepository.findByUserId("user1")).thenReturn(List.of(first, second));
        when(rerankClient.isConfigured()).thenReturn(true);
        when(rerankClient.rerank(eq("dependency injection"), anyList(), eq(1)))
                .thenReturn(List.of(new RerankResult(second, 0.98, "qwen3-rerank")));

        List<DocumentChunk> results = ksWithRerank.search("dependency injection", 1, "user1");

        assertEquals(2L, results.get(0).getId());
        verify(rerankClient).rerank(eq("dependency injection"), anyList(), eq(1));
    }

    @Test
    void search_withInvalidTopK_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                knowledgeService.search("query", 0, "user1"));
        assertThrows(IllegalArgumentException.class, () ->
                knowledgeService.search("query", 11, "user1"));
    }

    @Test
    void list_shouldReturnUserChunks() {
        when(documentRepository.findByUserId("user1")).thenReturn(List.of(
                new DocumentChunk(1L, "Title1", "Content1", List.of(), LocalDateTime.now(), "user1")
        ));

        List<DocumentChunk> results = knowledgeService.list("user1");

        assertEquals(1, results.size());
        verify(documentRepository).findByUserId("user1");
    }

    @Test
    void upload_shouldExtractSplitSaveAndIndex() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.txt");
        when(file.isEmpty()).thenReturn(false);
        when(documentTextExtractor.extractWithMetadata(file)).thenReturn(new DocumentTextExtractor.ExtractedDocument(
                "This is test content for upload.",
                "text",
                List.of(new DocumentTextExtractor.PageSpan(1, 0, 32))
        ));

        when(documentRepository.save(any(DocumentChunk.class))).thenAnswer(invocation -> {
            DocumentChunk chunk = invocation.getArgument(0);
            chunk.setId(1L);
            return chunk;
        });

        DocumentUploadResponse response = knowledgeService.upload(file, null, null, "user1");

        assertNotNull(response);
        assertEquals("test.txt", response.filename());
        assertEquals(1, response.chunkCount());
        assertEquals(0, response.chunks().get(0).getChunkIndex());
        assertEquals(1, response.chunks().get(0).getPageNumber());
        assertEquals("text", response.chunks().get(0).getSourceType());
        verify(documentRepository).save(any(DocumentChunk.class));
        verify(vectorKnowledgeService).index(any(DocumentChunk.class));
    }
}
