package com.liujianan.agentdemo.evaluation;

import java.time.LocalDateTime;
import java.util.List;

public record EvaluationCaseResponse(
        Long id,
        String question,
        List<String> expectedKeywords,
        String feedback,
        LocalDateTime createdAt
) {
    public static EvaluationCaseResponse from(EvaluationCase evaluationCase) {
        return new EvaluationCaseResponse(
                evaluationCase.getId(),
                evaluationCase.getQuestion(),
                evaluationCase.getExpectedKeywords(),
                evaluationCase.getFeedback(),
                evaluationCase.getCreatedAt()
        );
    }
}
