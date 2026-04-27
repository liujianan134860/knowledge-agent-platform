package com.liujianan.agentdemo.tool;

import com.liujianan.agentdemo.common.ApiResponse;
import com.liujianan.agentdemo.harness.TraceRecorder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {
    private final ToolRegistry toolRegistry;
    private final TraceRecorder traceRecorder;

    public ToolController(ToolRegistry toolRegistry, TraceRecorder traceRecorder) {
        this.toolRegistry = toolRegistry;
        this.traceRecorder = traceRecorder;
    }

    @GetMapping
    public ApiResponse<List<ToolDefinition>> listTools() {
        return ApiResponse.ok(toolRegistry.listTools());
    }

    @PostMapping("/{name}/invoke")
    public ApiResponse<ToolResult> invoke(@PathVariable String name, @Valid @RequestBody ToolInvokeRequest request) {
        long start = System.currentTimeMillis();
        ToolResult result = toolRegistry.invoke(name, request.input());
        traceRecorder.record("tool-direct", "TOOL_CALL", "invoked tool " + name, java.util.Map.of("latencyMs", System.currentTimeMillis() - start));
        return ApiResponse.ok(result);
    }
}
