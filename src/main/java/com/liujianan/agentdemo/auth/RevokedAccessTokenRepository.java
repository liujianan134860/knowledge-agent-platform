package com.liujianan.agentdemo.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, String> {
}
