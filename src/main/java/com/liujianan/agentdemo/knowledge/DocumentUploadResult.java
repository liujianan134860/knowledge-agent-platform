package com.liujianan.agentdemo.knowledge;

import java.util.List;

public record DocumentUploadResult(
        String filename,
        String contentType,
        int characterCount,
        int chunkCount,
        List<DocumentResponse> chunks
) {
    public static DocumentUploadResult from(DocumentUploadResponse response) {
        return new DocumentUploadResult(
                response.filename(),
                response.contentType(),
                response.characterCount(),
                response.chunkCount(),
                response.chunks().stream().map(DocumentResponse::from).toList()
        );
    }
}
