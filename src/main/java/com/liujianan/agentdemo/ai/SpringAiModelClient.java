package com.liujianan.agentdemo.ai;

import com.liujianan.agentdemo.harness.ContextBuilder;
import com.liujianan.agentdemo.harness.SessionMessage;
import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.llm.ModelClient;
import org.springframework.ai.chat.client.ChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Spring AI-based model client that uses ChatModel for LLM inference.
 * Implements the existing ModelClient interface for backward compatibility.
 * Only activated when a ChatModel bean is available (Spring AI OpenAI configured).
 */
@Service
@Primary
public class SpringAiModelClient implements ModelClient {
    private static final Logger log = LoggerFactory.getLogger(SpringAiModelClient.class);

    private final ChatClient chatClient;
    private final List<ToolCallbackProvider> toolCallbackProviders;

    public SpringAiModelClient(ChatModel chatModel) {
        this(chatModel, List.of());
    }

    public SpringAiModelClient(ChatModel chatModel, List<ToolCallbackProvider> toolCallbackProviders) {
        this(chatModel == null ? null : ChatClient.builder(chatModel).build(), toolCallbackProviders);
        log.info("SpringAiModelClient initialized with ChatModel: {}", chatModel == null ? "none" : chatModel.getClass().getSimpleName());
    }

    private final DeepSeekReasoningAdvisor reasoningAdvisor = new DeepSeekReasoningAdvisor();

    @Autowired
    public SpringAiModelClient(ObjectProvider<ChatModel> chatModelProvider,
                               ObjectProvider<ToolCallbackProvider> toolCallbackProvider) {
        this(chatModelProvider.getIfAvailable(),
                toolCallbackProvider.orderedStream().toList());
    }

    private SpringAiModelClient(ChatClient chatClient, List<ToolCallbackProvider> toolCallbackProviders) {
        this.chatClient = chatClient;
        this.toolCallbackProviders = toolCallbackProviders == null ? List.of() : toolCallbackProviders;
        log.info("SpringAiModelClient loaded {} tool callback provider(s)", this.toolCallbackProviders.size());
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String answer(String question, List<DocumentChunk> sources, List<SessionMessage> history) {
        if (chatClient == null) {
            return fallback(question, sources) + "\n\n[Spring AI ChatModel is not configured]";
        }
        try {
            List<Message> messages = buildMessages(question, sources, history);
            String result = chatClient.prompt()
                    .messages(messages)
                    .advisors(reasoningAdvisor)
                    .toolCallbacks(toolCallbackProviders.toArray(ToolCallbackProvider[]::new))
                    .call()
                    .content();
            return result != null ? result : "";
        } catch (Exception e) {
            log.error("Spring AI chat call failed", e);
            return fallback(question, sources) + "\n\n[Spring AI call failed: " + e.getMessage() + "]";
        }
    }

    @Override
    public void answerStream(String question, List<DocumentChunk> sources, List<SessionMessage> history,
                             Consumer<String> onDelta, Consumer<String> onDone, Consumer<String> onError) {
        if (chatClient == null) {
            String text = fallback(question, sources) + "\n\n[Spring AI ChatModel is not configured]";
            streamText(text, onDelta);
            onDone.accept(text);
            return;
        }
        try {
            List<Message> messages = buildMessages(question, sources, history);
            Flux<String> stream = chatClient.prompt()
                    .messages(messages)
                    .advisors(reasoningAdvisor)
                    .toolCallbacks(toolCallbackProviders.toArray(ToolCallbackProvider[]::new))
                    .stream()
                    .content();
            AtomicReference<StringBuilder> fullBuilder = new AtomicReference<>(new StringBuilder());

            stream.subscribe(
                    token -> {
                        if (token != null && !token.isEmpty()) {
                            fullBuilder.get().append(token);
                            onDelta.accept(token);
                        }
                    },
                    error -> {
                        log.error("Spring AI stream error", error);
                        streamFallback(question, sources, onDelta, onDone);
                    },
                    () -> onDone.accept(fullBuilder.get().toString())
            );
        } catch (Exception e) {
            log.error("Spring AI stream call failed", e);
            String text = fallback(question, sources);
            streamText(text, onDelta);
            onDone.accept(text);
        }
    }

    private void streamFallback(String question, List<DocumentChunk> sources,
                                Consumer<String> onDelta, Consumer<String> onDone) {
        String text = fallback(question, sources);
        streamText(text, onDelta);
        onDone.accept(text);
    }

    private void streamText(String text, Consumer<String> onDelta) {
        for (int i = 0; i < text.length(); i += 3) {
            int end = Math.min(i + 3, text.length());
            onDelta.accept(text.substring(i, end));
            try {
                Thread.sleep(30);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private List<Message> buildMessages(String question, List<DocumentChunk> sources, List<SessionMessage> history) {
        String systemPrompt = ContextBuilder.buildSystemPrompt(sources);

        List<Message> messages = new ArrayList<>();
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));

        // Add conversation history
        if (history != null && history.size() > 1) {
            int end = history.size() - 1;
            int start = Math.max(0, end - 10);
            for (int i = start; i < end; i++) {
                SessionMessage msg = history.get(i);
                if ("user".equals(msg.role())) {
                    messages.add(new UserMessage(msg.content()));
                } else if ("assistant".equals(msg.role())) {
                    messages.add(new AssistantMessage(msg.content()));
                }
            }
        }

        // Add current user question
        String userMessage = ContextBuilder.buildUserMessage(question, sources);
        messages.add(new UserMessage(userMessage));

        return messages;
    }

    private String fallback(String question, List<DocumentChunk> sources) {
        if (sources.isEmpty()) {
            return "知识库中没有检索到与“" + question + "”相关的片段。请先在左侧添加相关知识片段，或换一种更具体的问法。";
        }
        StringBuilder answer = new StringBuilder("已检索到 ").append(sources.size()).append(" 个相关知识片段：\n");
        for (int i = 0; i < sources.size(); i++) {
            DocumentChunk source = sources.get(i);
            answer.append("\n[").append(i + 1).append("] ")
                    .append(source.title()).append("：")
                    .append(source.content());
        }
        answer.append("\n\n（Spring AI 模型调用不可用，返回检索摘要。）");
        return answer.toString();
    }
}
