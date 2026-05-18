package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.common.ApiResponse;
import com.liujianan.agentdemo.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ApiResponse<SessionResponse> create(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(SessionResponse.from(sessionService.create(userId)));
    }

    @GetMapping
    public ApiResponse<List<SessionResponse>> list(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(sessionService.list(userId).stream().map(SessionResponse::from).toList());
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<SessionResponse>> page(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ApiResponse.ok(PageResponse.from(sessionService.list(userId, pageable).map(SessionResponse::from)));
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Boolean> delete(@PathVariable String sessionId, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        boolean deleted = sessionService.delete(sessionId, userId);
        return deleted ? ApiResponse.ok(true) : ApiResponse.fail("SESSION_NOT_FOUND", "session not found: " + sessionId);
    }
}
