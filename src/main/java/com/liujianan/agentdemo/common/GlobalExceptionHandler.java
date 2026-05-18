package com.liujianan.agentdemo.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("invalid request");
        return ApiResponse.fail("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail("BAD_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleUploadSize() {
        return ApiResponse.fail("UPLOAD_TOO_LARGE", "uploaded file is larger than the 20MB limit");
    }

    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleJwt(io.jsonwebtoken.JwtException exception) {
        return ApiResponse.fail("INVALID_TOKEN", "invalid token");
    }
}
