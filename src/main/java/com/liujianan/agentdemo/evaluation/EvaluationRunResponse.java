package com.liujianan.agentdemo.evaluation;

import java.time.LocalDateTime;

public record EvaluationRunResponse(
        Long id,
        Long caseId,
        double score,
        String promptVersion,
        String modelVersion,
        String retrievalVersion,
        String summary,
        LocalDateTime createdAt
) {
    public static EvaluationRunResponse from(EvaluationRun run) {
        return new EvaluationRunResponse(run.getId(), run.getCaseId(), run.getScore(), run.getPromptVersion(),
                run.getModelVersion(), run.getRetrievalVersion(), run.getSummary(), run.getCreatedAt());
    }
}
