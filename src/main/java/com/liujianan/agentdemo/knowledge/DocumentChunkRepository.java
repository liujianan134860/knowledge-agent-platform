package com.liujianan.agentdemo.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByUserId(String userId);

    void deleteByIdAndUserId(Long id, String userId);
}
