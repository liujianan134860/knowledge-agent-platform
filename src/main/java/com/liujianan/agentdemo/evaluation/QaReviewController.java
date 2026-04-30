package com.liujianan.agentdemo.evaluation;

import com.liujianan.agentdemo.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/qa")
public class QaReviewController {

    @PostMapping("/review")
    public ApiResponse<QaReviewResult> review(@RequestBody QaReviewRequest request,
                                               HttpServletRequest httpRequest) {
        // 1. 是否命中知识库
        boolean retrievalHit = request.sources() != null && !request.sources().isEmpty();

        // 2. 是否引用来源 [1], [2]
        boolean citationPresent = checkCitationPresence(request.answer());

        // 3. 关键词检查
        List<String> expectedKeywords = request.expectedKeywords() != null ? request.expectedKeywords() : List.of();
        String answerText = request.answer() != null ? request.answer() : "";
        List<String> matchedKeywords = expectedKeywords.stream()
                .filter(kw -> answerText.toLowerCase().contains(kw.toLowerCase()))
                .toList();
        List<String> missingKeywords = expectedKeywords.stream()
                .filter(kw -> !answerText.toLowerCase().contains(kw.toLowerCase()))
                .toList();
        boolean answerContainsExpectedKeywords = expectedKeywords.isEmpty() || matchedKeywords.size() >= expectedKeywords.size() * 0.5;

        // 4. 无来源断言检查
        boolean hasUnsupportedClaims = detectUnsupportedClaims(request.answer(), request.sources());

        // 5. 评分
        double score = calculateScore(retrievalHit, citationPresent, answerContainsExpectedKeywords,
                expectedKeywords.size(), matchedKeywords.size(), hasUnsupportedClaims);

        String summary = buildSummary(retrievalHit, citationPresent, answerContainsExpectedKeywords,
                expectedKeywords, matchedKeywords, missingKeywords, hasUnsupportedClaims, score);

        return ApiResponse.ok(new QaReviewResult(request.question(), retrievalHit, citationPresent,
                answerContainsExpectedKeywords, matchedKeywords, missingKeywords,
                hasUnsupportedClaims, score, summary));
    }

    private boolean checkCitationPresence(String answer) {
        if (answer == null || answer.isBlank()) return false;
        Pattern pattern = Pattern.compile("\\[\\d+\\]");
        Matcher matcher = pattern.matcher(answer);
        return matcher.find();
    }

    private boolean detectUnsupportedClaims(String answer,
                                             List<QaReviewRequest.QaSource> sources) {
        if (answer == null || answer.isBlank()) return false;
        if (sources == null || sources.isEmpty()) return true;

        String allSourcesText = sources.stream()
                .map(s -> (s.title() != null ? s.title() : "")
                        + " " + (s.content() != null ? s.content() : ""))
                .collect(Collectors.joining(" "))
                .toLowerCase();
        if (allSourcesText.isBlank()) return true;

        String[] sentences = answer.split("[。！？.!?\\n]+");
        int unsupportedCount = 0;

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() < 8) continue;
            // If sentence contains citation marker, consider it supported
            if (trimmed.matches(".*\\[\\d+\\].*")) continue;
            // Skip template-like or short phrases
            if (trimmed.startsWith("##") || trimmed.startsWith("**") || trimmed.length() < 15) continue;

            String[] words = trimmed.split("\\s+");
            long significantWords = Arrays.stream(words)
                    .filter(w -> w.replaceAll("[\\[\\](){}]", "").length() > 2)
                    .count();
            if (significantWords == 0) continue;

            long overlap = Arrays.stream(words)
                    .filter(w -> {
                        String clean = w.replaceAll("[\\[\\](){}]", "");
                        return clean.length() > 2 && allSourcesText.contains(clean.toLowerCase());
                    })
                    .count();

            if ((double) overlap / significantWords < 0.25) {
                unsupportedCount++;
            }
        }
        return unsupportedCount > 1;
    }

    private double calculateScore(boolean retrievalHit, boolean citationPresent,
                                   boolean keywordMatch, int totalKeywords,
                                   int matchedKeywords, boolean hasUnsupportedClaims) {
        double score = 0.0;
        score += retrievalHit ? 0.3 : 0.0;
        score += citationPresent ? 0.3 : 0.0;
        if (totalKeywords > 0) {
            score += 0.4 * ((double) matchedKeywords / totalKeywords);
        } else {
            score += 0.4;
        }
        if (hasUnsupportedClaims) {
            score -= 0.2;
        }
        return Math.max(0.0, Math.min(1.0, Math.round(score * 100.0) / 100.0));
    }

    private String buildSummary(boolean retrievalHit, boolean citationPresent,
                                 boolean keywordMatch, List<String> expectedKeywords,
                                 List<String> matchedKeywords, List<String> missingKeywords,
                                 boolean hasUnsupportedClaims, double score) {
        List<String> items = new ArrayList<>();
        items.add(retrievalHit ? "✅ 命中知识库" : "❌ 未命中知识库");
        items.add(citationPresent ? "✅ 引用来源" : "❌ 未引用来源");
        if (!expectedKeywords.isEmpty()) {
            if (keywordMatch) {
                items.add("✅ 包含期望关键词");
            } else {
                items.add("⚠️ 部分关键词未命中: " + String.join(", ", missingKeywords));
            }
        }
        items.add(hasUnsupportedClaims ? "⚠️ 存在无来源断言" : "✅ 无无来源断言");
        items.add("🏆 总体评分: " + String.format("%.0f", score * 100) + "/100");
        return String.join(" | ", items);
    }
}
