package com.liujianan.agentdemo.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    List<RefreshToken> findByUserIdAndRevokedAtIsNull(String userId);
}
