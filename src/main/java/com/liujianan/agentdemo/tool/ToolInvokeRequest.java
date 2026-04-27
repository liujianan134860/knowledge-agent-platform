package com.liujianan.agentdemo.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ToolInvokeRequest(
        @NotBlank
        @Size(max = 200)
        String input
) {
}
