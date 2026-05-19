package com.liujianan.agentdemo.tool.builtin;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.liujianan.agentdemo.tool.ToolInputParser;

@Service
public class FileReadTool {
    private static final Logger log = LoggerFactory.getLogger(FileReadTool.class);

    private static final Set<String> BLOCKED_NAMES = Set.of(
            ".env", "credentials", ".key", ".pem", ".p12", ".pfx", "id_rsa",
            "id_ed25519", "known_hosts", "authorized_keys");
    private static final List<String> BLOCKED_PREFIXES = List.of(
            "credentials", "secret", ".env");
    private static final List<String> BLOCKED_EXTENSIONS = List.of(
            ".key", ".pem", ".p12", ".pfx", ".jks", ".keystore", ".der");
    private static final long BINARY_CHECK_BYTES = 8192;
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final Path basePath;
    private final long maxFileSize;

    public FileReadTool(@Value("${filesystem.base-path:./data}") String basePathStr,
                        @Value("${filesystem.max-file-size:10485760}") long maxFileSize) {
        this.basePath = Paths.get(basePathStr).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;
        log.info("FileReadTool initialized: basePath={}, maxFileSize={}", this.basePath, this.maxFileSize);
    }

    @Tool(name = "file.read", description = "Read a file from the local filesystem. " +
            "Only files within the configured base path are accessible. " +
            "Input JSON: {\"path\":\"relative/path/to/file.txt\"}")
    public String read(String input) {
        try {
            Map<String, Object> params = ToolInputParser.parse(input);
            String relativePath = (String) params.get("path");
            if (relativePath == null || relativePath.isBlank()) {
                return "Error: 'path' is required";
            }

            Path resolved = basePath.resolve(relativePath).normalize();
            if (!resolved.startsWith(basePath)) {
                log.warn("Path traversal attempt blocked: requested={}, resolved={}", relativePath, resolved);
                return "Error: access denied — path is outside the allowed base directory";
            }

            if (!Files.exists(resolved)) {
                return "Error: file not found: " + relativePath;
            }
            if (!Files.isReadable(resolved)) {
                return "Error: file is not readable: " + relativePath;
            }
            if (Files.isDirectory(resolved)) {
                return "Error: path points to a directory, not a file: " + relativePath;
            }

            String fileName = resolved.getFileName().toString().toLowerCase();
            if (isBlocked(fileName)) {
                log.warn("Blocked sensitive file access: {}", resolved);
                return "Error: access denied — sensitive file type is blocked";
            }

            long fileSize = Files.size(resolved);
            if (fileSize > maxFileSize) {
                return String.format("Error: file too large (%d bytes, max %d bytes)", fileSize, maxFileSize);
            }

            if (isBinary(resolved)) {
                return "Error: file appears to be binary, not a text file";
            }

            String content = Files.readString(resolved, StandardCharsets.UTF_8);
            long lines = content.lines().count();

            return String.format("""
                    File: %s
                    Size: %d bytes
                    Lines: %d
                    --- Content Start ---
                    %s
                    --- Content End ---""",
                    relativePath, fileSize, lines, content);
        } catch (IOException e) {
            log.error("file.read IO error", e);
            return "Error reading file: " + e.getMessage();
        } catch (Exception e) {
            log.error("file.read failed", e);
            return "Error: " + e.getMessage();
        }
    }

    private boolean isBlocked(String fileName) {
        if (BLOCKED_NAMES.contains(fileName)) return true;
        for (String prefix : BLOCKED_PREFIXES) {
            if (fileName.startsWith(prefix)) return true;
        }
        for (String ext : BLOCKED_EXTENSIONS) {
            if (fileName.endsWith(ext)) return true;
        }
        return false;
    }

    private boolean isBinary(Path path) {
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            byte[] buf = new byte[(int) Math.min(BINARY_CHECK_BYTES, raf.length())];
            raf.readFully(buf);
            for (byte b : buf) {
                if (b == 0) return true;
            }
            return false;
        } catch (IOException e) {
            return true;
        }
    }
}
