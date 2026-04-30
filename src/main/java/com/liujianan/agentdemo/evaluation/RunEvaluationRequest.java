package com.liujianan.agentdemo.evaluation;

public record RunEvaluationRequest(
        Long caseId,
        String answer
) {
}
