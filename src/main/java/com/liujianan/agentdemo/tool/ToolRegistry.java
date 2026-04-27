package com.liujianan.agentdemo.tool;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class ToolRegistry {
    private final Map<String, RegisteredTool> tools = Map.of(
            "echo", new RegisteredTool(
                    new ToolDefinition("echo", "Return the input text for connectivity testing.", "hello", "{\"input\":\"string\"}", "PUBLIC", 1000),
                    input -> new ToolResult("echo", input)
            ),
            "calculator", new RegisteredTool(
                    new ToolDefinition("calculator", "Calculate simple expressions such as '1 + 2'.", "12 + 30", "{\"input\":\"number operator number\"}", "PUBLIC", 1000),
                    this::calculate
            ),
            "http_mock", new RegisteredTool(
                    new ToolDefinition("http_mock", "Mock an HTTP API call and return a deterministic response.", "GET /api/projects", "{\"input\":\"method path\"}", "INTERNAL", 1500),
                    input -> new ToolResult("http_mock", "mock response for: " + input)
            )
    );

    public List<ToolDefinition> listTools() {
        return tools.values().stream()
                .map(RegisteredTool::definition)
                .toList();
    }

    public ToolResult invoke(String name, String input) {
        RegisteredTool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("unknown tool: " + name);
        }
        return tool.handler().apply(input);
    }

    private ToolResult calculate(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("calculator input must look like: 12 + 30");
        }
        BigDecimal left = new BigDecimal(parts[0]);
        BigDecimal right = new BigDecimal(parts[2]);
        BigDecimal result = switch (parts[1]) {
            case "+" -> left.add(right);
            case "-" -> left.subtract(right);
            case "*" -> left.multiply(right);
            case "/" -> {
                if (BigDecimal.ZERO.compareTo(right) == 0) {
                    throw new IllegalArgumentException("division by zero");
                }
                yield left.divide(right);
            }
            default -> throw new IllegalArgumentException("unsupported operator: " + parts[1]);
        };
        return new ToolResult("calculator", result.stripTrailingZeros().toPlainString());
    }

    private record RegisteredTool(ToolDefinition definition, Function<String, ToolResult> handler) {
    }
}
