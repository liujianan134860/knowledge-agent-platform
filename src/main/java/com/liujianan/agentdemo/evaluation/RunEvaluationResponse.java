package com.liujianan.agentdemo.evaluation;

public record RunEvaluationResponse(
        String question,
        String answer,
        boolean retrievalHit,
        boolean citationPresent,
        boolean keywordMatch,
        boolean hasUnsupportedClaims,
        double score,
        String summary
) {
}
