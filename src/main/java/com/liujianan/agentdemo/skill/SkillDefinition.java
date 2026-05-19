package com.liujianan.agentdemo.skill;

import java.util.List;

public class SkillDefinition {
    private String id;
    private String name;
    private String version;
    private String description;
    private List<String> triggers;
    private String systemPrompt;

    public SkillDefinition() {
    }

    public SkillDefinition(String id, String name, String version, String description,
                           List<String> triggers, String systemPrompt) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.triggers = triggers;
        this.systemPrompt = systemPrompt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getTriggers() { return triggers; }
    public void setTriggers(List<String> triggers) { this.triggers = triggers; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    // Record-style accessors for compatibility
    public String id() { return id; }
    public String name() { return name; }
    public String version() { return version; }
    public String description() { return description; }
    public List<String> triggers() { return triggers; }
    public String systemPrompt() { return systemPrompt; }
}
