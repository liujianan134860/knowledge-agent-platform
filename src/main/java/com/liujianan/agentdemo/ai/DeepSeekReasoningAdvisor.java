package com.liujianan.agentdemo.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeepSeekReasoningAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekReasoningAdvisor.class);
    static final String CONTEXT_KEY = "deepseek_reasoning_content";

    @Override
    public String getName() { return "deepseek-reasoning"; }

    @Override
    public int getOrder() { return -100; }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Prompt enrichedPrompt = injectReasoning(request.prompt(), request.context());
        ChatClientRequest enrichedRequest = ChatClientRequest.builder()
                .prompt(enrichedPrompt)
                .context(request.context())
                .build();

        ChatClientResponse response = chain.nextCall(enrichedRequest);

        String rc = extractReasoning(response.chatResponse());
        if (rc != null && !rc.isEmpty()) {
            log.debug("Stored reasoning_content ({} chars)", rc.length());
            Map<String, Object> ctx = new HashMap<>();
            if (response.context() != null) ctx.putAll(response.context());
            ctx.put(CONTEXT_KEY, rc);
            return ChatClientResponse.builder()
                    .chatResponse(response.chatResponse())
                    .context(ctx)
                    .build();
        }
        return response;
    }

    private Prompt injectReasoning(Prompt prompt, Map<String, Object> context) {
        if (context == null) return prompt;
        String stored = (String) context.get(CONTEXT_KEY);
        if (stored == null || stored.isEmpty()) return prompt;

        boolean needsInjection = false;
        for (Message msg : prompt.getInstructions()) {
            if (msg instanceof AssistantMessage) {
                Object existing = msg.getMetadata().get(CONTEXT_KEY);
                if (existing == null || !stored.equals(existing)) { needsInjection = true; break; }
            }
        }
        if (!needsInjection) return prompt;

        List<Message> enriched = new ArrayList<>();
        for (Message msg : prompt.getInstructions()) {
            if (msg instanceof AssistantMessage) {
                AssistantMessage assistant = (AssistantMessage) msg;
                Map<String, Object> meta = new HashMap<>(assistant.getMetadata());
                meta.put(CONTEXT_KEY, stored);
                enriched.add(AssistantMessage.builder()
                        .content(assistant.getText())
                        .properties(meta)
                        .toolCalls(assistant.getToolCalls())
                        .media(assistant.getMedia())
                        .build());
            } else {
                enriched.add(msg);
            }
        }
        log.debug("Injected reasoning_content into assistant messages");
        return new Prompt(enriched, prompt.getOptions());
    }

    private String extractReasoning(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResults() == null) return null;
        for (Generation gen : chatResponse.getResults()) {
            if (gen.getMetadata() != null) {
                Object rc = gen.getMetadata().get("reasoning_content");
                if (rc instanceof String) {
                    String s = (String) rc;
                    if (!s.isEmpty()) return s;
                }
            }
        }
        return null;
    }
}
