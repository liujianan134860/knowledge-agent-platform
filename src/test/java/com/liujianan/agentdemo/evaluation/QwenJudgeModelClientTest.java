package com.liujianan.agentdemo.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QwenJudgeModelClientTest {

    @Test
    void isConfigured_withoutApiKey_shouldReturnFalse() {
        QwenJudgeModelClient client = new QwenJudgeModelClient(new ObjectMapper(), "",
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen3.6-plus", 5);

        assertFalse(client.isConfigured());
        assertEquals("qwen3.6-plus", client.modelName());
    }

    @Test
    void judge_withoutApiKey_shouldThrowClearError() {
        QwenJudgeModelClient client = new QwenJudgeModelClient(new ObjectMapper(), "",
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen3.6-plus", 5);

        assertThrows(IllegalStateException.class, () -> client.judge("score this"));
    }
}
