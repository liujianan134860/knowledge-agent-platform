package com.liujianan.agentdemo.skill;

import com.liujianan.agentdemo.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    @GetMapping
    public ApiResponse<List<SkillInfo>> getSkills() {
        List<SkillInfo> skills = new ArrayList<>();
        Path skillsDir = Path.of(".claude", "skills");
        if (Files.exists(skillsDir)) {
            try (var stream = Files.list(skillsDir)) {
                List<Path> skillDirs = stream.filter(Files::isDirectory).sorted().toList();
                for (Path dir : skillDirs) {
                    Path skillFile = dir.resolve("SKILL.md");
                    if (Files.exists(skillFile)) {
                        String content = Files.readString(skillFile);
                        String name = dir.getFileName().toString();
                        String displayName = extractHeading(content, name);
                        String overview = extractSection(content, "Overview");
                        skills.add(new SkillInfo(name, displayName, overview, content));
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }
        return ApiResponse.ok(skills);
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
