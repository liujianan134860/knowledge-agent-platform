package com.liujianan.agentdemo.tool.builtin;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BuiltinToolCallbackConfig {

    @Bean
    public ToolCallbackProvider builtinToolsToolCallbackProvider(
            DateTimeTool dateTimeTool,
            TaskTool taskTool,
            ReminderTool reminderTool,
            NoteTool noteTool,
            MemoryTool memoryTool,
            KnowledgeSearchTool knowledgeSearchTool,
            DocumentTool documentTool,
            WebSearchTool webSearchTool,
            FileReadTool fileReadTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(dateTimeTool, taskTool, reminderTool, noteTool, memoryTool,
                        knowledgeSearchTool, documentTool, webSearchTool, fileReadTool)
                .build();
    }
}
