package com.liujianan.agentdemo.evaluation;

import com.liujianan.agentdemo.common.AiPlatformProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SemanticJudgeService {
    private static final Pattern SCORE_PATTERN = Pattern.compile("(?:score|分数)\\s*[:：]\\s*(0(?:\\.\\d+)?|1(?:\\.0+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]|[a-zA-Z0-9]{2,}");

    private final ObjectProvider<JudgeModelClient> judgeModelClientProvider;
    private final AiPlatformProperties properties;

    public SemanticJudgeService(ObjectProvider<JudgeModelClient> judgeModelClientProvider, AiPlatformProperties properties) {
        this.judgeModelClientProvider = judgeModelClientProvider;
        this.properties = properties;
    }

    public JudgeResult judge(String question, String answer, List<String> expectedKeywords) {
        return judge(question, answer, "", expectedKeywords);
    }

    public JudgeResult judge(String question, String answer, String sourcesText, List<String> expectedKeywords) {
        JudgeModelClient judgeModelClient = judgeModelClientProvider.getIfAvailable();
        if (properties.isLlmJudgeEnabled() && judgeModelClient != null && judgeModelClient.isConfigured()) {
            try {
                String prompt = """
                        You are a strict bilingual LLM-as-judge evaluator for a RAG/chat answer.
                        Return only:
                        score: <number from 0 to 1>
                        reason: <short reason>

                        Question: %s
                        Expected keywords: %s
                        Retrieved sources:
                        %s

                        Answer: %s

                        Rubric:
                        - 0.90-1.00: fully correct, directly answers the question, complete, and source-grounded when sources are provided.
                        - 0.70-0.89: mostly correct but minor omissions, weak wording, or minor citation issues.
                        - 0.40-0.69: partially correct, incomplete, weakly grounded, or missing important details.
                        - 0.10-0.39: mostly wrong, unsupported, irrelevant, or contradicts sources.
                        - 0.00: empty, refusal without reason, or completely unrelated.

                        Rules:
                        - If retrieved sources are provided and the answer makes source-based claims without citations, do not score above 0.80.
                        - If retrieved sources are provided and the answer contradicts them, do not score above 0.30.
                        - If no retrieved sources are provided but the question clearly asks about documents, knowledge base, project-specific details, citations, or RAG grounding, do not score above 0.60.
                        - If the question is only greeting/chat/pinyin and no sources are provided, citations are not required.
                        - Chinese text is valid content; never treat Chinese as gibberish.
                        """.formatted(question, expectedKeywords, sourcesText == null || sourcesText.isBlank() ? "(none)" : sourcesText, answer);
                String result = judgeModelClient.judge(prompt);
                Matcher matcher = SCORE_PATTERN.matcher(result == null ? "" : result);
                if (matcher.find()) {
                    double score = clamp(Double.parseDouble(matcher.group(1)));
                    return new JudgeResult(score, properties.getJudgeProvider() + ":" + judgeModelClient.modelName(), result.strip());
                }
            } catch (RuntimeException ignored) {
                // Fall back to deterministic semantic overlap so evaluation remains available offline.
            }
        }
        return fallbackJudge(answer, expectedKeywords);
    }

    private JudgeResult fallbackJudge(String answer, List<String> expectedKeywords) {
        if (answer == null || answer.isBlank()) {
            return new JudgeResult(0.0, "fallback", "empty answer");
        }
        Set<String> answerTokens = new HashSet<>(tokens(answer));
        if (answerTokens.isEmpty()) {
            return new JudgeResult(0.0, "fallback", "no meaningful tokens");
        }
        List<String> expectedTokens = expectedKeywords == null ? List.of() : expectedKeywords.stream()
                .flatMap(keyword -> tokens(keyword).stream())
                .toList();
        if (expectedTokens.isEmpty()) {
            return new JudgeResult(0.5, "fallback", "no expected keywords configured");
        }
        long matched = expectedTokens.stream().filter(answerTokens::contains).count();
        double score = clamp((double) matched / expectedTokens.size());
        return new JudgeResult(score, "fallback", "semantic keyword overlap " + matched + "/" + expectedTokens.size());
    }

    private List<String> tokens(String text) {
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        java.util.ArrayList<String> tokens = new java.util.ArrayList<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private double clamp(double score) {
        return Math.max(0.0, Math.min(1.0, Math.round(score * 100.0) / 100.0));
    }

    public record JudgeResult(double score, String mode, String reason) {
    }
}
