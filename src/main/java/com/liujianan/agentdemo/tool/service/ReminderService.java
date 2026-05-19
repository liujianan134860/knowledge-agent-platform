package com.liujianan.agentdemo.tool.service;

import com.liujianan.agentdemo.tool.entity.ReminderItem;
import com.liujianan.agentdemo.tool.repository.ReminderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderService {
    private final ReminderItemRepository reminderRepository;

    public ReminderService(ReminderItemRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    @Transactional
    public ReminderItem create(String userId, String title, LocalDateTime remindAt, String note) {
        ReminderItem reminder = new ReminderItem(null, userId, title, remindAt, "PENDING", note, LocalDateTime.now());
        return reminderRepository.save(reminder);
    }

    public List<ReminderItem> list(String userId, String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            return reminderRepository.findByUserIdAndStatusOrderByRemindAtAsc(userId, statusFilter.toUpperCase());
        }
        return reminderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<ReminderItem> listPending(String userId) {
        return reminderRepository.findByUserIdAndStatusOrderByRemindAtAsc(userId, "PENDING");
    }
}
