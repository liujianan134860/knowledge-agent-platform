package com.liujianan.agentdemo.tool;

import com.liujianan.agentdemo.common.ApiResponse;
import com.liujianan.agentdemo.common.HarnessMetrics;
import com.liujianan.agentdemo.harness.TraceRecorder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolControllerTest {

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private TraceRecorder traceRecorder;

    private HarnessMetrics harnessMetrics;
    private ToolController toolController;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        harnessMetrics = new HarnessMetrics(new SimpleMeterRegistry());
        toolController = new ToolController(toolRegistry, traceRecorder, harnessMetrics);
        request = new MockHttpServletRequest();
        request.setAttribute("userId", "testUser");
    }

    @Test
    void listTools_shouldReturnTools() {
        when(toolRegistry.listTools()).thenReturn(List.of(
                new ToolDefinition("echo", "Echo test", "hello", "{}", "PUBLIC", 1000)
        ));

        ApiResponse<List<ToolDefinition>> response = toolController.listTools();

        assertTrue(response.success());
        assertEquals(1, response.data().size());
        assertEquals("echo", response.data().get(0).name());
    }

    @Test
    void invoke_shouldReturnResultAndRecordMetrics() {
        when(toolRegistry.invoke("echo", "hello"))
                .thenReturn(new ToolResult("echo", "hello"));

        ApiResponse<ToolResult> response = toolController.invoke("echo",
                new ToolInvokeRequest("hello"), request);

        assertTrue(response.success());
        assertEquals("hello", response.data().output());
        assertEquals(1.0, harnessMetrics.getToolInvokeCount(), 0.001);
        assertEquals(1.0, harnessMetrics.getToolSuccessCount(), 0.001);
        assertEquals(0.0, harnessMetrics.getToolFailureCount(), 0.001);
        verify(traceRecorder).record(anyString(), anyString(), anyString(), anyMap(), anyString());
    }

    @Test
    void invoke_whenToolThrowsException_shouldRecordFailureMetric() {
        when(toolRegistry.invoke("calculator", "bad input"))
                .thenThrow(new IllegalArgumentException("Invalid input"));

        assertThrows(IllegalArgumentException.class, () -> {
            toolController.invoke("calculator", new ToolInvokeRequest("bad input"), request);
        });

        assertEquals(1.0, harnessMetrics.getToolInvokeCount(), 0.001);
        assertEquals(0.0, harnessMetrics.getToolSuccessCount(), 0.001);
        assertEquals(1.0, harnessMetrics.getToolFailureCount(), 0.001);
    }
}
