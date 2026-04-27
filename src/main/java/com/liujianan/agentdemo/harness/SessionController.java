package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ApiResponse<ChatSession> create() {
        return ApiResponse.ok(sessionService.create());
    }

    @GetMapping
    public ApiResponse<List<ChatSession>> list() {
        return ApiResponse.ok(sessionService.list());
    }
}
