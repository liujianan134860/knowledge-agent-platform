package com.liujianan.agentdemo.knowledge;

public record RerankResult(DocumentChunk chunk, double score, String model) {
}
