package com.liujianan.agentdemo.ai;

import com.liujianan.agentdemo.harness.SessionMessage;
import com.liujianan.agentdemo.knowledge.DocumentChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpringAiModelClientTest {

    @Mock
    private ChatModel chatModel;

    private SpringAiModelClient modelClient;

    @BeforeEach
    void setUp() {
        modelClient = new SpringAiModelClient(chatModel);
    }

    private ChatResponse createChatResponse(String text) {
        return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(text))))
                .build();
    }

    @Test
    void isConfigured_shouldReturnTrue() {
        assertTrue(modelClient.isConfigured());
    }

    @Test
    void answer_shouldReturnResponseFromChatModel() {
        String expectedAnswer = "This is a test answer from the AI model.";
        when(chatModel.call(any(Prompt.class))).thenReturn(createChatResponse(expectedAnswer));

        String result = modelClient.answer("What is Java?", new ArrayList<>(), new ArrayList<>());

        assertEquals(expectedAnswer, result);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void answer_withEmptySources_shouldStillCallChatModel() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(createChatResponse("I don't have specific knowledge about this."));

        String result = modelClient.answer("Hello", new ArrayList<>(), new ArrayList<>());

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void answer_withSourcesAndHistory_shouldBuildPromptWithContext() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(createChatResponse("Based on the sources, here is the answer."));

        List<DocumentChunk> sources = List.of(
                new DocumentChunk(1L, "Java Basics", "Java is a programming language.",
                        List.of("java"), LocalDateTime.now(), "user1")
        );
        List<SessionMessage> history = List.of(
                new SessionMessage("user", "What is Java?", LocalDateTime.now()),
                new SessionMessage("assistant", "Java is...", LocalDateTime.now())
        );

        String result = modelClient.answer("Tell me more about Java", sources, history);

        assertEquals("Based on the sources, here is the answer.", result);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void answer_whenChatModelThrowsException_shouldReturnFallback() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API error"));

        String result = modelClient.answer("What is AI?", new ArrayList<>(), new ArrayList<>());

        assertTrue(result.contains("知识库中没有检索到"));
        assertTrue(result.contains("[Spring AI call failed: API error]"));
    }

    @Test
    void answer_withSources_whenChatModelThrowsException_shouldReturnFallbackWithSources() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Network error"));

        List<DocumentChunk> sources = List.of(
                new DocumentChunk(1L, "AI Intro", "Artificial Intelligence is...",
                        List.of("ai"), LocalDateTime.now(), "user1")
        );

        String result = modelClient.answer("What is AI?", sources, new ArrayList<>());

        assertTrue(result.contains("已检索到 1 个相关知识片段"));
        assertTrue(result.contains("[Spring AI call failed: Network error]"));
    }

    @Test
    void answerStream_shouldDeliverTokensViaConsumer() throws InterruptedException {
        String token1 = "Hello";
        String token2 = " world";
        String token3 = "!";

        when(chatModel.stream(any(Prompt.class)))
                .thenReturn(Flux.just(
                        createChatResponse(token1),
                        createChatResponse(token2),
                        createChatResponse(token3)
                ));

        StringBuilder streamedText = new StringBuilder();
        AtomicReference<String> doneText = new AtomicReference<>();
        List<String> errors = new ArrayList<>();

        modelClient.answerStream("Hi", new ArrayList<>(), new ArrayList<>(),
                delta -> streamedText.append(delta),
                full -> doneText.set(full),
                error -> errors.add(error)
        );

        Thread.sleep(500);

        assertEquals("Hello world!", streamedText.toString());
        assertEquals("Hello world!", doneText.get());
        assertTrue(errors.isEmpty());
        verify(chatModel, times(1)).stream(any(Prompt.class));
    }

    @Test
    void answerStream_whenChatModelThrowsException_shouldReturnFallbackStream() throws InterruptedException {
        when(chatModel.stream(any(Prompt.class))).thenThrow(new RuntimeException("Stream error"));

        StringBuilder streamedText = new StringBuilder();
        AtomicReference<String> doneText = new AtomicReference<>();
        List<String> errors = new ArrayList<>();

        modelClient.answerStream("Hi", new ArrayList<>(), new ArrayList<>(),
                delta -> streamedText.append(delta),
                full -> doneText.set(full),
                error -> errors.add(error)
        );

        Thread.sleep(500);

        assertTrue(streamedText.length() > 0);
        assertNotNull(doneText.get());
        assertTrue(doneText.get().contains("知识库中没有检索到"));
    }
}
