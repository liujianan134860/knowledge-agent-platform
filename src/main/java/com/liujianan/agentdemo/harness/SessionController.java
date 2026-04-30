package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ApiResponse<ChatSession> create(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(sessionService.create(userId));
    }

    @GetMapping
    public ApiResponse<List<ChatSession>> list(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(sessionService.list(userId));
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Boolean> delete(@PathVariable String sessionId, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        boolean deleted = sessionService.delete(sessionId, userId);
        return deleted ? ApiResponse.ok(true) : ApiResponse.fail("session not found: " + sessionId);
    }
}
