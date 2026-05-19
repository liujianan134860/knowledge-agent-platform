package com.liujianan.agentdemo.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentRunRepository extends JpaRepository<AgentRun, String> {
    List<AgentRun> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<AgentRun> findByIdAndUserId(String id, String userId);
}
