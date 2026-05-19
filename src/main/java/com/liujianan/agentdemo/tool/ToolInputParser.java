package com.liujianan.agentdemo.tool;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared JSON parsing for built-in tools. Unwraps Spring AI MethodToolCallback
 * "input" parameter when present.
 */
public final class ToolInputParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolInputParser() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(String raw) {
        if (raw == null || raw.isBlank()) return new LinkedHashMap<>();
        try {
            Map<String, Object> params = MAPPER.readValue(raw, LinkedHashMap.class);
            // Unwrap: MethodToolCallback wraps the String param as {"input": "..."}
            if (params.size() == 1 && params.containsKey("input")
                    && params.get("input") instanceof String s) {
                return MAPPER.readValue(s, LinkedHashMap.class);
            }
            return params;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
