package com.liujianan.agentdemo.tool.repository;

import com.liujianan.agentdemo.tool.entity.TaskItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {
    List<TaskItem> findByUserIdOrderByCreatedAtDesc(String userId);
    List<TaskItem> findByUserIdAndStatusOrderByDueDateAsc(String userId, String status);
    Optional<TaskItem> findByIdAndUserId(Long id, String userId);
}
