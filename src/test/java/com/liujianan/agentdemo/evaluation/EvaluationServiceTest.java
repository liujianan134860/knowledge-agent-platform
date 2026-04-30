package com.liujianan.agentdemo.evaluation;

import com.liujianan.agentdemo.common.HarnessMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationCaseRepository evaluationRepository;

    private HarnessMetrics harnessMetrics;
    private EvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        harnessMetrics = new HarnessMetrics(new SimpleMeterRegistry());
        evaluationService = new EvaluationService(evaluationRepository, harnessMetrics);
    }

    @Test
    void add_shouldSaveAndReturnEvaluationCase() {
        CreateEvaluationRequest request = new CreateEvaluationRequest(
                "What is Java?",
                List.of("programming", "language"),
                "Java is a programming language."
        );

        EvaluationCase expectedCase = new EvaluationCase(
                1L, "What is Java?", List.of("programming", "language"),
                "Java is a programming language.", LocalDateTime.now(), "user1"
        );

        when(evaluationRepository.save(any(EvaluationCase.class))).thenReturn(expectedCase);

        EvaluationCase result = evaluationService.add(request, "user1");

        assertNotNull(result);
        assertEquals("What is Java?", result.getQuestion());
        verify(evaluationRepository).save(any(EvaluationCase.class));
    }

    @Test
    void list_shouldReturnUserCases() {
        when(evaluationRepository.findByUserIdOrderByCreatedAtDesc("user1"))
                .thenReturn(List.of(
                        new EvaluationCase(1L, "Q1", List.of("kw1"), "", LocalDateTime.now(), "user1")
                ));

        List<EvaluationCase> results = evaluationService.list("user1");

        assertEquals(1, results.size());
        assertEquals("Q1", results.get(0).getQuestion());
    }

    @Test
    void run_withFullMatch_shouldReturnHighScore() {
        EvaluationCase evalCase = new EvaluationCase(
                1L, "What is Java?",
                List.of("programming", "language", "object-oriented"),
                "Java is an object-oriented programming language.",
                LocalDateTime.now(), "user1"
        );

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evalCase));

        String answer = "Java is an object-oriented programming language. [1] " +
                "It is widely used for enterprise applications.";

        RunEvaluationResponse response = evaluationService.run(1L, answer, "user1");

        assertTrue(response.retrievalHit());
        assertTrue(response.citationPresent());
        assertTrue(response.keywordMatch());
        assertTrue(response.score() >= 0.6);

        // Verify metrics were recorded
        assertEquals(1.0, harnessMetrics.getEvaluationRunCount(), 0.001);
    }

    @Test
    void run_withNoMatch_shouldReturnLowScore() {
        EvaluationCase evalCase = new EvaluationCase(
                1L, "What is Python?",
                List.of("dynamic", "interpreted", "scripting"),
                "", LocalDateTime.now(), "user1"
        );

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evalCase));

        String answer = "I don't know about this topic.";

        RunEvaluationResponse response = evaluationService.run(1L, answer, "user1");

        assertTrue(response.retrievalHit());
        assertFalse(response.citationPresent());
        assertFalse(response.keywordMatch());
        assertTrue(response.score() < 0.6);

        assertEquals(1.0, harnessMetrics.getEvaluationRunCount(), 0.001);
    }

    @Test
    void run_withNonexistentCase_shouldThrowException() {
        when(evaluationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                evaluationService.run(999L, "answer", "user1"));
    }

    @Test
    void run_withOnlySomeKeywords_shouldReturnPartialScore() {
        EvaluationCase evalCase = new EvaluationCase(
                1L, "What is Spring?",
                List.of("framework", "dependency injection", "Java", "enterprise"),
                "", LocalDateTime.now(), "user1"
        );

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evalCase));

        String answer = "Spring is a Java framework. [1]";

        RunEvaluationResponse response = evaluationService.run(1L, answer, "user1");

        assertTrue(response.retrievalHit());
        assertTrue(response.citationPresent());
        assertTrue(response.keywordMatch());
        // Should have matched: "Java", "framework" - at least 2 out of 4
        assertTrue(response.score() >= 0.3);
    }

    @Test
    void run_withEmptyExpectedKeywords_shouldHaveBaseScore() {
        EvaluationCase evalCase = new EvaluationCase(
                1L, "Tell me about Java",
                List.of(), "", LocalDateTime.now(), "user1"
        );

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evalCase));

        String answer = "Here is what I know about Java. [1]";

        RunEvaluationResponse response = evaluationService.run(1L, answer, "user1");

        // Base score: retrievalHit(0.3) + citationPresent(0.3) + 0.4 = 1.0
        assertEquals(1.0, response.score(), 0.01);
    }
}
