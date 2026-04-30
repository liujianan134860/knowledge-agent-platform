package com.liujianan.agentdemo.evaluation;

import com.liujianan.agentdemo.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {
    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping
    public ApiResponse<List<EvaluationCase>> list(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(evaluationService.list(userId));
    }

    @PostMapping
    public ApiResponse<EvaluationCase> add(@Valid @RequestBody CreateEvaluationRequest request,
                                            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(evaluationService.add(request, userId));
    }

    @PostMapping("/run")
    public ApiResponse<RunEvaluationResponse> run(@RequestBody java.util.Map<String, Object> body,
                                                   HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        Object caseIdObj = body.get("caseId");
        Object answerObj = body.get("answer");
        if (caseIdObj == null || answerObj == null) {
            return ApiResponse.fail("caseId and answer are required");
        }
        Long caseId = caseIdObj instanceof Number n ? n.longValue() : Long.valueOf(caseIdObj.toString());
        String answer = answerObj.toString();
        return ApiResponse.ok(evaluationService.run(caseId, answer, userId));
    }
}
