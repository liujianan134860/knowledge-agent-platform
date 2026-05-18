package com.liujianan.agentdemo.harness;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TraceEventRepository extends JpaRepository<TraceEvent, Long> {
    List<TraceEvent> findByUserIdAndSessionIdOrderByCreatedAtDesc(String userId, String sessionId);
    List<TraceEvent> findByUserIdAndSessionIdOrderByCreatedAtAsc(String userId, String sessionId);
    List<TraceEvent> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<TraceEvent> findByUserIdAndSessionId(String userId, String sessionId, Pageable pageable);
    Page<TraceEvent> findByUserId(String userId, Pageable pageable);
}
