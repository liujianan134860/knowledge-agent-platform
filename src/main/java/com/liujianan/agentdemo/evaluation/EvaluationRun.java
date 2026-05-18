package com.liujianan.agentdemo.evaluation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_run")
public class EvaluationRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long caseId;

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    private boolean retrievalHit;
    private boolean citationPresent;
    private boolean keywordMatch;
    private boolean hasUnsupportedClaims;
    private double score;

    @Column(length = 80)
    private String promptVersion;

    @Column(length = 80)
    private String modelVersion;

    @Column(length = 80)
    private String retrievalVersion;

    @Column(length = 500)
    private String summary;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected EvaluationRun() {
    }

    public EvaluationRun(Long id, Long caseId, String userId, String answer, boolean retrievalHit,
                         boolean citationPresent, boolean keywordMatch, boolean hasUnsupportedClaims,
                         double score, String promptVersion, String modelVersion, String retrievalVersion,
                         String summary, LocalDateTime createdAt) {
        this.id = id;
        this.caseId = caseId;
        this.userId = userId;
        this.answer = answer;
        this.retrievalHit = retrievalHit;
        this.citationPresent = citationPresent;
        this.keywordMatch = keywordMatch;
        this.hasUnsupportedClaims = hasUnsupportedClaims;
        this.score = score;
        this.promptVersion = promptVersion;
        this.modelVersion = modelVersion;
        this.retrievalVersion = retrievalVersion;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getCaseId() { return caseId; }
    public String getUserId() { return userId; }
    public double getScore() { return score; }
    public String getPromptVersion() { return promptVersion; }
    public String getModelVersion() { return modelVersion; }
    public String getRetrievalVersion() { return retrievalVersion; }
    public String getSummary() { return summary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
