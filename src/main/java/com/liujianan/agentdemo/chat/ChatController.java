package com.liujianan.agentdemo.chat;

import com.liujianan.agentdemo.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final TaskExecutor taskExecutor;

    public ChatController(ChatService chatService, TaskExecutor taskExecutor) {
        this.chatService = chatService;
        this.taskExecutor = taskExecutor;
    }

    @PostMapping
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(chatService.answer(request, userId));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        SseEmitter emitter = new SseEmitter(60_000L);
        emitter.onCompletion(() -> {});
        emitter.onError(throwable -> {});
        emitter.onTimeout(() -> {});
        String userId = (String) httpRequest.getAttribute("userId");
        taskExecutor.execute(() -> {
            try {
                chatService.answerStream(request, userId,
                        sessionId -> sendEvent(emitter, "session", sessionId),
                        sources -> sendEvent(emitter, "sources", String.valueOf(sources.size())),
                        delta -> sendEvent(emitter, "delta", delta),
                        latency -> {
                            sendEvent(emitter, "done", "latencyMs=" + latency);
                            emitter.complete();
                        }
                );
            } catch (Exception e) {
                sendEvent(emitter, "error", e.getMessage());
                emitter.complete();
            }
        });
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException ignored) {}
    }
}
