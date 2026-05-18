package com.liujianan.agentdemo.evaluation;

public interface JudgeModelClient {
    boolean isConfigured();

    String judge(String prompt);

    String modelName();
}
