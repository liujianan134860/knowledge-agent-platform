package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.knowledge.DocumentChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContextBuilder {
    private ContextBuilder() {
    }

    public static String buildSystemPrompt(List<DocumentChunk> sources) {
        if (sources == null || sources.isEmpty()) {
            return """
                    你是一个简洁、友好的中文问答助手。
                    当前没有检索到知识库片段时，也要正常回答用户的问题。
                    如果问题明显需要项目私有知识，请说明当前知识库没有命中，并给出通用建议。
                    回答使用 Markdown，结构清晰，不要输出乱码。
                    """;
        }
        return """
                你是一个知识库问答助手。
                请结合用户问题和给定知识片段生成回答，优先依据知识库内容，不要只复述原文。
                回答默认使用中文和 Markdown。
                关键结论后使用 [1]、[2] 这样的编号引用来源。
                如果知识片段不足以完整回答，请明确说明缺少哪些信息。
                """;
    }

    public static String buildUserMessage(String question, List<DocumentChunk> sources) {
        if (sources == null || sources.isEmpty()) {
            return "用户问题：\n" + question;
        }
        return "用户问题：\n" + question + "\n\n知识片段：\n" + buildContext(sources);
    }

    public static List<Map<String, String>> buildMessages(String question, List<DocumentChunk> sources,
                                                          List<SessionMessage> history) {
        String system = buildSystemPrompt(sources);
        String user = buildUserMessage(question, sources);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));

        if (history != null && history.size() > 1) {
            int end = history.size() - 1;
            int start = Math.max(0, end - 10);
            for (int i = start; i < end; i++) {
                SessionMessage msg = history.get(i);
                messages.add(Map.of("role", msg.role(), "content", msg.content()));
            }
        }
        messages.add(Map.of("role", "user", "content", user));

        return messages;
    }

    private static String buildContext(List<DocumentChunk> sources) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            DocumentChunk source = sources.get(i);
            context.append("[").append(i + 1).append("] ")
                    .append(source.title()).append(": ")
                    .append(source.content()).append("\n");
        }
        return context.toString();
    }
}
