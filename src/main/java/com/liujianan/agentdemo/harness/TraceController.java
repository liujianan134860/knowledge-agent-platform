package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/traces")
public class TraceController {
    private final TraceRecorder traceRecorder;

    public TraceController(TraceRecorder traceRecorder) {
        this.traceRecorder = traceRecorder;
    }

    @GetMapping
    public ApiResponse<List<TraceEvent>> list(@RequestParam(required = false) String sessionId,
                                               HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(traceRecorder.list(sessionId, userId));
    }
}
