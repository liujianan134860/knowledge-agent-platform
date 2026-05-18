package com.liujianan.agentdemo.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai-platform")
public class AiPlatformProperties {
    private String promptVersion = "prompt-v1";
    private String retrievalVersion = "hybrid-v1";
    private int modelMaxAttempts = 2;
    private int modelCircuitBreakerThreshold = 3;
    private long modelCircuitBreakerOpenMs = 30000;
    private int retrievalTopK = 3;
    private String modelVersion = "configured-model";
    private String embeddingProvider = "openai-compatible";
    private String embeddingModel = "text-embedding-3-small";
    private int embeddingDimensions = 1536;
    private boolean llmJudgeEnabled = true;
    private String judgeProvider = "qwen";
    private String judgeModel = "qwen3.6-plus";
    private boolean rerankEnabled = true;
    private String rerankProvider = "aliyun-bailian";
    private String rerankModel = "qwen3-rerank";
    private int rerankCandidateMultiplier = 4;

    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getRetrievalVersion() { return retrievalVersion; }
    public void setRetrievalVersion(String retrievalVersion) { this.retrievalVersion = retrievalVersion; }
    public int getModelMaxAttempts() { return modelMaxAttempts; }
    public void setModelMaxAttempts(int modelMaxAttempts) { this.modelMaxAttempts = modelMaxAttempts; }
    public int getModelCircuitBreakerThreshold() { return modelCircuitBreakerThreshold; }
    public void setModelCircuitBreakerThreshold(int modelCircuitBreakerThreshold) { this.modelCircuitBreakerThreshold = modelCircuitBreakerThreshold; }
    public long getModelCircuitBreakerOpenMs() { return modelCircuitBreakerOpenMs; }
    public void setModelCircuitBreakerOpenMs(long modelCircuitBreakerOpenMs) { this.modelCircuitBreakerOpenMs = modelCircuitBreakerOpenMs; }
    public int getRetrievalTopK() { return retrievalTopK; }
    public void setRetrievalTopK(int retrievalTopK) { this.retrievalTopK = retrievalTopK; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getEmbeddingProvider() { return embeddingProvider; }
    public void setEmbeddingProvider(String embeddingProvider) { this.embeddingProvider = embeddingProvider; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public int getEmbeddingDimensions() { return embeddingDimensions; }
    public void setEmbeddingDimensions(int embeddingDimensions) { this.embeddingDimensions = embeddingDimensions; }
    public boolean isLlmJudgeEnabled() { return llmJudgeEnabled; }
    public void setLlmJudgeEnabled(boolean llmJudgeEnabled) { this.llmJudgeEnabled = llmJudgeEnabled; }
    public String getJudgeProvider() { return judgeProvider; }
    public void setJudgeProvider(String judgeProvider) { this.judgeProvider = judgeProvider; }
    public String getJudgeModel() { return judgeModel; }
    public void setJudgeModel(String judgeModel) { this.judgeModel = judgeModel; }
    public boolean isRerankEnabled() { return rerankEnabled; }
    public void setRerankEnabled(boolean rerankEnabled) { this.rerankEnabled = rerankEnabled; }
    public String getRerankProvider() { return rerankProvider; }
    public void setRerankProvider(String rerankProvider) { this.rerankProvider = rerankProvider; }
    public String getRerankModel() { return rerankModel; }
    public void setRerankModel(String rerankModel) { this.rerankModel = rerankModel; }
    public int getRerankCandidateMultiplier() { return rerankCandidateMultiplier; }
    public void setRerankCandidateMultiplier(int rerankCandidateMultiplier) { this.rerankCandidateMultiplier = rerankCandidateMultiplier; }
}
