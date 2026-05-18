package com.liujianan.agentdemo.common;

import java.time.Instant;

public record ApiResponse<T>(boolean success, String code, String message, T data, String requestId, Instant timestamp) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "ok", data, RequestIdFilter.currentRequestId(), Instant.now());
    }

    public static <T> ApiResponse<T> fail(String message) {
        return fail("COMMON_ERROR", message);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null, RequestIdFilter.currentRequestId(), Instant.now());
    }
}
