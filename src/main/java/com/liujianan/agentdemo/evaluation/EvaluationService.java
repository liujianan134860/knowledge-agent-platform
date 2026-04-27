package com.liujianan.agentdemo.evaluation;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class EvaluationService {
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<EvaluationCase> cases = new CopyOnWriteArrayList<>();

    public EvaluationService() {
        cases.add(new EvaluationCase(1L, "What is Agent Harness?", List.of("model", "tool", "trace"), "baseline sample", LocalDateTime.now()));
    }

    public EvaluationCase add(CreateEvaluationRequest request) {
        EvaluationCase evaluation = new EvaluationCase(
                idGenerator.incrementAndGet(),
                request.question(),
                request.expectedKeywords() == null ? List.of() : request.expectedKeywords(),
                request.feedback(),
                LocalDateTime.now()
        );
        cases.add(evaluation);
        return evaluation;
    }

    public List<EvaluationCase> list() {
        return cases.stream().sorted((a, b) -> b.createdAt().compareTo(a.createdAt())).toList();
    }
}
