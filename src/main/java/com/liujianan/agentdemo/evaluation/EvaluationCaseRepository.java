package com.liujianan.agentdemo.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EvaluationCaseRepository extends JpaRepository<EvaluationCase, Long> {
    List<EvaluationCase> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<EvaluationCase> findByUserId(String userId, Pageable pageable);
}
