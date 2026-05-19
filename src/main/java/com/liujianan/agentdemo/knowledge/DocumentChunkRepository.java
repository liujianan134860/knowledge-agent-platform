package com.liujianan.agentdemo.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByUserId(String userId);
    Page<DocumentChunk> findByUserId(String userId, Pageable pageable);

    void deleteByIdAndUserId(Long id, String userId);

    List<DocumentChunk> findByUserIdAndDocumentIdOrderByChunkIndexAsc(String userId, String documentId);
}
