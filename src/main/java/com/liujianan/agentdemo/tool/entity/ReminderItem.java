package com.liujianan.agentdemo.tool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminder_item")
public class ReminderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDateTime remindAt;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ReminderItem() {
    }

    public ReminderItem(Long id, String userId, String title, LocalDateTime remindAt,
                        String status, String note, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.remindAt = remindAt;
        this.status = status;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public LocalDateTime getRemindAt() { return remindAt; }
    public String getStatus() { return status; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setRemindAt(LocalDateTime remindAt) { this.remindAt = remindAt; }
    public void setStatus(String status) { this.status = status; }
    public void setNote(String note) { this.note = note; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long id() { return id; }
    public String userId() { return userId; }
    public String title() { return title; }
    public LocalDateTime remindAt() { return remindAt; }
    public String status() { return status; }
    public String note() { return note; }
    public LocalDateTime createdAt() { return createdAt; }
}
