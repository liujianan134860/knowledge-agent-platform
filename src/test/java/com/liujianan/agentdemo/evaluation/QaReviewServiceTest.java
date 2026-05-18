package com.liujianan.agentdemo.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QaReviewServiceTest {

    @Test
    void review_forGreetingWithoutSources_shouldNotRequireCitation() {
        SemanticJudgeService semanticJudgeService = mock(SemanticJudgeService.class);
        when(semanticJudgeService.judge(anyString(), anyString(), anyString(), any()))
                .thenReturn(new SemanticJudgeService.JudgeResult(1.0, "qwen:qwen3.6-plus",
                        "score: 1.0 reason: greeting answered correctly"));
        QaReviewService service = new QaReviewService(semanticJudgeService);

        QaReviewResult result = service.review(new QaReviewRequest(
                "nihao",
                "你好！很高兴见到你。",
                List.of(),
                List.of()
        ));

        assertFalse(result.hasUnsupportedClaims());
        assertTrue(result.summary().contains("source citation not required"));
        assertTrue(result.summary().contains("Rule reference score: 100/100"));
    }

    @Test
    void review_forRagQuestionWithoutSources_shouldReportMissingGrounding() {
        SemanticJudgeService semanticJudgeService = mock(SemanticJudgeService.class);
        when(semanticJudgeService.judge(anyString(), anyString(), anyString(), any()))
                .thenReturn(new SemanticJudgeService.JudgeResult(0.3, "fallback", "not grounded"));
        QaReviewService service = new QaReviewService(semanticJudgeService);

        QaReviewResult result = service.review(new QaReviewRequest(
                "根据知识库说明一下项目架构",
                "这个项目使用了后端服务和前端页面。",
                List.of(),
                List.of()
        ));

        assertTrue(result.hasUnsupportedClaims());
        assertTrue(result.summary().contains("RAG grounding expected"));
        assertTrue(result.summary().contains("No knowledge source retrieved"));
    }

    @Test
    void review_forRagQuestionWithoutSources_shouldCapHighJudgeScore() {
        SemanticJudgeService semanticJudgeService = mock(SemanticJudgeService.class);
        when(semanticJudgeService.judge(anyString(), anyString(), anyString(), any()))
                .thenReturn(new SemanticJudgeService.JudgeResult(1.0, "qwen:qwen3.6-plus",
                        "score: 1.0 reason: fluent answer"));
        QaReviewService service = new QaReviewService(semanticJudgeService);

        QaReviewResult result = service.review(new QaReviewRequest(
                "根据知识库说明项目架构",
                "这个项目使用了 Spring Boot、RAG 和权限控制。",
                List.of(),
                List.of()
        ));

        assertTrue(result.score() <= 0.60);
    }

    @Test
    void review_forSourcedAnswerWithoutCitation_shouldCapScore() {
        SemanticJudgeService semanticJudgeService = mock(SemanticJudgeService.class);
        when(semanticJudgeService.judge(anyString(), anyString(), anyString(), any()))
                .thenReturn(new SemanticJudgeService.JudgeResult(1.0, "qwen:qwen3.6-plus",
                        "score: 1.0 reason: correct answer"));
        QaReviewService service = new QaReviewService(semanticJudgeService);

        QaReviewResult result = service.review(new QaReviewRequest(
                "What is Java?",
                "Java is a programming language.",
                List.of(new QaReviewRequest.QaSource("Source", "Java is a programming language.", "1", "", 0)),
                List.of()
        ));

        assertTrue(result.score() <= 0.80);
    }
}
