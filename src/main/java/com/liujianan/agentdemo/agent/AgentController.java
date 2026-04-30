package com.liujianan.agentdemo.agent;

import com.liujianan.agentdemo.common.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    @GetMapping
    public ApiResponse<List<AgentInfo>> getAgents() {
        List<AgentInfo> agents = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:agents/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;
                String name = filename.replace(".md", "");
                String content;
                try (InputStream is = resource.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    content = reader.lines().collect(Collectors.joining("\n"));
                }
                agents.add(parseAgent(name, content));
            }
        } catch (Exception e) {
            // Return empty list if directory not found
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
