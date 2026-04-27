package com.liujianan.agentdemo.chat;

import com.liujianan.agentdemo.common.ApiResponse;
import jakarta.validation.Valid;
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

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(chatService.answer(request));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) throws IOException {
        SseEmitter emitter = new SseEmitter(30_000L);
        ChatResponse response = chatService.answer(request);
        emitter.send(SseEmitter.event().name("session").data(response.sessionId()));
        emitter.send(SseEmitter.event().name("sources").data(response.sources().size()));
        for (String part : response.answer().split(" ")) {
            emitter.send(SseEmitter.event().name("delta").data(part + " "));
        }
        emitter.send(SseEmitter.event().name("done").data("latencyMs=" + response.latencyMs()));
        emitter.complete();
        return emitter;
    }
}
