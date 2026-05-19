package com.liujianan.agentdemo.agent;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liujianan.agentdemo.harness.SessionService;
import com.liujianan.agentdemo.harness.TraceAgent;
import com.liujianan.agentdemo.skill.SkillDefinition;
import com.liujianan.agentdemo.skill.SkillRegistry;
import com.liujianan.agentdemo.tool.ToolRegistry;
import com.liujianan.agentdemo.tool.entity.ToolApproval;

@Service
public class ReActExecutor {
    private static final Logger log = LoggerFactory.getLogger(ReActExecutor.class);
    private static final int DEFAULT_MAX_STEPS = 20;
    private static final int MAX_PARSE_RETRIES = 3;

    private final ChatModel chatModel;
    private final ToolRegistry toolRegistry;
    private final SkillRegistry skillRegistry;
    private final Map<String, ToolCallback> toolCallbackMap;
    private final AgentRunRepository runRepository;
    private final AgentStepRepository stepRepository;
    private final TraceAgent traceAgent;
    private final SessionService sessionService;

    public ReActExecutor(ObjectProvider<ChatModel> chatModelProvider,
                         ToolRegistry toolRegistry,
                         SkillRegistry skillRegistry,
                         AgentRunRepository runRepository,
                         AgentStepRepository stepRepository,
                         TraceAgent traceAgent,
                         SessionService sessionService) {
        this.chatModel = chatModelProvider.getIfAvailable();
        this.toolRegistry = toolRegistry;
        this.skillRegistry = skillRegistry;
        this.toolCallbackMap = toolRegistry.getCallbacks();
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.traceAgent = traceAgent;
        this.sessionService = sessionService;
        log.info("ReActExecutor initialized with {} tools: {}", toolCallbackMap.size(), toolCallbackMap.keySet());
    }

    @Transactional
    public AgentRun execute(String sessionId, String taskDescription, String userId) {
        return executeStream(sessionId, taskDescription, userId, step -> {}, approval -> {});
    }

    @Transactional
    public AgentRun executeStream(String sessionId, String taskDescription, String userId,
                                  Consumer<AgentStepResponse> stepCallback,
                                  Consumer<ToolApproval> approvalCallback) {
        if (chatModel == null) {
            throw new IllegalStateException("ChatModel not configured — cannot execute agent task");
        }

        // Match skill
        SkillDefinition skill = skillRegistry.match(taskDescription).orElse(null);
        String skillId = skill != null ? skill.getId() : null;

        String runId = UUID.randomUUID().toString();
        AgentRun run = new AgentRun(runId, sessionId, userId, AgentRunStatus.PENDING,
                taskDescription, null, null, DEFAULT_MAX_STEPS, skillId,
                LocalDateTime.now(), LocalDateTime.now());
        run = runRepository.save(run);

        String traceSessionId = sessionId != null ? sessionId : runId;
        traceAgent.record(traceSessionId, "AGENT_START",
                "Agent task: " + taskDescription,
                Map.of("runId", runId, "taskLength", taskDescription.length()), userId);

        run.setStatus(AgentRunStatus.IN_PROGRESS);
        run.setUpdatedAt(LocalDateTime.now());
        run = runRepository.save(run);

        // Set up approval callback
        toolRegistry.setApprovalCallback(approval -> {
            approval.setRunId(runId);
            approvalCallback.accept(approval);
        });

        try {
            String finalAnswer = runReActLoop(run, skill, traceSessionId, userId, stepCallback);
            run.setFinalAnswer(finalAnswer);
            run.setStatus(AgentRunStatus.COMPLETED);

            if (sessionId != null && !sessionId.isBlank()) {
                try {
                    sessionService.append(sessionId, "assistant", finalAnswer, userId);
                } catch (Exception e) {
                    log.warn("Failed to append agent answer to session {}: {}", sessionId, e.getMessage());
                }
            }

            traceAgent.record(traceSessionId, "AGENT_COMPLETE",
                    "Agent completed: " + taskDescription,
                    Map.of("runId", runId, "finalAnswerLength", finalAnswer.length()), userId);
        } catch (Exception e) {
            log.error("Agent run {} failed", runId, e);
            run.setStatus(AgentRunStatus.FAILED);
            run.setErrorMessage(e.getMessage());
            traceAgent.record(traceSessionId, "AGENT_ERROR",
                    "Agent failed: " + e.getMessage(),
                    Map.of("runId", runId, "error", e.getMessage()), userId);
        } finally {
            toolRegistry.clearApprovalCallback();
        }

        run.setUpdatedAt(LocalDateTime.now());
        return runRepository.save(run);
    }

