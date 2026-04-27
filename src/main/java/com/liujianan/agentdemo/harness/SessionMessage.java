package com.liujianan.agentdemo.harness;

import java.time.LocalDateTime;

public record SessionMessage(
        String role,
        String content,
        LocalDateTime createdAt
) {
}
