package com.liujianan.agentdemo.knowledge;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentChunk(
        Long id,
        String title,
        String content,
        List<String> tags,
        LocalDateTime createdAt
) {
}
