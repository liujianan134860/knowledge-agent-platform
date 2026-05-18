package com.liujianan.agentdemo.evaluation;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QaReviewService {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[\\d+]");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("[。！？.!?\\n]+");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]+|[a-zA-Z]{2,}|\\d{2,}");

    private final SemanticJudgeService semanticJudgeService;

    public QaReviewService(SemanticJudgeService semanticJudgeService) {
        this.semanticJudgeService = semanticJudgeService;
    }

    public QaReviewResult review(QaReviewRequest request) {
        String answer = request.answer() != null ? request.answer() : "";
        List<QaReviewRequest.QaSource> sources = request.sources() != null ? request.sources() : List.of();
        List<String> expectedKeywords = request.expectedKeywords() != null ? request.expectedKeywords() : List.of();
        boolean ragExpected = shouldExpectRagGrounding(request.question(), sources, expectedKeywords);

        boolean retrievalHit = !sources.isEmpty();
        boolean citationPresent = checkCitationPresence(answer);

        List<String> matchedKeywords = expectedKeywords.stream()
                .filter(kw -> answer.toLowerCase(Locale.ROOT).contains(kw.toLowerCase(Locale.ROOT)))
                .toList();
        List<String> missingKeywords = expectedKeywords.stream()
                .filter(kw -> !answer.toLowerCase(Locale.ROOT).contains(kw.toLowerCase(Locale.ROOT)))
                .toList();
        boolean keywordMatch = expectedKeywords.isEmpty()
                || (double) matchedKeywords.size() / expectedKeywords.size() >= 0.5;

        boolean hasUnsupportedClaims = ragExpected && detectUnsupportedClaims(answer, sources);
        double ruleScore = calculateRuleScore(answer, retrievalHit, citationPresent, keywordMatch,
                ragExpected, expectedKeywords.size(), matchedKeywords.size(), hasUnsupportedClaims);

        SemanticJudgeService.JudgeResult judge = semanticJudgeService.judge(
                request.question(), answer, buildSourcesText(sources), expectedKeywords);
        double score = finalScore(judge.score(), ruleScore, ragExpected,
                retrievalHit, citationPresent, hasUnsupportedClaims);

        String summary = buildSummary(retrievalHit, citationPresent, keywordMatch,
                expectedKeywords, matchedKeywords, missingKeywords, hasUnsupportedClaims,
                ragExpected, score, ruleScore, judge);

        return new QaReviewResult(request.question(), retrievalHit, citationPresent,
                keywordMatch, matchedKeywords, missingKeywords,
                hasUnsupportedClaims, score, summary);
    }

    public boolean checkCitationPresence(String answer) {
        return answer != null && !answer.isBlank() && CITATION_PATTERN.matcher(answer).find();
    }

    public boolean detectUnsupportedClaims(String answer, List<QaReviewRequest.QaSource> sources) {
        if (answer == null || answer.isBlank()) return false;
        if (sources == null || sources.isEmpty()) return true;

        StringBuilder sourceBuilder = new StringBuilder();
        for (QaReviewRequest.QaSource source : sources) {
            if (source.title() != null) sourceBuilder.append(source.title()).append(" ");
            if (source.content() != null) sourceBuilder.append(source.content()).append(" ");
        }
        String sourceText = sourceBuilder.toString().toLowerCase(Locale.ROOT);
        if (sourceText.isBlank()) return true;

        String[] sentences = SENTENCE_SPLIT.split(answer);
        int unsupportedCount = 0;
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() < 10) continue;
            if (CITATION_PATTERN.matcher(trimmed).find()) continue;
            if (trimmed.startsWith("##") || trimmed.startsWith("**") || trimmed.startsWith("```")) continue;

            List<String> tokens = extractMeaningfulTokens(trimmed);
            if (tokens.isEmpty()) continue;

            long matchCount = tokens.stream()
                    .filter(token -> sourceText.contains(token.toLowerCase(Locale.ROOT)))
                    .count();
            double matchRate = (double) matchCount / tokens.size();
            if (matchRate < 0.2) {
                unsupportedCount++;
            }
        }
        return unsupportedCount >= 2;
    }

    private List<String> extractMeaningfulTokens(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() > 1 && !isStopWord(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private boolean isStopWord(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "the", "is", "are", "was", "were", "be", "been", "being",
                 "have", "has", "had", "do", "does", "did", "will", "would",
                 "shall", "should", "may", "might", "can", "could",
                 "this", "that", "these", "those", "it", "its", "they", "them",
                 "we", "you", "he", "she", "him", "her", "his", "my", "your",
                 "our", "their", "a", "an", "and", "or", "but", "if", "in",
                 "on", "at", "to", "for", "of", "with", "by", "from", "as",
                 "not", "no", "so", "than", "then", "also",
                 "very", "just", "about", "up", "out", "over", "after", "before", "between",
                 "的", "了", "在", "是", "和", "或", "与", "及", "可以", "这个", "那个",
                 "什么", "怎么", "如何", "因为", "所以", "但是", "而且", "如果", "那么" -> true;
            default -> false;
        };
    }

    private double calculateRuleScore(String answer, boolean retrievalHit, boolean citationPresent,
                                      boolean keywordMatch, boolean ragExpected, int totalKeywords,
                                      int matchedKeywords, boolean hasUnsupportedClaims) {
        if (answer == null || answer.isBlank() || answer.length() < 5) {
            return 0.0;
        }
        if (!ragExpected) {
            double score = 0.70;
            if (keywordMatch) score += 0.15;
            if (!hasUnsupportedClaims) score += 0.15;
            return clamp(score);
        }
        double score = 0.0;
        score += retrievalHit ? 0.30 : 0.0;
        score += citationPresent ? 0.30 : 0.0;
        if (totalKeywords > 0) {
            score += 0.30 * ((double) matchedKeywords / totalKeywords);
        } else {
            score += 0.15;
        }
        if (hasUnsupportedClaims) {
            score -= 0.20;
        }
        return clamp(score);
    }

    private String buildSummary(boolean retrievalHit, boolean citationPresent,
                                boolean keywordMatch, List<String> expectedKeywords,
                                List<String> matchedKeywords, List<String> missingKeywords,
                                boolean hasUnsupportedClaims, boolean ragExpected,
                                double score, double ruleScore,
                                SemanticJudgeService.JudgeResult judge) {
        List<String> items = new ArrayList<>();
        items.add("LLM-as-judge: " + judge.mode());
        items.add("Judge score: " + String.format("%.0f", score * 100) + "/100");
        items.add("Rule reference score: " + String.format("%.0f", ruleScore * 100) + "/100");
        items.add(ragExpected ? "RAG grounding expected" : "General/chat answer; source citation not required");
        if (ragExpected) {
            items.add(retrievalHit ? "Retrieved sources found" : "No knowledge source retrieved");
            items.add(citationPresent ? "Source citation present" : "Source citation missing");
        }
        if (!expectedKeywords.isEmpty()) {
            items.add(keywordMatch
                    ? "Keyword coverage " + matchedKeywords.size() + "/" + expectedKeywords.size()
                    : "Missing keywords: " + String.join(", ", missingKeywords));
        }
        items.add(hasUnsupportedClaims ? "Possible unsupported claims" : "No obvious unsupported claims in rule check");
        items.add("Reason: " + judge.reason());
        return String.join(" | ", items);
    }

    private double finalScore(double judgeScore, double ruleScore, boolean ragExpected,
                              boolean retrievalHit, boolean citationPresent,
                              boolean hasUnsupportedClaims) {
        if (!ragExpected) {
            return clamp(judgeScore);
        }
        double score = judgeScore * 0.60 + ruleScore * 0.40;
        if (!retrievalHit) {
            score = Math.min(score, 0.60);
        }
        if (retrievalHit && !citationPresent) {
            score = Math.min(score, 0.80);
        }
        if (hasUnsupportedClaims) {
            score = Math.min(score, 0.70);
        }
        return clamp(score);
    }

    private boolean shouldExpectRagGrounding(String question, List<QaReviewRequest.QaSource> sources,
                                             List<String> expectedKeywords) {
        if (sources != null && !sources.isEmpty()) {
            return true;
        }
        if (expectedKeywords != null && !expectedKeywords.isEmpty()) {
            return true;
        }
        if (question == null || question.isBlank()) {
            return false;
        }
        String normalized = question.trim().toLowerCase(Locale.ROOT);
        if (isConversationalQuestion(normalized)) {
            return false;
        }
        return normalized.contains("根据")
                || normalized.contains("知识库")
                || normalized.contains("文档")
                || normalized.contains("资料")
                || normalized.contains("项目")
                || normalized.contains("引用")
                || normalized.contains("来源")
                || normalized.contains("rag")
                || normalized.contains("document")
                || normalized.contains("source")
                || normalized.contains("according to");
    }

    private boolean isConversationalQuestion(String normalized) {
        return normalized.matches("^(hi|hello|hey|你好|您好|在吗|嗨|哈喽|ni\\s*hao|nihao|ninhao)[。.!！?？\\s]*$")
                || normalized.contains("拼音")
                || normalized.contains("pinyin")
                || normalized.contains("打招呼")
                || normalized.contains("问候");
    }

    private String buildSourcesText(List<QaReviewRequest.QaSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            QaReviewRequest.QaSource source = sources.get(i);
            builder.append("[").append(i + 1).append("] ")
                    .append(source.title() == null ? "" : source.title())
                    .append(": ")
                    .append(source.content() == null ? "" : source.content())
                    .append("\n");
        }
        return builder.toString();
    }

    private double clamp(double score) {
        return Math.max(0.0, Math.min(1.0, Math.round(score * 100.0) / 100.0));
    }
}