    private String runReActLoop(AgentRun run, SkillDefinition skill, String traceSessionId, String userId,
                                Consumer<AgentStepResponse> stepCallback) {
        String systemPrompt = buildReActSystemPrompt(skill);
        List<Message> messages = new java.util.ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(run.getTaskDescription()));

        int stepIndex = 0;
        int maxSteps = run.getMaxSteps();
        int consecutiveParseFailures = 0;

        while (stepIndex < maxSteps) {
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            Generation generation = response.getResult();
            AssistantMessage assistantMessage = generation.getOutput();
            String content = assistantMessage.getText();
            if (content == null) {
                content = "";
            }

            log.debug("Agent run {} step {}: {}", run.getId(), stepIndex, truncate(content, 200));

            // Save THOUGHT step
            AgentStep thoughtStep = saveStep(run.getId(), stepIndex++, AgentStepType.THOUGHT, content,
                    null, null, null, traceSessionId, userId);
            stepCallback.accept(AgentStepResponse.from(thoughtStep));
            messages.add(assistantMessage);

            // Try to extract Action
            String toolName = extractToolName(content);
            String toolInput = extractToolInput(content);

            if (toolName != null && toolInput != null) {
                // ACTION step
                AgentStep actionStep = saveStep(run.getId(), stepIndex++, AgentStepType.ACTION,
                        "Calling tool: " + toolName,
                        toolName, toolInput, null, traceSessionId, userId);
                stepCallback.accept(AgentStepResponse.from(actionStep));

                String toolResult = executeTool(toolName, toolInput, run.getId(), userId);
                AgentStep obsStep = saveStep(run.getId(), stepIndex++, AgentStepType.OBSERVATION,
                        null,
                        toolName, null, toolResult, traceSessionId, userId);
                stepCallback.accept(AgentStepResponse.from(obsStep));

                // Feed observation back
                messages.add(new UserMessage("Observation: " + toolResult));
                consecutiveParseFailures = 0;
                continue;
            }

            // Try to extract Final answer
            String finalAnswer = extractFinal(content);
            if (finalAnswer != null) {
                AgentStep finalStep = saveStep(run.getId(), stepIndex, AgentStepType.FINAL, finalAnswer,
                        null, null, null, traceSessionId, userId);
                stepCallback.accept(AgentStepResponse.from(finalStep));
                return finalAnswer;
            }

            // Neither Action nor Final found — retry or fail
            consecutiveParseFailures++;
            if (consecutiveParseFailures >= MAX_PARSE_RETRIES) {
                AgentStep errStep = saveStep(run.getId(), stepIndex++, AgentStepType.ERROR,
                        "Model failed to produce valid Action or Final after " + MAX_PARSE_RETRIES + " attempts",
                        null, null, null, traceSessionId, userId);
                stepCallback.accept(AgentStepResponse.from(errStep));
                return "Unable to complete the task. The model did not follow the ReAct format after multiple attempts.";
            }

            AgentStep errStep = saveStep(run.getId(), stepIndex++, AgentStepType.ERROR,
                    "No Action or Final found in response (retry " + consecutiveParseFailures + "/" + MAX_PARSE_RETRIES + ")",
                    null, null, null, traceSessionId, userId);
            stepCallback.accept(AgentStepResponse.from(errStep));
            messages.add(new UserMessage(
                    "Your response did not contain a valid Action or Final. " +
                    "Please use the format: Action: tool_name\\nAction Input: JSON\\nOR: Final: your answer"));
        }

