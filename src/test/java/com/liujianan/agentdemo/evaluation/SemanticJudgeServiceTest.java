package com.liujianan.agentdemo.evaluation;

import com.liujianan.agentdemo.common.AiPlatformProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SemanticJudgeServiceTest {

    @Test
    void judge_whenModelConfigured_shouldParseLlmScore() {
        JudgeModelClient judgeModelClient = mock(JudgeModelClient.class);
        when(judgeModelClient.isConfigured()).thenReturn(true);
        when(judgeModelClient.modelName()).thenReturn("qwen3.6-plus");
        when(judgeModelClient.judge(anyString()))
                .thenReturn("score: 0.82\nreason: covers the main idea");
        ObjectProvider<JudgeModelClient> provider = mockProvider(judgeModelClient);

        SemanticJudgeService service = new SemanticJudgeService(provider, new AiPlatformProperties());

        SemanticJudgeService.JudgeResult result = service.judge("What is Spring?",
                "Spring is a Java framework.", List.of("Spring", "framework"));

        assertEquals("qwen:qwen3.6-plus", result.mode());
        assertEquals(0.82, result.score(), 0.001);
    }

    @Test
    void judge_withoutModel_shouldFallbackToSemanticOverlap() {
        AiPlatformProperties properties = new AiPlatformProperties();
        properties.setLlmJudgeEnabled(false);
        SemanticJudgeService service = new SemanticJudgeService(mockProvider(null), properties);

        SemanticJudgeService.JudgeResult result = service.judge("What is Spring?",
                "Spring is a Java framework.", List.of("Spring", "framework"));

        assertEquals("fallback", result.mode());
        assertTrue(result.score() > 0.5);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<JudgeModelClient> mockProvider(JudgeModelClient judgeModelClient) {
        ObjectProvider<JudgeModelClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(judgeModelClient);
        return provider;
    }
}
