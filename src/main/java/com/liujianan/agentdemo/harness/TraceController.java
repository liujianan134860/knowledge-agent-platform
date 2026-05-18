package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.common.ApiResponse;
import com.liujianan.agentdemo.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public ApiResponse<List<TraceEventResponse>> list(@RequestParam(required = false) String sessionId,
                                                       HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(traceRecorder.list(sessionId, userId).stream().map(TraceEventResponse::from).toList());
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<TraceEventResponse>> page(@RequestParam(required = false) String sessionId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "50") int size,
                                                              HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(PageResponse.from(traceRecorder.list(sessionId, userId, pageable).map(TraceEventResponse::from)));
    }
}
