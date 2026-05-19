package com.liujianan.agentdemo.tool.repository;

import com.liujianan.agentdemo.tool.entity.ReminderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReminderItemRepository extends JpaRepository<ReminderItem, Long> {
    List<ReminderItem> findByUserIdOrderByCreatedAtDesc(String userId);
    List<ReminderItem> findByUserIdAndStatusOrderByRemindAtAsc(String userId, String status);
    Optional<ReminderItem> findByIdAndUserId(Long id, String userId);
    List<ReminderItem> findByUserIdAndRemindAtBeforeAndStatus(String userId, LocalDateTime now, String status);
}
