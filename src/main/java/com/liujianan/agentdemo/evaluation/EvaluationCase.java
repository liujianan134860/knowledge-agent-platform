package com.liujianan.agentdemo.evaluation;

import java.time.LocalDateTime;
import java.util.List;

public record EvaluationCase(
        Long id,
        String question,
        List<String> expectedKeywords,
        String feedback,
        LocalDateTime createdAt
) {
}
