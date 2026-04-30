package com.liujianan.agentdemo.knowledge;

import com.liujianan.agentdemo.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public ApiResponse<List<DocumentChunk>> list(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(knowledgeService.list(userId));
    }

    @PostMapping
    public ApiResponse<DocumentChunk> add(@Valid @RequestBody AddDocumentRequest request, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(knowledgeService.add(request, userId));
    }

    @PostMapping("/upload")
    public ApiResponse<DocumentUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(required = false) String title,
                                                      @RequestParam(required = false) String tags,
                                                      HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        List<String> tagList = tags == null || tags.isBlank()
                ? List.of("upload")
                : Arrays.stream(tags.split("[,，]")).map(String::trim).filter(tag -> !tag.isBlank()).toList();
        return ApiResponse.ok(knowledgeService.upload(file, title, tagList, userId));
    }

    @GetMapping("/search")
    public ApiResponse<List<DocumentChunk>> search(@RequestParam String query,
                                                   @RequestParam(defaultValue = "3") int topK,
                                                   HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(knowledgeService.search(query, topK, userId));
    }

    @DeleteMapping("/{documentId}")
    public ApiResponse<Boolean> delete(@PathVariable Long documentId, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        boolean deleted = knowledgeService.delete(documentId, userId);
        return deleted ? ApiResponse.ok(true) : ApiResponse.fail("document not found: " + documentId);
    }
}
