package com.liujianan.agentdemo.tool.repository;

import com.liujianan.agentdemo.tool.entity.NoteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoteItemRepository extends JpaRepository<NoteItem, Long> {
    List<NoteItem> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<NoteItem> findByIdAndUserId(Long id, String userId);
    List<NoteItem> findByUserIdAndTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String userId, String titleKeyword, String contentKeyword);
    List<NoteItem> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(String userId, LocalDateTime since);
}
