package com.liujianan.agentdemo.mcp;

import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.knowledge.KnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpToolServiceTest {

    @Mock
    private KnowledgeService knowledgeService;

    private McpToolService mcpToolService;

    @BeforeEach
    void setUp() {
        mcpToolService = new McpToolService(knowledgeService);
    }

    @Test
    void searchKnowledge_shouldReturnFormattedResults() {
        List<DocumentChunk> chunks = List.of(
                new DocumentChunk(1L, "Java Basics", "Java is a programming language.",
                        List.of("java"), LocalDateTime.now(), "user1"),
                new DocumentChunk(2L, "Python Guide", "Python is a dynamic language.",
                        List.of("python"), LocalDateTime.now(), "user1")
        );

        when(knowledgeService.search("programming languages", 5, "anonymous"))
                .thenReturn(chunks);

        String result = mcpToolService.searchKnowledge("programming languages");

        assertTrue(result.contains("Found 2 relevant chunks"));
        assertTrue(result.contains("Java Basics"));
    }

    @Test
    void searchKnowledge_whenNoResults_shouldReturnNoResultsMessage() {
        when(knowledgeService.search("unknown topic", 5, "anonymous"))
                .thenReturn(List.of());

        String result = mcpToolService.searchKnowledge("unknown topic");

        assertTrue(result.contains("No relevant knowledge found"));
    }

    @Test
    void platformStatus_shouldReturnFormattedStatus() {
        when(knowledgeService.list("anonymous")).thenReturn(List.of(
                new DocumentChunk(1L, "Doc1", "Content1", List.of("tag1"), LocalDateTime.now(), "user1")
        ));

        String result = mcpToolService.platformStatus();

        assertTrue(result.contains("Knowledge chunks: 1"));
    }
}
