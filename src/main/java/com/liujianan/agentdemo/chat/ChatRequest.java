package com.liujianan.agentdemo.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank
        @Size(max = 300)
        String question,

        String sessionId
) {
}
