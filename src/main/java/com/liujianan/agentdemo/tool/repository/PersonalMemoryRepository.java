package com.liujianan.agentdemo.tool.repository;

import com.liujianan.agentdemo.tool.entity.PersonalMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalMemoryRepository extends JpaRepository<PersonalMemory, Long> {
    Optional<PersonalMemory> findByUserIdAndKey(String userId, String key);
    List<PersonalMemory> findByUserIdAndCategory(String userId, String category);
    List<PersonalMemory> findByUserId(String userId);
    void deleteByUserIdAndKey(String userId, String key);
}
