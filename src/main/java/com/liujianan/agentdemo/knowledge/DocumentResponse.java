package com.liujianan.agentdemo.knowledge;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentResponse(
        Long id,
        String title,
        String content,
        List<String> tags,
        LocalDateTime createdAt,
        Citation citation
) {
    public static DocumentResponse from(DocumentChunk chunk) {
        return new DocumentResponse(
                chunk.getId(),
                chunk.getTitle(),
                chunk.getContent(),
                chunk.getTags(),
                chunk.getCreatedAt(),
                Citation.from(chunk)
        );
    }

    public record Citation(
            Long chunkId,
            String documentId,
            Integer chunkIndex,
            Integer pageNumber,
            Integer startOffset,
            Integer endOffset,
            String sourceType,
            String sourceName,
            String title
    ) {
        static Citation from(DocumentChunk chunk) {
            return new Citation(
                    chunk.getId(),
                    chunk.getDocumentId(),
                    chunk.getChunkIndex(),
                    chunk.getPageNumber(),
                    chunk.getStartOffset(),
                    chunk.getEndOffset(),
                    chunk.getSourceType(),
                    chunk.getSourceName(),
                    chunk.getTitle()
            );
        }
    }
}
