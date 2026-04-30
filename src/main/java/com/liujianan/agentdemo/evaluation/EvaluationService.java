package com.liujianan.agentdemo.evaluation;

import com.liujianan.agentdemo.common.HarnessMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EvaluationService {
    private final EvaluationCaseRepository evaluationRepository;
    private final HarnessMetrics harnessMetrics;

    public EvaluationService(EvaluationCaseRepository evaluationRepository, HarnessMetrics harnessMetrics) {
        this.evaluationRepository = evaluationRepository;
        this.harnessMetrics = harnessMetrics;
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

        boolean retrievalHit = answer != null && !answer.isBlank();
        boolean citationPresent = checkCitationPresence(answer);
        List<String> expected = evalCase.getExpectedKeywords();
        List<String> answerKeywords = expected.stream()
                .filter(kw -> answer != null && answer.toLowerCase().contains(kw.toLowerCase()))
                .toList();
        boolean keywordMatch = !expected.isEmpty() && !answerKeywords.isEmpty();

        double score = 0.0;
        score += retrievalHit ? 0.3 : 0.0;
        score += citationPresent ? 0.3 : 0.0;
        if (!expected.isEmpty()) {
            score += 0.4 * ((double) answerKeywords.size() / expected.size());
        } else {
            score += 0.4;
        }
        score = Math.max(0.0, Math.min(1.0, Math.round(score * 100.0) / 100.0));

        // Record evaluation metrics
        harnessMetrics.recordEvaluationRun(score);

        return new RunEvaluationResponse(evalCase.getQuestion(), answer, retrievalHit, citationPresent, keywordMatch, score);
    }

    private boolean checkCitationPresence(String answer) {
        if (answer == null || answer.isBlank()) return false;
        Pattern pattern = Pattern.compile("\\[\\d+\\]");
        Matcher matcher = pattern.matcher(answer);
        return matcher.find();
    }
}
