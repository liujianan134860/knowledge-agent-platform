package com.liujianan.agentdemo.auth;

public record AuthResponse(
        String token,
        String userId,
        String username
) {
}
