package com.liujianan.agentdemo.auth;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository,
                                   RevokedAccessTokenRepository revokedAccessTokenRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            JwtUtil.TokenClaims claims = jwtUtil.parseToken(token);
            if (claims.tokenId() != null && revokedAccessTokenRepository.existsById(claims.tokenId())) {
                throw new JwtException("revoked token");
            }
            User user = userRepository.findById(claims.userId()).orElseThrow(() -> new JwtException("unknown user"));
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new JwtException("disabled user");
            }
            request.setAttribute("userId", claims.userId());
            request.setAttribute("username", claims.username());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    claims.userId(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException e) {
            request.setAttribute("authError", "token expired");
        } catch (JwtException | IllegalArgumentException e) {
            request.setAttribute("authError", "invalid token");
        }

        filterChain.doFilter(request, response);
    }
}
