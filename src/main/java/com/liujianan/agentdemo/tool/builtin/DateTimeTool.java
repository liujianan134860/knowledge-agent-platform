package com.liujianan.agentdemo.tool.builtin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.liujianan.agentdemo.mcp.McpToolService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class DateTimeTool {

    @Tool(name = "datetime.now", description = "Get the current date and time. Returns ISO-8601 formatted timestamp.")
    public String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
