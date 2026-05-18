package com.liujianan.agentdemo.evaluation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RunEvaluationCaseRequest(
        @NotNull Long caseId,
        @NotBlank String answer
) {
}
