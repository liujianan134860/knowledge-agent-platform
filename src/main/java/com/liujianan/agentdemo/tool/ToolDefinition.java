package com.liujianan.agentdemo.tool;

public class ToolDefinition {
    private String name;
    private String description;
    private ToolRiskLevel riskLevel;
    private String category;
    private long timeoutMs;

    public ToolDefinition() {
    }

    public ToolDefinition(String name, String description, ToolRiskLevel riskLevel, String category, long timeoutMs) {
        this.name = name;
        this.description = description;
        this.riskLevel = riskLevel;
        this.category = category;
        this.timeoutMs = timeoutMs;
    }

    public static ToolDefinition of(String name, String description, ToolRiskLevel riskLevel, String category) {
        return new ToolDefinition(name, description, riskLevel, category, 30000);
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public ToolRiskLevel getRiskLevel() { return riskLevel; }
    public String getCategory() { return category; }
    public long getTimeoutMs() { return timeoutMs; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setRiskLevel(ToolRiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public void setCategory(String category) { this.category = category; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }

    public String name() { return name; }
    public String description() { return description; }
    public ToolRiskLevel riskLevel() { return riskLevel; }
    public String category() { return category; }
    public long timeoutMs() { return timeoutMs; }
}
