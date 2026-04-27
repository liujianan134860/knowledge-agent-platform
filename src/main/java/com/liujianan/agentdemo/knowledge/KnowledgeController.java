package com.liujianan.agentdemo.knowledge;

import com.liujianan.agentdemo.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public ApiResponse<List<DocumentChunk>> list() {
        return ApiResponse.ok(knowledgeService.list());
    }

    @PostMapping
    public ApiResponse<DocumentChunk> add(@Valid @RequestBody AddDocumentRequest request) {
        return ApiResponse.ok(knowledgeService.add(request));
    }

    @GetMapping("/search")
    public ApiResponse<List<DocumentChunk>> search(@RequestParam String query,
                                                   @RequestParam(defaultValue = "3") int topK) {
        return ApiResponse.ok(knowledgeService.search(query, topK));
    }
}
