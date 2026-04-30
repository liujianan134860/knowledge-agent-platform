package com.liujianan.agentdemo.evaluation;

import com.liujianan.agentdemo.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QaReviewControllerTest {

    @Mock
    private QaReviewService qaReviewService;

    @InjectMocks
    private QaReviewController controller;

    @Test
    void review_withHitAndCitations_shouldReturnHighScore() {
        QaReviewRequest request = new QaReviewRequest(
                "What is Java?",
                "Java is a programming language [1]. It is object-oriented [2].",
                List.of(
                        new QaReviewRequest.QaSource("Source1", "Java is a programming language", "1", "", 0),
                        new QaReviewRequest.QaSource("Source2", "Java is object-oriented", "2", "", 0)
                ),
                List.of("programming", "Java")
        );

        QaReviewResult expected = new QaReviewResult(
                "What is Java?", true, true, true,
                List.of("programming", "Java"), List.of(),
                false, 0.9, "✅ 命中知识库 | ✅ 标注来源引用 | ✅ 关键词覆盖 (2/2) | ✅ 内容有据可查 | 🏆 综合评分: 90/100"
        );
        when(qaReviewService.review(request)).thenReturn(expected);

        ApiResponse<QaReviewResult> response = controller.review(request);

        assertTrue(response.success());
        QaReviewResult result = response.data();
        assertTrue(result.retrievalHit());
        assertTrue(result.citationPresent());
        assertTrue(result.score() >= 0.6);
    }

    @Test
    void review_withNoSources_shouldReturnLowScore() {
        QaReviewRequest request = new QaReviewRequest(
                "What is Java?",
                "I think Java is something about coffee.",
                List.of(),
                List.of("programming", "language")
        );

        QaReviewResult expected = new QaReviewResult(
                "What is Java?", false, false, false,
                List.of(), List.of("programming", "language"),
                true, 0.0, "❌ 未命中知识库 | ❌ 缺少来源引用 | ⚠️ 关键词未达标: 命中 0/2，缺失: programming, language | ⚠️ 存在无依据断言 | 🏆 综合评分: 0/100"
        );
        when(qaReviewService.review(request)).thenReturn(expected);

        ApiResponse<QaReviewResult> response = controller.review(request);

        assertTrue(response.success());
        QaReviewResult result = response.data();
        assertFalse(result.retrievalHit());
        assertFalse(result.citationPresent());
        assertFalse(result.answerContainsExpectedKeywords());
        assertTrue(result.score() < 0.6);
    }

    @Test
    void review_withEmptyAnswer_shouldReturnZeroScore() {
        // QaReviewService returns 0.0 for empty/blank answers
        QaReviewRequest request = new QaReviewRequest(
                "What is Java?",
                "",
                List.of(),
                List.of()
        );

        QaReviewResult expected = new QaReviewResult(
                "What is Java?", false, false, true,
                List.of(), List.of(),
                false, 0.0, "❌ 未命中知识库 | ❌ 缺少来源引用 | ✅ 内容有据可查 | 🏆 综合评分: 0/100"
        );
        when(qaReviewService.review(request)).thenReturn(expected);

        ApiResponse<QaReviewResult> response = controller.review(request);

        assertTrue(response.success());
        QaReviewResult result = response.data();
        assertEquals(0.0, result.score(), 0.01);
    }

    @Test
    void review_withUnsupportedClaims_shouldDeductScore() {
        QaReviewRequest request = new QaReviewRequest(
                "What is quantum computing?",
                "Quantum computing uses qubits. It will revolutionize everything. " +
                        "Many companies are working on it. The speed is incredible.",
                List.of(
                        new QaReviewRequest.QaSource("Source", "Quantum computing uses qubits", "1", "", 0)
                ),
                List.of("quantum")
        );

        QaReviewResult expected = new QaReviewResult(
                "What is quantum computing?", true, false, true,
                List.of("quantum"), List.of(),
                true, 0.4, "✅ 命中知识库 | ❌ 缺少来源引用 | ✅ 关键词覆盖 (1/1) | ⚠️ 存在无依据断言 | 🏆 综合评分: 40/100"
        );
        when(qaReviewService.review(request)).thenReturn(expected);

        ApiResponse<QaReviewResult> response = controller.review(request);

        assertTrue(response.success());
        QaReviewResult result = response.data();
        assertTrue(result.hasUnsupportedClaims());
    }

    @Test
    void review_withMatchingKeywords_shouldIndicateKeywordMatch() {
        QaReviewRequest request = new QaReviewRequest(
                "What is Spring?",
                "Spring is a Java framework [1] for enterprise applications.",
                List.of(
                        new QaReviewRequest.QaSource("Source", "Spring is a Java framework", "1", "", 0)
                ),
                List.of("Spring", "Java", "framework")
        );

        QaReviewResult expected = new QaReviewResult(
                "What is Spring?", true, true, true,
                List.of("Spring", "Java", "framework"), List.of(),
                false, 0.9, "✅ 命中知识库 | ✅ 标注来源引用 | ✅ 关键词覆盖 (3/3) | ✅ 内容有据可查 | 🏆 综合评分: 90/100"
        );
        when(qaReviewService.review(request)).thenReturn(expected);

        ApiResponse<QaReviewResult> response = controller.review(request);

        assertTrue(response.success());
        QaReviewResult result = response.data();
        assertTrue(result.answerContainsExpectedKeywords());
        assertEquals(3, result.matchedKeywords().size());
        assertTrue(result.missingKeywords().isEmpty());
    }
}
