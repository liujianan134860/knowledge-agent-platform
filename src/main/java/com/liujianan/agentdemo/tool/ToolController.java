package com.liujianan.agentdemo.tool;

import com.liujianan.agentdemo.common.ApiResponse;
import com.liujianan.agentdemo.common.HarnessMetrics;
import com.liujianan.agentdemo.harness.TraceRecorder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tools")
public class ToolController {
    private final ToolRegistry toolRegistry;
    private final TraceRecorder traceRecorder;
    private final HarnessMetrics harnessMetrics;

    public ToolController(ToolRegistry toolRegistry, TraceRecorder traceRecorder, HarnessMetrics harnessMetrics) {
        this.toolRegistry = toolRegistry;
        this.traceRecorder = traceRecorder;
        this.harnessMetrics = harnessMetrics;
    }

    @GetMapping
    public ApiResponse<List<ToolDefinition>> listTools() {
        return ApiResponse.ok(toolRegistry.listTools());
    }

    @PostMapping("/{name}/invoke")
    public ApiResponse<ToolResult> invoke(@PathVariable String name,
                                           @Valid @RequestBody ToolInvokeRequest request,
                                           HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        long start = System.currentTimeMillis();
        harnessMetrics.recordToolInvocation();
        try {
            ToolResult result = toolRegistry.invoke(name, request.input());
            harnessMetrics.recordToolSuccess();
            traceRecorder.record("tool-direct", "TOOL_CALL", "invoked tool " + name,
                    Map.of("latencyMs", System.currentTimeMillis() - start), userId);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            harnessMetrics.recordToolFailure();
            traceRecorder.record("tool-direct", "TOOL_CALL", "tool " + name + " failed: " + e.getMessage(),
                    Map.of("latencyMs", System.currentTimeMillis() - start), userId);
            throw e;
        }
    }
}
