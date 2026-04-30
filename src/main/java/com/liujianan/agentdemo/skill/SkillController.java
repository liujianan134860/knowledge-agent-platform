package com.liujianan.agentdemo.skill;

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
@RequestMapping("/api/skills")
public class SkillController {

    @GetMapping
    public ApiResponse<List<SkillInfo>> getSkills() {
        List<SkillInfo> skills = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:skills/*/SKILL.md");
            for (Resource resource : resources) {
                String content;
                try (InputStream is = resource.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    content = reader.lines().collect(Collectors.joining("\n"));
                }
                // Extract name from path: classpath:skills/{name}/SKILL.md
                String url = resource.getURL().toString();
                String name = extractSkillName(url);
                String displayName = extractHeading(content, name);
                String overview = extractSection(content, "Overview");
                skills.add(new SkillInfo(name, displayName, overview, content));
            }
        } catch (Exception e) {
            // Return empty list if directory not found
        }
        return ApiResponse.ok(skills);
    }

    private String extractSkillName(String url) {
        // URL pattern: .../skills/{name}/SKILL.md
        int skillsIdx = url.indexOf("/skills/");
        if (skillsIdx < 0) return "unknown";
        String afterSkills = url.substring(skillsIdx + 8);
        int slashIdx = afterSkills.indexOf('/');
        if (slashIdx < 0) return "unknown";
        return afterSkills.substring(0, slashIdx);
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
        return content.substring(start, end).trim();
    }
}
