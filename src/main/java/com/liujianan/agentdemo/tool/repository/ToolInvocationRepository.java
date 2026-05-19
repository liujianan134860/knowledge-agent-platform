package com.liujianan.agentdemo.tool.repository;

import com.liujianan.agentdemo.tool.entity.ToolInvocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolInvocationRepository extends JpaRepository<ToolInvocation, Long> {
    List<ToolInvocation> findByRunIdOrderByCreatedAtAsc(String runId);
    List<ToolInvocation> findByUserIdOrderByCreatedAtDesc(String userId);
}
