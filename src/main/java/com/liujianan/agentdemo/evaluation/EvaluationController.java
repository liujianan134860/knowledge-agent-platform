package com.liujianan.agentdemo.evaluation;

import com.liujianan.agentdemo.common.ApiResponse;
import com.liujianan.agentdemo.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ApiResponse<List<EvaluationCaseResponse>> list(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(evaluationService.list(userId).stream().map(EvaluationCaseResponse::from).toList());
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<EvaluationCaseResponse>> page(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size,
                                                                  HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(PageResponse.from(evaluationService.list(userId, pageable).map(EvaluationCaseResponse::from)));
    }

    @PostMapping
    public ApiResponse<EvaluationCaseResponse> add(@Valid @RequestBody CreateEvaluationRequest request,
                                                   HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(EvaluationCaseResponse.from(evaluationService.add(request, userId)));
    }

    @PostMapping("/run")
    public ApiResponse<RunEvaluationResponse> run(@Valid @RequestBody RunEvaluationCaseRequest request,
                                                  HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(evaluationService.run(request.caseId(), request.answer(), userId));
    }

    @GetMapping("/runs")
    public ApiResponse<PageResponse<EvaluationRunResponse>> runs(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size,
                                                                 HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(PageResponse.from(evaluationService.listRuns(userId, pageable).map(EvaluationRunResponse::from)));
    }
}
