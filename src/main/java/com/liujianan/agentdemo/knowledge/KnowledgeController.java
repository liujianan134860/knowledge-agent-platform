package com.liujianan.agentdemo.knowledge;

import com.liujianan.agentdemo.common.ApiResponse;
import com.liujianan.agentdemo.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public ApiResponse<List<DocumentResponse>> list(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(knowledgeService.list(userId).stream().map(DocumentResponse::from).toList());
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<DocumentResponse>> page(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size,
                                                            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(PageResponse.from(knowledgeService.list(userId, pageable).map(DocumentResponse::from)));
    }

    @PostMapping
    public ApiResponse<DocumentResponse> add(@Valid @RequestBody AddDocumentRequest request, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(DocumentResponse.from(knowledgeService.add(request, userId)));
    }

    @PostMapping("/upload")
    public ApiResponse<DocumentUploadResult> upload(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(required = false) String title,
                                                    @RequestParam(required = false) String tags,
                                                    HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        List<String> tagList = tags == null || tags.isBlank()
                ? List.of("upload")
                : Arrays.stream(tags.split("[,;，；]")).map(String::trim).filter(tag -> !tag.isBlank()).toList();
        return ApiResponse.ok(DocumentUploadResult.from(knowledgeService.upload(file, title, tagList, userId)));
    }

    @GetMapping("/search")
    public ApiResponse<List<DocumentResponse>> search(@RequestParam String query,
                                                      @RequestParam(defaultValue = "3") int topK,
                                                      HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(knowledgeService.search(query, topK, userId).stream().map(DocumentResponse::from).toList());
    }

    @DeleteMapping("/{documentId}")
    public ApiResponse<Boolean> delete(@PathVariable Long documentId, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        boolean deleted = knowledgeService.delete(documentId, userId);
        return deleted ? ApiResponse.ok(true) : ApiResponse.fail("DOCUMENT_NOT_FOUND", "document not found: " + documentId);
    }
}
