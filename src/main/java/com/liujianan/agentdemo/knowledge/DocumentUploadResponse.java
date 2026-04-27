package com.liujianan.agentdemo.knowledge;

import java.util.List;

public record DocumentUploadResponse(
        String filename,
        String contentType,
        int characterCount,
        int chunkCount,
        List<DocumentChunk> chunks
) {
}
