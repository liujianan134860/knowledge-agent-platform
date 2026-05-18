package com.liujianan.agentdemo.evaluation;

import com.liujianan.agentdemo.audit.AuditService;
import com.liujianan.agentdemo.common.AiPlatformProperties;
import com.liujianan.agentdemo.common.HarnessMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class EvaluationService {
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[\\d+\\]");

    private final EvaluationCaseRepository evaluationRepository;
    private final EvaluationRunRepository evaluationRunRepository;
    private final HarnessMetrics harnessMetrics;
    private final QaReviewService qaReviewService;
    private final SemanticJudgeService semanticJudgeService;
    private final AiPlatformProperties aiPlatformProperties;
    private final AuditService auditService;

    public EvaluationService(EvaluationCaseRepository evaluationRepository,
                             EvaluationRunRepository evaluationRunRepository,
                             HarnessMetrics harnessMetrics,
                             QaReviewService qaReviewService,
                             SemanticJudgeService semanticJudgeService,
                             AiPlatformProperties aiPlatformProperties,
                             AuditService auditService) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationRunRepository = evaluationRunRepository;
        this.harnessMetrics = harnessMetrics;
        this.qaReviewService = qaReviewService;
        this.semanticJudgeService = semanticJudgeService;
        this.aiPlatformProperties = aiPlatformProperties;
        this.auditService = auditService;
    }

    @Transactional
    public EvaluationCase add(CreateEvaluationRequest request, String userId) {
        EvaluationCase evaluation = new EvaluationCase(
                null,
                request.question(),
                request.expectedKeywords() == null ? List.of() : request.expectedKeywords(),
                request.feedback(),
                LocalDateTime.now(),
                userId
        );
        return evaluationRepository.save(evaluation);
    }

    public List<EvaluationCase> list(String userId) {
        return evaluationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<EvaluationCase> list(String userId, Pageable pageable) {
        return evaluationRepository.findByUserId(userId, pageable);
    }

    @Transactional
    public RunEvaluationResponse run(Long caseId, String answer, String userId) {
        EvaluationCase evalCase = evaluationRepository.findById(caseId).orElse(null);
        if (evalCase == null || !userId.equals(evalCase.getUserId())) {
            throw new IllegalArgumentException("evaluation case not found: " + caseId);
        }

        String answerText = answer != null ? answer : "";
        List<String> expectedKeywords = evalCase.getExpectedKeywords() != null
                ? evalCase.getExpectedKeywords() : List.of();

        QaReviewRequest reviewRequest = new QaReviewRequest(
                evalCase.getQuestion(), answerText, List.of(), expectedKeywords
        );
        var reviewData = qaReviewService.review(reviewRequest);

        boolean retrievalHit = !answerText.isBlank();
        boolean citationPresent = checkCitationPresence(answerText);
        boolean keywordMatch = reviewData.answerContainsExpectedKeywords();
        boolean hasUnsupportedClaims = reviewData.hasUnsupportedClaims();
        SemanticJudgeService.JudgeResult judge = semanticJudgeService.judge(evalCase.getQuestion(), answerText, expectedKeywords);
        double score = Math.max(reviewData.score(), Math.round((reviewData.score() * 0.6 + judge.score() * 0.4) * 100.0) / 100.0);
        String summary = reviewData.summary() + " | semanticJudge=" + judge.mode() + ", score=" + judge.score();

        harnessMetrics.recordEvaluationRun(score);
        EvaluationRun saved = evaluationRunRepository.save(new EvaluationRun(null, caseId, userId, answerText,
                retrievalHit, citationPresent, keywordMatch, hasUnsupportedClaims, score,
                aiPlatformProperties.getPromptVersion(), aiPlatformProperties.getJudgeModel(),
                aiPlatformProperties.getRetrievalVersion(), summary, LocalDateTime.now()));
        auditService.record(userId, "EVALUATION_RUN", "EVALUATION_CASE", String.valueOf(caseId),
                java.util.Map.of("runId", saved.getId() == null ? -1L : saved.getId(), "score", score));

        return new RunEvaluationResponse(
                evalCase.getQuestion(), answerText, retrievalHit, citationPresent,
                keywordMatch, hasUnsupportedClaims, score, summary
        );
    }

    public Page<EvaluationRun> listRuns(String userId, Pageable pageable) {
        return evaluationRunRepository.findByUserId(userId, pageable);
    }

    private boolean checkCitationPresence(String answer) {
        if (answer == null || answer.isBlank()) return false;
        return CITATION_PATTERN.matcher(answer).find();
    }
}
