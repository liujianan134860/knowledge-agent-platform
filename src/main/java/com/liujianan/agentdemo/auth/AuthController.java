package com.liujianan.agentdemo.auth;

import com.liujianan.agentdemo.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(userService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request, HttpServletRequest servletRequest) {
        String authHeader = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null;
        String userId = String.valueOf(servletRequest.getAttribute("userId"));
        userService.logout(accessToken, request, userId);
        return ApiResponse.ok(null);
    }
}
