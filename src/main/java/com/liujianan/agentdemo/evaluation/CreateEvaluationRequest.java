package com.liujianan.agentdemo.evaluation;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateEvaluationRequest(
        @NotBlank String question,
        List<String> expectedKeywords,
        String feedback
) {
}
