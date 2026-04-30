# Trace Review Skill

## Overview
This skill defines how execution traces are recorded, stored, and reviewed for debugging and observability.

## Trace Event Schema
```java
public record TraceEvent(
    Long id,
    String sessionId,
    String stage,
    String message,
    Map<String, Object> attributes,
    LocalDateTime createdAt
) {}
```

## Trace Stages
| Stage | Trigger | Key Attributes |
|-------|---------|---------------|
| `USER_INPUT` | Question received | `questionLength` |
| `SESSION_MEMORY` | Session history loaded | `historyMessageCount` |
| `RETRIEVAL` | Knowledge search executed | `topK`, `hitCount`, `scores` |
| `CONTEXT_BUILD` | Prompt constructed | `sourceCount`, `totalContextLength` |
| `TOOL_PLAN` | Tool selection decision | `toolName`, `confidence` |
| `TOOL_CALL` | Tool execution | `toolName`, `latencyMs`, `status` |
| `ANSWER_GENERATION` | LLM response generation | `latencyMs`, `promptTokens`, `llmConfigured` |
| `QA_REVIEW` | Answer quality check | `retrievalHit`, `citationPresent`, `score` |
| `EVALUATION` | Evaluation case matching | `expectedKeywords`, `matches` |

## Trace Querying
- `GET /api/traces` — list all trace events (optionally filtered by `?sessionId=xxx`)
- Events are stored in-memory (CopyOnWriteArrayList), ordered by creation time descending

## Review Checklist
1. Was `USER_INPUT` recorded for every request?
2. Did `RETRIEVAL` return the expected number of chunks?
3. Did `ANSWER_GENERATION` succeed or fall back?
4. Were tool calls traced with latency and status?
5. Are there gaps between expected stages?

## Future Enhancements
- Add `parentTraceId` for nested/cascaded traces
- Add `runId` to group all stages of a single QA run
- Add `durationMs` field for stage-level timing
- Add `errorMessage` field for failure diagnosis
- Persist traces to a database for historical analysis
