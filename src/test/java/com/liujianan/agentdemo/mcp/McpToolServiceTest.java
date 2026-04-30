package com.liujianan.agentdemo.mcp;

import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.knowledge.KnowledgeService;
import com.liujianan.agentdemo.tool.ToolRegistry;
import com.liujianan.agentdemo.tool.ToolResult;
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
    private ToolRegistry toolRegistry;

    @Mock
    private KnowledgeService knowledgeService;

    private McpToolService mcpToolService;

    @BeforeEach
    void setUp() {
        mcpToolService = new McpToolService(toolRegistry, knowledgeService);
    }

    @Test
    void calculator_shouldDelegateToToolRegistry() {
        when(toolRegistry.invoke("calculator", "12 + 30"))
                .thenReturn(new ToolResult("calculator", "42"));

        String result = mcpToolService.calculator("12 + 30");

        assertEquals("42", result);
        verify(toolRegistry).invoke("calculator", "12 + 30");
    }

    @Test
    void calculator_whenError_shouldReturnErrorMessage() {
        when(toolRegistry.invoke("calculator", "invalid"))
                .thenThrow(new IllegalArgumentException("invalid input"));

        String result = mcpToolService.calculator("invalid");

        assertTrue(result.contains("Error"));
        assertTrue(result.contains("invalid input"));
    }

    @Test
    void echo_shouldDelegateToToolRegistry() {
        when(toolRegistry.invoke("echo", "hello"))
                .thenReturn(new ToolResult("echo", "hello"));

        String result = mcpToolService.echo("hello");

        assertEquals("hello", result);
        verify(toolRegistry).invoke("echo", "hello");
    }

    @Test
    void searchKnowledge_shouldReturnFormattedResults() {
        List<DocumentChunk> chunks = List.of(
                new DocumentChunk(1L, "Java Basics", "Java is a programming language.",
                        List.of("java"), LocalDateTime.now(), "user1"),
                new DocumentChunk(2L, "Python Guide", "Python is a dynamic language.",
                        List.of("python"), LocalDateTime.now(), "user1")
        );

        when(knowledgeService.search("programming languages", 5, "user1"))
                .thenReturn(chunks);

        String result = mcpToolService.searchKnowledge("programming languages", "user1");

        assertTrue(result.contains("Found 2 relevant chunks"));
        assertTrue(result.contains("Java Basics"));
        assertTrue(result.contains("Java is a programming language"));
        assertTrue(result.contains("Python Guide"));
        assertTrue(result.contains("Python is a dynamic language"));
    }

    @Test
    void searchKnowledge_whenNoResults_shouldReturnNoResultsMessage() {
        when(knowledgeService.search("unknown topic", 5, "user1"))
                .thenReturn(List.of());

        String result = mcpToolService.searchKnowledge("unknown topic", "user1");

        assertTrue(result.contains("No relevant knowledge found"));
    }

    @Test
    void searchKnowledge_whenError_shouldReturnError() {
        when(knowledgeService.search(anyString(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("DB error"));

        String result = mcpToolService.searchKnowledge("test", "user1");

        assertTrue(result.contains("Search failed"));
        assertTrue(result.contains("DB error"));
    }

    @Test
    void httpMock_shouldDelegateToToolRegistry() {
        when(toolRegistry.invoke("http_mock", "GET /api/projects"))
                .thenReturn(new ToolResult("http_mock", "mock response for: GET /api/projects"));

        String result = mcpToolService.httpMock("GET /api/projects");

        assertEquals("mock response for: GET /api/projects", result);
        verify(toolRegistry).invoke("http_mock", "GET /api/projects");
    }

    @Test
    void platformStatus_shouldReturnFormattedStatus() {
        when(knowledgeService.list("user1")).thenReturn(List.of(
                new DocumentChunk(1L, "Doc1", "Content1", List.of("tag1"), LocalDateTime.now(), "user1")
        ));
        when(toolRegistry.listTools()).thenReturn(List.of(
                new com.liujianan.agentdemo.tool.ToolDefinition(
                        "calculator", "Calculate", "1+1", "{}", "PUBLIC", 1000),
                new com.liujianan.agentdemo.tool.ToolDefinition(
                        "echo", "Echo", "hello", "{}", "PUBLIC", 1000)
        ));

        String result = mcpToolService.platformStatus("user1");

        assertTrue(result.contains("Knowledge chunks: 1"));
        assertTrue(result.contains("calculator"));
        assertTrue(result.contains("echo"));
    }

    @Test
    void platformStatus_whenError_shouldReturnError() {
        when(knowledgeService.list("user1")).thenThrow(new RuntimeException("Error"));

        String result = mcpToolService.platformStatus("user1");

        assertTrue(result.contains("Status check failed"));
    }
}
