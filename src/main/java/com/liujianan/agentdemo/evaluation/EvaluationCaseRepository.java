package com.liujianan.agentdemo.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationCaseRepository extends JpaRepository<EvaluationCase, Long> {
    List<EvaluationCase> findByUserIdOrderByCreatedAtDesc(String userId);
}
