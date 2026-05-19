package com.liujianan.agentdemo.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Provides a PgVectorStore backed by PostgreSQL + pgvector extension.
 * Activated when spring.vectorstore.pgvector.initialize-schema=true.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.vectorstore.pgvector", name = "initialize-schema", havingValue = "true")
public class PgVectorStoreConfig {
    private static final Logger log = LoggerFactory.getLogger(PgVectorStoreConfig.class);

    @Value("${spring.vectorstore.pgvector.index-type:HNSW}")
    private String indexType;

    @Value("${spring.vectorstore.pgvector.distance-type:COSINE_DISTANCE}")
    private String distanceType;

    @Value("${spring.vectorstore.pgvector.dimensions:1536}")
    private int dimensions;

    @Value("${spring.vectorstore.pgvector.remove-existing-vector-store-table:false}")
    private boolean dropExisting;

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        PgVectorStore store = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(dimensions)
                .distanceType(PgVectorStore.PgDistanceType.valueOf(distanceType.toUpperCase()))
                .indexType(PgVectorStore.PgIndexType.valueOf(indexType.toUpperCase()))
                .initializeSchema(true)
                .removeExistingVectorStoreTable(dropExisting)
                .build();
        log.info("PgVectorStore initialized (dimensions={}, distance={}, index={})",
                dimensions, distanceType, indexType);
        return store;
    }
}
