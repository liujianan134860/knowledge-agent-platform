package com.liujianan.agentdemo.skill;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class SkillLoader {
    private static final Logger log = LoggerFactory.getLogger(SkillLoader.class);
    private static final String SKILL_LOCATION = "classpath:skills/*/skill.yml";

    private final ResourcePatternResolver resourceResolver;

    public SkillLoader(ResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @SuppressWarnings("unchecked")
    public List<SkillDefinition> loadAll() {
        List<SkillDefinition> skills = new ArrayList<>();
        try {
            Resource[] resources = resourceResolver.getResources(SKILL_LOCATION);
            Yaml yaml = new Yaml();
            for (Resource res : resources) {
                try (InputStream in = res.getInputStream()) {
                    Map<String, Object> data = yaml.load(in);
                    if (data == null) continue;
                    SkillDefinition def = new SkillDefinition();
                    def.setId((String) data.get("id"));
                    def.setName((String) data.get("name"));
                    def.setVersion((String) data.getOrDefault("version", "1.0"));
                    def.setDescription((String) data.get("description"));
                    def.setSystemPrompt((String) data.get("systemPrompt"));
                    Object triggersObj = data.get("triggers");
                    if (triggersObj instanceof List<?> list) {
                        def.setTriggers(list.stream().map(Object::toString).toList());
                    } else {
                        def.setTriggers(List.of());
                    }
                    skills.add(def);
                    log.info("Loaded skill: {} ({} triggers)", def.getId(), def.getTriggers().size());
                } catch (Exception e) {
                    log.warn("Failed to load skill from {}: {}", res.getFilename(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("No skills found at {}: {}", SKILL_LOCATION, e.getMessage());
        }
        return skills;
    }
}
