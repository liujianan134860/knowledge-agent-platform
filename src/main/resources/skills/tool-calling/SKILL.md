# Tool Calling Skill

## Overview
This skill defines how tools are registered, discovered, invoked, and monitored in the platform.

## Tool Registration
Tools are registered at startup with the following metadata:
```java
public record ToolDefinition(
    String name,           // unique identifier
    String description,    // human-readable description
    String inputExample,   // example input for discovery
    String parameterSchema,// JSON Schema for parameters
    String permission,     // PUBLIC | INTERNAL
    long timeoutMs         // execution timeout
) {}
```

## Available Tools
### echo
- **Description**: Return the input text for connectivity testing.
- **Input**: Any string
- **Permission**: PUBLIC
- **Timeout**: 1000ms

### calculator
- **Description**: Calculate simple expressions such as '1 + 2'.
- **Input**: Format: `number operator number` (e.g., `12 + 30`)
- **Operators**: `+`, `-`, `*`, `/`
- **Permission**: PUBLIC
- **Timeout**: 1000ms

### http_mock
- **Description**: Mock an HTTP API call and return a deterministic response.
- **Input**: Format: `METHOD path` (e.g., `GET /api/projects`)
- **Permission**: INTERNAL
- **Timeout**: 1500ms

## Invocation Flow
1. Client sends `POST /api/tools/{name}/invoke` with `{"input": "..."}`
2. Validate tool exists
3. Validate input format
4. Execute tool handler function
5. Record trace event with latency
6. Return `ToolResult { toolName, output }`

## Error Handling
- Unknown tool: 400 with available tool names
- Invalid input: 400 with expected format
- Timeout: 400 with timeout value
- Execution failure: 400 with error message
