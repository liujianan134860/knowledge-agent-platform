package com.liujianan.agentdemo.evaluation;

import java.util.List;

public record QaReviewResult(
        String question,
        boolean retrievalHit,
        boolean citationPresent,
        boolean answerContainsExpectedKeywords,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        boolean hasUnsupportedClaims,
        double score,
        String summary
) {
}
