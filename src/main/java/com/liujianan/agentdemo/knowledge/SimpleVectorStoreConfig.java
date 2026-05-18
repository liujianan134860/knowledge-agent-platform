package com.liujianan.agentdemo.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Path;

/**
 * Provides an in-memory SimpleVectorStore with file persistence
 * for local development and production (H2-backed).
 */
@Configuration
public class SimpleVectorStoreConfig {
    private static final Logger log = LoggerFactory.getLogger(SimpleVectorStoreConfig.class);
    private static final String STORE_FILE = "data/vector-store.json";

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File file = Path.of(STORE_FILE).toFile();

        if (file.exists()) {
            try {
                store.load(file);
                log.info("Loaded vector store from {} ({} entries)", file.getAbsolutePath(), countStore(store));
            } catch (Exception e) {
                log.warn("Failed to load vector store: {}", e.getMessage());
            }
        } else {
            log.info("No existing vector store file at {}, starting fresh", file.getAbsolutePath());
        }

        return new AutoSaveVectorStore(store, file);
    }

    private int countStore(SimpleVectorStore store) {
        try {
            var field = SimpleVectorStore.class.getDeclaredField("store");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var map = (java.util.Map<String, ?>) field.get(store);
            return map.size();
        } catch (Exception e) {
            return -1;
        }
    }

    private static class AutoSaveVectorStore implements VectorStore {
        private final SimpleVectorStore delegate;
        private final File file;

        AutoSaveVectorStore(SimpleVectorStore delegate, File file) {
            this.delegate = delegate;
            this.file = file;
        }

        @Override
        public void add(java.util.List<org.springframework.ai.document.Document> documents) {
            delegate.add(documents);
            persist();
        }

        @Override
        public java.util.List<org.springframework.ai.document.Document> similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest request) {
            return delegate.similaritySearch(request);
        }

        @Override
        public void delete(java.util.List<String> idList) {
            delegate.delete(idList);
            persist();
        }

        @Override
        public void delete(org.springframework.ai.vectorstore.filter.Filter.Expression filterExpression) {
            delegate.delete(filterExpression);
            persist();
        }

        private void persist() {
            try {
                file.getParentFile().mkdirs();
                delegate.save(file);
            } catch (Exception e) {
                log.warn("Failed to persist vector store: {}", e.getMessage());
            }
        }
    }
}
