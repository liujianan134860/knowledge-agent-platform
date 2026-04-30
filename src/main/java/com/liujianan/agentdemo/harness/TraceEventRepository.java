package com.liujianan.agentdemo.harness;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraceEventRepository extends JpaRepository<TraceEvent, Long> {
    List<TraceEvent> findByUserIdAndSessionIdOrderByCreatedAtDesc(String userId, String sessionId);
    List<TraceEvent> findByUserIdAndSessionIdOrderByCreatedAtAsc(String userId, String sessionId);
    List<TraceEvent> findByUserIdOrderByCreatedAtDesc(String userId);
}
