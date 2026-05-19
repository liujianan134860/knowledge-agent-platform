package com.liujianan.agentdemo.tool.service;

import com.liujianan.agentdemo.tool.entity.PersonalMemory;
import com.liujianan.agentdemo.tool.repository.PersonalMemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MemoryService {
    private final PersonalMemoryRepository memoryRepository;

    public MemoryService(PersonalMemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @Transactional
    public PersonalMemory remember(String userId, String key, String value, String category) {
        Optional<PersonalMemory> existing = memoryRepository.findByUserIdAndKey(userId, key);
        LocalDateTime now = LocalDateTime.now();
        if (existing.isPresent()) {
            PersonalMemory mem = existing.get();
            mem.setValue(value);
            if (category != null) mem.setCategory(category);
            mem.setUpdatedAt(now);
            return memoryRepository.save(mem);
        }
        PersonalMemory mem = new PersonalMemory(null, userId, key, value, category, now, now);
        return memoryRepository.save(mem);
    }

    public Optional<PersonalMemory> recall(String userId, String key) {
        return memoryRepository.findByUserIdAndKey(userId, key);
    }

    public List<PersonalMemory> recallByCategory(String userId, String category) {
        return memoryRepository.findByUserIdAndCategory(userId, category);
    }

    public List<PersonalMemory> listAll(String userId) {
        return memoryRepository.findByUserId(userId);
    }

    @Transactional
    public void forget(String userId, String key) {
        memoryRepository.deleteByUserIdAndKey(userId, key);
    }
}
