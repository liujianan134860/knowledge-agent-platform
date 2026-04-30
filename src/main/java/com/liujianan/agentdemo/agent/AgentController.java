package com.liujianan.agentdemo.agent;

import com.liujianan.agentdemo.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    @GetMapping
    public ApiResponse<List<AgentInfo>> getAgents() {
        List<AgentInfo> agents = new ArrayList<>();
        Path agentsDir = Path.of(".claude", "agents");
        if (Files.exists(agentsDir)) {
            try (var stream = Files.list(agentsDir)) {
                List<Path> files = stream.filter(p -> p.toString().endsWith(".md")).sorted().toList();
                for (Path file : files) {
                    String content = Files.readString(file);
                    String name = file.getFileName().toString().replace(".md", "");
                    AgentInfo info = parseAgent(name, content);
                    agents.add(info);
                }
            } catch (IOException e) {
                // ignore
            }
        }
        return ApiResponse.ok(agents);
    }

    private AgentInfo parseAgent(String name, String content) {
        String displayName = extractHeading(content, name);
        String role = extractRole(content, displayName);
        String responsibilities = extractSection(content, "Responsibilities");
        String inputSummary = extractSection(content, "Input");
        String outputSummary = extractSection(content, "Output");
        return new AgentInfo(name, displayName, role, responsibilities, inputSummary, outputSummary);
    }

    private String extractRole(String content, String displayName) {
        // Try to find the role description: first sentence after the heading
        String[] lines = content.split("\n");
        boolean inHeading = true;
        for (String line : lines) {
            if (inHeading && line.startsWith("# ")) {
                inHeading = false;
                continue;
            }
            if (!inHeading && !line.isBlank() && !line.startsWith("#")) {
                String role = line.trim();
                if (role.length() > 60) role = role.substring(0, 60) + "...";
                return role;
            }
        }
        return displayName;
    }

    private String extractHeading(String content, String defaultName) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("# ")) {
                return line.substring(2).trim();
            }
        }
        return defaultName;
    }

    private String extractSection(String content, String sectionName) {
        String marker = "## " + sectionName;
        int start = content.indexOf(marker);
        if (start < 0) return "";
        start = content.indexOf('\n', start);
        if (start < 0) return "";
        int end = content.indexOf("## ", start + 1);
        if (end < 0) end = content.length();
        String section = content.substring(start, end).trim();
        return section.lines()
                .map(line -> line.replaceFirst("^-\\s+", "").trim())
                .filter(l -> !l.isEmpty())
                .collect(Collectors.joining("\n"));
    }
}
