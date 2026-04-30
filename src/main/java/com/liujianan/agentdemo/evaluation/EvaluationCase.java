package com.liujianan.agentdemo.evaluation;

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
@Table(name = "evaluation_case")
public class EvaluationCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String question;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> expectedKeywords;

    @Column(length = 500)
    private String feedback;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 64)
    private String userId;

    public EvaluationCase() {
    }

    public EvaluationCase(Long id, String question, List<String> expectedKeywords, String feedback,
                          LocalDateTime createdAt, String userId) {
        this.id = id;
        this.question = question;
        this.expectedKeywords = expectedKeywords;
        this.feedback = feedback;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getQuestion() { return question; }
    public List<String> getExpectedKeywords() { return expectedKeywords; }
    public String getFeedback() { return feedback; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getUserId() { return userId; }

    public void setId(Long id) { this.id = id; }
    public void setQuestion(String question) { this.question = question; }
    public void setExpectedKeywords(List<String> expectedKeywords) { this.expectedKeywords = expectedKeywords; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUserId(String userId) { this.userId = userId; }

    // Method-style accessors
    public Long id() { return id; }
    public String question() { return question; }
    public List<String> expectedKeywords() { return expectedKeywords; }
    public String feedback() { return feedback; }
    public LocalDateTime createdAt() { return createdAt; }
    public String userId() { return userId; }
}
