package com.liujianan.agentdemo.evaluation;

import java.util.List;

public record QaReviewRequest(
        String question,
        String answer,
        List<QaSource> sources,
        List<String> expectedKeywords
) {
    public record QaSource(String title, String content, String chunkId, String tags, double score) {
    }
}
