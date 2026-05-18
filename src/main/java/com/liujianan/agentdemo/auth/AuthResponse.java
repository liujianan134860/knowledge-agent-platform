package com.liujianan.agentdemo.auth;

public record AuthResponse(
        String token,
        String refreshToken,
        String userId,
        String username,
        String role
) {
}
