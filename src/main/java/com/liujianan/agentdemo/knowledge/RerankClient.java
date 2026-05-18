package com.liujianan.agentdemo.knowledge;

import java.util.List;

public interface RerankClient {
    boolean isConfigured();

    List<RerankResult> rerank(String query, List<DocumentChunk> candidates, int topK);

    String modelName();
}
