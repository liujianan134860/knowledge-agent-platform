package com.liujianan.agentdemo.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "document_chunk")
public class DocumentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 3000)
    private String content;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> tags;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 64)
    private String userId;

    public DocumentChunk() {
    }

    public DocumentChunk(Long id, String title, String content, List<String> tags, LocalDateTime createdAt, String userId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public List<String> getTags() { return tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getUserId() { return userId; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUserId(String userId) { this.userId = userId; }

    // Method-style accessors for backward compatibility
    public Long id() { return id; }
    public String title() { return title; }
    public String content() { return content; }
    public List<String> tags() { return tags; }
    public LocalDateTime createdAt() { return createdAt; }
    public String userId() { return userId; }
}
