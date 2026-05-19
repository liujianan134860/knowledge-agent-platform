package com.liujianan.agentdemo.tool.repository;

import com.liujianan.agentdemo.tool.entity.ToolApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ToolApprovalRepository extends JpaRepository<ToolApproval, Long> {
    List<ToolApproval> findByRunIdAndStatus(String runId, String status);
    List<ToolApproval> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);
    Optional<ToolApproval> findByIdAndUserId(Long id, String userId);
}
