package com.liujianan.agentdemo.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AddDocumentRequest(
        @NotBlank
        @Size(max = 80)
        String title,

        @NotBlank
        @Size(max = 1000)
        String content,

        List<String> tags
) {
}
