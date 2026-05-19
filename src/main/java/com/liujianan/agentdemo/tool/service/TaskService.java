package com.liujianan.agentdemo.tool.service;

import com.liujianan.agentdemo.tool.entity.TaskItem;
import com.liujianan.agentdemo.tool.repository.TaskItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {
    private final TaskItemRepository taskRepository;

    public TaskService(TaskItemRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskItem create(String userId, String title, String description, String priority, LocalDateTime dueDate) {
        LocalDateTime now = LocalDateTime.now();
        TaskItem task = new TaskItem(null, userId, title, description,
                priority != null ? priority.toUpperCase() : "MEDIUM",
                "PENDING", dueDate, null, now, now);
        return taskRepository.save(task);
    }

    public List<TaskItem> list(String userId, String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            return taskRepository.findByUserIdAndStatusOrderByDueDateAsc(userId, statusFilter.toUpperCase());
        }
        return taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public TaskItem update(String userId, Long id, String title, String description, String priority, LocalDateTime dueDate) {
        TaskItem task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("task not found: " + id));
        if (title != null && !title.isBlank()) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (priority != null) task.setPriority(priority.toUpperCase());
        if (dueDate != null) task.setDueDate(dueDate);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Transactional
    public TaskItem complete(String userId, Long id) {
        TaskItem task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("task not found: " + id));
        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }
}
