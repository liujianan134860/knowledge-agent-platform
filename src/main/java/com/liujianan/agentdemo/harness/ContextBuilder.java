package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.knowledge.DocumentChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContextBuilder {
    private ContextBuilder() {
    }

    public static String buildSystemPrompt(List<DocumentChunk> sources) {
        return sources.isEmpty()
                ? "你是一个简洁、友好的中文问答助手。当前没有检索到知识库片段时，可以先自然回应用户；如果用户提出专业问题，请说明尚未命中知识库，并给出通用建议。回答使用 Markdown 格式，适当使用标题、列表、加粗等排版，使内容层次分明、易于阅读。"
                : "你是一个知识库问答助手。请结合用户问题和给定知识片段生成回答，优先依据知识库内容，不要只复述原文。回答默认使用中文，使用 Markdown 格式排版：\n- 用 ### 标题分段\n- 用 - 或 1. 创建列表\n- 用 **加粗** 突出重点\n- 用 > 引用原文\n- 关键结论后用 [1]、[2] 引用来源编号\n- 如有代码示例用 `` ` `` 包裹\n如果知识片段不足以完整回答，请明确说明缺少的信息。";
    }

    public static String buildUserMessage(String question, List<DocumentChunk> sources) {
        if (sources.isEmpty()) {
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
