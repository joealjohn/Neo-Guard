package dev.neoobfuscator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents an obfuscation job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObfuscationJob {
    private String id;
    private String originalFilename;
    private JobStatus status;
    private String configJson;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String inputPath;
    private String outputPath;
    private String errorMessage;
    private String logs;
}
