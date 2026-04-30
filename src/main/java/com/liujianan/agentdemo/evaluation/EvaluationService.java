package com.liujianan.agentdemo.evaluation;

import com.liujianan.agentdemo.common.HarnessMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class EvaluationService {
    private final EvaluationCaseRepository evaluationRepository;
    private final HarnessMetrics harnessMetrics;
    private final QaReviewService qaReviewService;

    // 中英文句子分隔符
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[\\d+\\]");

    public EvaluationService(EvaluationCaseRepository evaluationRepository,
                             HarnessMetrics harnessMetrics,
                             QaReviewService qaReviewService) {
        this.evaluationRepository = evaluationRepository;
        this.harnessMetrics = harnessMetrics;
        this.qaReviewService = qaReviewService;
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

    public RunEvaluationResponse run(Long caseId, String answer, String userId) {
        EvaluationCase evalCase = evaluationRepository.findById(caseId).orElse(null);
        if (evalCase == null) {
            throw new IllegalArgumentException("evaluation case not found: " + caseId);
        }

        // 复用 QaReviewService 的质量检查逻辑
        // 构造 QaReviewRequest（无 sources 时传入空列表）
        String answerText = answer != null ? answer : "";
        List<QaReviewRequest.QaSource> sources = answerText.isBlank() ? List.of() : List.of();
        List<String> expectedKeywords = evalCase.getExpectedKeywords() != null
                ? evalCase.getExpectedKeywords() : List.of();

        QaReviewRequest reviewRequest = new QaReviewRequest(
                evalCase.getQuestion(), answerText, sources, expectedKeywords
        );

        var reviewData = qaReviewService.review(reviewRequest);

        boolean retrievalHit = !answerText.isBlank();
        boolean citationPresent = checkCitationPresence(answerText);
        boolean keywordMatch = reviewData.answerContainsExpectedKeywords();
        boolean hasUnsupportedClaims = reviewData.hasUnsupportedClaims();
        double score = reviewData.score();
        String summary = reviewData.summary();

        // 记录评测指标
        harnessMetrics.recordEvaluationRun(score);

        return new RunEvaluationResponse(
                evalCase.getQuestion(), answerText, retrievalHit, citationPresent,
                keywordMatch, hasUnsupportedClaims, score, summary
        );
    }

    private boolean checkCitationPresence(String answer) {
        if (answer == null || answer.isBlank()) return false;
        return CITATION_PATTERN.matcher(answer).find();
    }
}
