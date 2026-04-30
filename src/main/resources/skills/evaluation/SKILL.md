# Evaluation Skill

## Overview
This skill defines how evaluation cases are created and used to assess answer quality.

## Evaluation Case Schema
```java
public record EvaluationCase(
    Long id,
    String question,
    List<String> expectedKeywords,
    String feedback,
    LocalDateTime createdAt
) {}
```

## Creating Evaluation Cases
- `POST /api/evaluations` with `{"question": "...", "expectedKeywords": ["..."], "feedback": "..."}`
- Cases are stored in-memory (CopyOnWriteArrayList)

## Evaluation Metrics
1. **Retrieval Hit**: Did the knowledge base return relevant chunks?
2. **Citation Presence**: Does the answer contain source references [1], [2]?
3. **Keyword Match**: Do expected keywords appear in the answer?
4. **No Unsourced Claims**: Does the answer avoid assertions not supported by sources?

## Example Evaluation Output
```json
{
  "question": "What is RAG Flow?",
  "expectedKeywords": ["retrieval", "context", "citation"],
  "retrievalHit": true,
  "citationPresent": true,
  "answerContainsExpectedKeywords": true,
  "score": 0.86
}
```

## Scoring Rules
- Score range: 0.0 to 1.0
- `retrievalHit`: 0.3 weight
- `citationPresent`: 0.3 weight
- `keywordCoverage`: 0.4 weight (ratio of matched keywords)
- Reduce score by 0.2 if unsourced claims detected

## Comparison Testing
- Run the same question with and without knowledge base
- Compare streaming vs non-streaming answer consistency
- Compare answers from different models (when multi-model support is added)

## Future Enhancements
- Automated batch evaluation against all cases
- LLM-as-judge for semantic answer quality
- Human feedback integration (thumbs up/down on answers)
- Evaluation history and trend visualization