        // Max steps exceeded
        run.setStatus(AgentRunStatus.TIMEOUT);
        run.setErrorMessage("Exceeded maximum steps (" + maxSteps + ")");
        saveStep(run.getId(), stepIndex, AgentStepType.ERROR,
                "Max steps (" + maxSteps + ") exceeded", null, null, null, traceSessionId, userId);
        return "Task was not completed within the maximum number of steps (" + maxSteps + ").";
    }

    private String buildReActSystemPrompt(SkillDefinition skill) {
        StringBuilder sb = new StringBuilder();

        if (skill != null) {
            sb.append(skill.getSystemPrompt()).append("\n\n");
        }

        sb.append("""
            You are an AI assistant that runs in a loop of Thought, Action, Observation.
            Use the following format for each step:

            Thought: your reasoning about what to do next
            Action: the tool name to use
            Action Input: the JSON input for the tool
            Observation: the result from the tool
            ... (repeat Thought/Action/Observation as needed)

            When you have enough information to answer the user, output:
            Thought: I now have enough information
            Final: your final answer to the user

            IMPORTANT:
            - Always output exactly one Action OR one Final per response, not both.
            - Action Input must be a single-line JSON object.
            - Use the exact tool names listed below.
            - Think in Chinese. Answer in Chinese.

            Available tools:
            """);

        if (toolCallbackMap.isEmpty()) {
            sb.append("  (no tools available)\n");
        } else {
            for (Map.Entry<String, ToolCallback> entry : toolCallbackMap.entrySet()) {
                ToolDefinition def = entry.getValue().getToolDefinition();
                sb.append("  - name: ").append(def.name()).append("\n");
                sb.append("    description: ").append(def.description()).append("\n");
                sb.append("    input schema: ").append(def.inputSchema()).append("\n");
            }
        }

        return sb.toString();
    }

    // --- Text extraction ---

    private String extractToolName(String content) {
        if (content == null) return null;
        Pattern p = Pattern.compile("Action:\\s*(\\S+)", Pattern.MULTILINE);
        Matcher m = p.matcher(content);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractToolInput(String content) {
        if (content == null) return null;
        Pattern p = Pattern.compile("Action\\s*Input:\\s*(\\{)", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        if (!m.find()) return null;
        int start = m.start(1);
        int depth = 0;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    String extracted = content.substring(start, i + 1).trim();
                    log.debug("Extracted tool input ({} chars): {}", extracted.length(), truncate(extracted, 300));
                    return extracted;
                }
            }
        }
        log.warn("Unmatched braces in tool input starting at: {}", truncate(content.substring(start), 100));
        return null;
    }

    private String extractFinal(String content) {
        if (content == null) return null;
        Pattern p = Pattern.compile("Final:\\s*(.*)", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        return m.find() ? m.group(1).trim() : null;
    }

    // --- Tool execution ---

    private String executeTool(String toolName, String toolInput, String runId, String userId) {
        log.info("ReAct tool call: {} with input: {}", toolName, truncate(toolInput, 200));
        String result = toolRegistry.execute(toolName, toolInput, runId, userId);
        log.info("ReAct tool result: {}", truncate(result, 200));
        return result;
    }

    // --- Step persistence ---

    private AgentStep saveStep(String runId, int stepIndex, AgentStepType type, String content,
                          String toolName, String toolInput, String toolResult,
                          String traceSessionId, String userId) {
        AgentStep step = new AgentStep(null, runId, stepIndex, type, content,
                toolName, toolInput, toolResult, LocalDateTime.now());
        step = stepRepository.save(step);

        String traceMsg = switch (type) {
            case THOUGHT -> truncate(content, 200);
            case ACTION -> "Tool: " + toolName;
            case OBSERVATION -> "Observation: " + truncate(toolResult, 200);
            case FINAL -> "Final answer";
            case ERROR -> "Error: " + truncate(content, 100);
        };

        traceAgent.record(traceSessionId, "AGENT_STEP_" + type.name(),
                traceMsg,
                Map.of("stepIndex", stepIndex, "type", type.name(),
                        "toolName", toolName != null ? toolName : ""),
                userId);
        return step;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
