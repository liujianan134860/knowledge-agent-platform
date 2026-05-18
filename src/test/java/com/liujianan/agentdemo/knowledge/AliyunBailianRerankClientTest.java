package com.liujianan.agentdemo.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AliyunBailianRerankClientTest {

    @Test
    void isConfigured_withoutApiKey_shouldReturnFalse() {
        AliyunBailianRerankClient client = new AliyunBailianRerankClient(new ObjectMapper(), "",
                "https://dashscope.aliyuncs.com/compatible-api/v1", "qwen3-rerank", 5);

        assertFalse(client.isConfigured());
        assertEquals("qwen3-rerank", client.modelName());
    }

    @Test
    void rerank_withoutApiKey_shouldThrowClearError() {
        AliyunBailianRerankClient client = new AliyunBailianRerankClient(new ObjectMapper(), "",
                "https://dashscope.aliyuncs.com/compatible-api/v1", "qwen3-rerank", 5);

        assertThrows(IllegalStateException.class, () -> client.rerank("query", java.util.List.of(
                new DocumentChunk(1L, "Title", "Content", java.util.List.of(), java.time.LocalDateTime.now(), "u1")
        ), 1));
    }
}
