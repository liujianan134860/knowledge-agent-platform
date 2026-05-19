package com.liujianan.agentdemo.tool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_memory")
public class PersonalMemory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(name = "mem_key", nullable = false, length = 100)
    private String key;

    @Column(name = "mem_value", columnDefinition = "TEXT", nullable = false)
    private String value;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public PersonalMemory() {
    }

    public PersonalMemory(Long id, String userId, String key, String value, String category,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.key = key;
        this.value = value;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getKey() { return key; }
    public String getValue() { return value; }
    public String getCategory() { return category; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setKey(String key) { this.key = key; }
    public void setValue(String value) { this.value = value; }
    public void setCategory(String category) { this.category = category; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long id() { return id; }
    public String userId() { return userId; }
    public String key() { return key; }
    public String value() { return value; }
    public String category() { return category; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
}
