package com.liujianan.agentdemo.mcp;

import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers @Tool-annotated methods (from McpToolService) as tool callbacks
 * so that the LLM (via ChatClient) can discover and invoke these tools
 * during answer generation.
 */
@Configuration
public class LocalToolCallbackConfig {

    @Bean
    public ToolCallbackProvider localToolsToolCallbackProvider(McpToolService mcpToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(mcpToolService)
                .build();
    }
}
