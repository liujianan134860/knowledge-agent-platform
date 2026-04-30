package com.liujianan.agentdemo.evaluation;

import com.liujianan.agentdemo.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qa")
public class QaReviewController {

    private final QaReviewService qaReviewService;

    public QaReviewController(QaReviewService qaReviewService) {
        this.qaReviewService = qaReviewService;
    }

    @PostMapping("/review")
    public ApiResponse<QaReviewResult> review(@RequestBody QaReviewRequest request) {
        QaReviewResult result = qaReviewService.review(request);
        return ApiResponse.ok(result);
    }
}
