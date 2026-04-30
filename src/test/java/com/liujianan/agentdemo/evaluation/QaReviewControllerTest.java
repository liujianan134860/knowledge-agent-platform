package com.liujianan.agentdemo.evaluation;

import com.liujianan.agentdemo.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QaReviewControllerTest {

    private final QaReviewController controller = new QaReviewController();

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

        ApiResponse<QaReviewResult> response = controller.review(request, new MockHttpServletRequest());

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

        ApiResponse<QaReviewResult> response = controller.review(request, new MockHttpServletRequest());

        assertTrue(response.success());
        QaReviewResult result = response.data();
        assertFalse(result.retrievalHit());
        assertFalse(result.citationPresent());
        assertFalse(result.answerContainsExpectedKeywords());
        assertTrue(result.score() < 0.6);
    }

    @Test
    void review_withEmptyAnswer_shouldReturnBaseScore() {
        // With empty answer, no sources, and no expected keywords:
        // - retrievalHit: false → 0.0
        // - citationPresent: false → 0.0
        // - expectedKeywords empty → 0.4
        // - hasUnsupportedClaims: false → no deduction
        // Score = 0.4
        QaReviewRequest request = new QaReviewRequest(
                "What is Java?",
                "",
                List.of(),
                List.of()
        );

        ApiResponse<QaReviewResult> response = controller.review(request, new MockHttpServletRequest());

        assertTrue(response.success());
        QaReviewResult result = response.data();
        assertEquals(0.4, result.score(), 0.01);
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

        ApiResponse<QaReviewResult> response = controller.review(request, new MockHttpServletRequest());

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

        ApiResponse<QaReviewResult> response = controller.review(request, new MockHttpServletRequest());

        assertTrue(response.success());
        QaReviewResult result = response.data();
        assertTrue(result.answerContainsExpectedKeywords());
        assertEquals(3, result.matchedKeywords().size());
        assertTrue(result.missingKeywords().isEmpty());
    }
}
