package com.liujianan.agentdemo.skill;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SkillRegistry {
    private static final Logger log = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

    public SkillRegistry(SkillLoader loader) {
        List<SkillDefinition> loaded = loader.loadAll();
        for (SkillDefinition def : loaded) {
            skills.put(def.getId(), def);
        }
        log.info("SkillRegistry initialized with {} skills: {}", skills.size(), skills.keySet());
    }

    /**
     * Match a task description to a skill by keyword/phrase matching.
     * Returns the skill with the longest matching trigger, or empty if none match.
     */
    public Optional<SkillDefinition> match(String taskDescription) {
        if (taskDescription == null || taskDescription.isBlank()) return Optional.empty();
        String lower = taskDescription.toLowerCase();

        SkillDefinition best = null;
        int bestLength = 0;

        for (SkillDefinition def : skills.values()) {
            for (String trigger : def.getTriggers()) {
                if (lower.contains(trigger.toLowerCase()) && trigger.length() > bestLength) {
                    best = def;
                    bestLength = trigger.length();
                }
            }
        }

        if (best != null) {
            log.info("Skill matched: {} (trigger length: {})", best.getId(), bestLength);
        }
        return Optional.ofNullable(best);
    }

    public SkillDefinition get(String id) {
        return skills.get(id);
    }

    public List<SkillDefinition> listAll() {
        return List.copyOf(skills.values());
    }

    public Map<String, SkillDefinition> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public int count() {
        return skills.size();
    }
}
