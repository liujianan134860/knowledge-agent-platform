package com.liujianan.agentdemo.evaluation;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 回答质量检查服务。封装了 QaReviewController 中的审核逻辑，
 * 供 Controller 和 EvaluationService 共用。
 */
@Service
public class QaReviewService {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[\\d+\\]");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("[。！？.!?\\n]+");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]+|[a-zA-Z]{2,}|\\d{2,}");

    /**
     * 执行回答质量检查并返回结构化结果。
     */
    public QaReviewResult review(QaReviewRequest request) {
        String answer = request.answer() != null ? request.answer() : "";
        List<QaReviewRequest.QaSource> sources = request.sources();
        List<String> expectedKeywords = request.expectedKeywords() != null ? request.expectedKeywords() : List.of();

        // 1. 知识库命中
        boolean retrievalHit = sources != null && !sources.isEmpty();

        // 2. 引用标记
        boolean citationPresent = checkCitationPresence(answer);

        // 3. 关键词匹配
        List<String> matchedKeywords = expectedKeywords.stream()
                .filter(kw -> answer.toLowerCase().contains(kw.toLowerCase()))
                .toList();
        List<String> missingKeywords = expectedKeywords.stream()
                .filter(kw -> !answer.toLowerCase().contains(kw.toLowerCase()))
                .toList();
        boolean keywordMatch = expectedKeywords.isEmpty()
                || (double) matchedKeywords.size() / expectedKeywords.size() >= 0.5;

        // 4. 无来源断言
        boolean hasUnsupportedClaims = detectUnsupportedClaims(answer, sources);

        // 5. 综合评分
        double score = calculateScore(answer, retrievalHit, citationPresent, keywordMatch,
                expectedKeywords.size(), matchedKeywords.size(), hasUnsupportedClaims);

        // 6. 摘要
        String summary = buildSummary(retrievalHit, citationPresent, keywordMatch,
                expectedKeywords, matchedKeywords, missingKeywords, hasUnsupportedClaims, score);

        return new QaReviewResult(request.question(), retrievalHit, citationPresent,
                keywordMatch, matchedKeywords, missingKeywords,
                hasUnsupportedClaims, score, summary);
    }

    /**
     * 检查回答中是否包含 [数字] 格式的引用标记。
     */
    public boolean checkCitationPresence(String answer) {
        if (answer == null || answer.isBlank()) return false;
        return CITATION_PATTERN.matcher(answer).find();
    }

    /**
     * 检测回答中是否存在"无来源断言"——即句子的内容在知识库来源中找不到依据。
     * 兼容中英文混排文本。
     */
    public boolean detectUnsupportedClaims(String answer, List<QaReviewRequest.QaSource> sources) {
        if (answer == null || answer.isBlank()) return false;
        if (sources == null || sources.isEmpty()) return true;

        StringBuilder sourceBuilder = new StringBuilder();
        for (QaReviewRequest.QaSource s : sources) {
            if (s.title() != null) sourceBuilder.append(s.title()).append(" ");
            if (s.content() != null) sourceBuilder.append(s.content()).append(" ");
        }
        String sourceText = sourceBuilder.toString().toLowerCase();
        if (sourceText.isBlank()) return true;

        String[] sentences = SENTENCE_SPLIT.split(answer);
        int unsupportedCount = 0;

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() < 5) continue;
            if (CITATION_PATTERN.matcher(trimmed).find()) continue;
            if (trimmed.startsWith("##") || trimmed.startsWith("**") || trimmed.startsWith("```")
                    || trimmed.length() < 10) continue;

            List<String> tokens = extractMeaningfulTokens(trimmed);
            if (tokens.isEmpty()) continue;

            long matchCount = tokens.stream()
                    .filter(token -> sourceText.contains(token.toLowerCase()))
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
            tokens.add(matcher.group());
        }
        return tokens.stream()
                .filter(t -> t.length() > 1)
                .filter(t -> !isStopWord(t))
                .toList();
    }

    private boolean isStopWord(String token) {
        return switch (token.toLowerCase()) {
            case "the", "is", "are", "was", "were", "be", "been", "being",
                 "have", "has", "had", "do", "does", "did", "will", "would",
                 "shall", "should", "may", "might", "can", "could",
                 "this", "that", "these", "those", "it", "its", "they", "them",
                 "we", "you", "he", "she", "him", "her", "his", "my", "your",
                 "our", "their", "a", "an", "and", "or", "but", "if", "in",
                 "on", "at", "to", "for", "of", "with", "by", "from", "as",
                 "not", "no", "so", "than", "then", "also",
                 "very", "just", "about", "up", "out", "over", "after",
                 "before", "between",
                 "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一",
                 "一个", "可以", "这个", "那个", "什么", "怎么", "如何", "因为", "所以",
                 "但是", "而且", "或者", "如果", "虽然", "然而", "那么", "这样", "那样" -> true;
            default -> false;
        };
    }

    private double calculateScore(String answer, boolean retrievalHit, boolean citationPresent,
                                   boolean keywordMatch, int totalKeywords,
                                   int matchedKeywords, boolean hasUnsupportedClaims) {
        if (answer == null || answer.isBlank() || answer.length() < 5) {
            return 0.0;
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

        return Math.max(0.0, Math.min(1.0, Math.round(score * 100.0) / 100.0));
    }

    private String buildSummary(boolean retrievalHit, boolean citationPresent,
                                 boolean keywordMatch, List<String> expectedKeywords,
                                 List<String> matchedKeywords, List<String> missingKeywords,
                                 boolean hasUnsupportedClaims, double score) {
        List<String> items = new ArrayList<>();
        items.add(retrievalHit ? "✅ 命中知识库" : "❌ 未命中知识库");
        items.add(citationPresent ? "✅ 标注来源引用" : "❌ 缺少来源引用");
        if (!expectedKeywords.isEmpty()) {
            if (keywordMatch) {
                items.add("✅ 关键词覆盖 (" + matchedKeywords.size() + "/" + expectedKeywords.size() + ")");
            } else {
                items.add("⚠️ 关键词未达标: 命中 " + matchedKeywords.size() + "/" + expectedKeywords.size()
                        + "，缺失: " + String.join(", ", missingKeywords));
            }
        }
        items.add(hasUnsupportedClaims ? "⚠️ 存在无依据断言" : "✅ 内容有据可查");
        items.add("🏆 综合评分: " + String.format("%.0f", score * 100) + "/100");
        return String.join(" | ", items);
    }
}
