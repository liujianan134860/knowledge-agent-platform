package com.liujianan.agentdemo.knowledge;

import com.liujianan.agentdemo.common.AiPlatformProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingConfigurationValidator implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingConfigurationValidator.class);

    private final AiPlatformProperties properties;
    private final Environment environment;
    private final ObjectProvider<VectorKnowledgeService> vectorKnowledgeService;

    public EmbeddingConfigurationValidator(AiPlatformProperties properties, Environment environment,
                                           ObjectProvider<VectorKnowledgeService> vectorKnowledgeService) {
        this.properties = properties;
        this.environment = environment;
        this.vectorKnowledgeService = vectorKnowledgeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    public EmbeddingValidationResult validate() {
        boolean vectorEnabled = vectorKnowledgeService.getIfAvailable() != null;
        String chatBaseUrl = environment.getProperty("spring.ai.openai.base-url", "");
        String embeddingBaseUrl = environment.getProperty("spring.ai.openai.embedding.base-url", "");
        String embeddingModel = properties.getEmbeddingModel();
        int configuredDimensions = properties.getEmbeddingDimensions();
        int vectorDimensions = environment.getProperty("spring.ai.vectorstore.pgvector.dimensions", Integer.class, configuredDimensions);

        boolean dimensionMatch = configuredDimensions == vectorDimensions;
        boolean deepSeekChat = chatBaseUrl.contains("deepseek");
        boolean bailianEmbedding = embeddingBaseUrl.contains("dashscope") || "aliyun-bailian".equalsIgnoreCase(properties.getEmbeddingProvider());
        boolean embeddingSeparated = !embeddingBaseUrl.isBlank() && !embeddingBaseUrl.contains("deepseek");
        boolean usable = !vectorEnabled || (dimensionMatch && (!deepSeekChat || embeddingSeparated) && bailianEmbedding);

        if (!usable) {
            log.warn("Embedding configuration requires attention: vectorEnabled={}, embeddingModel={}, configuredDimensions={}, vectorDimensions={}, chatBaseUrl={}, embeddingBaseUrl={}",
                    vectorEnabled, embeddingModel, configuredDimensions, vectorDimensions, chatBaseUrl, embeddingBaseUrl);
        } else {
            log.info("Embedding configuration validated: vectorEnabled={}, provider={}, model={}, dimensions={}",
                    vectorEnabled, properties.getEmbeddingProvider(), embeddingModel, configuredDimensions);
        }
        return new EmbeddingValidationResult(vectorEnabled, properties.getEmbeddingProvider(), embeddingModel,
                configuredDimensions, vectorDimensions, usable);
    }

    public record EmbeddingValidationResult(boolean vectorEnabled, String provider, String model,
                                            int configuredDimensions, int vectorDimensions, boolean usable) {
    }
}
