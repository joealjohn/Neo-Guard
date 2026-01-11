package dev.neoobfuscator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service that automatically cleans up temporary files like Skidfuscator error
 * logs.
 * Runs every 5 minutes to keep the working directory clean.
 */
@Service
public class CleanupService {

    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);

    // Clean up files older than 5 minutes
    private static final long CLEANUP_AGE_MINUTES = 5;

    /**
     * Scheduled task that runs every 5 minutes to clean up error files.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000 ms
    public void cleanupErrorFiles() {
        Path workingDir = Paths.get(System.getProperty("user.dir"));

        try {
            // Delete skidfuscator error files
            deleteMatchingFiles(workingDir, "skidfuscator-error-*.txt");

            // Also clean up old config files
            cleanupOldConfigs();

            // Clean up old mappings
            cleanupOldMappings();

        } catch (Exception e) {
            log.warn("Error during cleanup: {}", e.getMessage());
        }
    }

    private void deleteMatchingFiles(Path directory, String pattern) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, pattern)) {
            int count = 0;
            for (Path file : stream) {
                try {
                    Files.deleteIfExists(file);
                    count++;
                } catch (IOException e) {
                    log.debug("Could not delete {}: {}", file.getFileName(), e.getMessage());
                }
            }
            if (count > 0) {
                log.info("Cleaned up {} error files", count);
            }
        } catch (IOException e) {
            log.debug("Error listing files for cleanup: {}", e.getMessage());
        }
    }

    private void cleanupOldConfigs() {
        Path configsDir = Paths.get(System.getProperty("user.dir"), "configs");
        if (Files.exists(configsDir)) {
            cleanupOldFiles(configsDir, CLEANUP_AGE_MINUTES);
        }
    }

    private void cleanupOldMappings() {
        Path mappingsDir = Paths.get(System.getProperty("user.dir"), "mappings");
        if (Files.exists(mappingsDir)) {
            cleanupOldFiles(mappingsDir, CLEANUP_AGE_MINUTES);
        }
    }

    private void cleanupOldFiles(Path directory, long ageMinutes) {
        if (!Files.exists(directory))
            return;

        Instant cutoff = Instant.now().minus(ageMinutes, ChronoUnit.MINUTES);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            int count = 0;
            for (Path file : stream) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    if (attrs.creationTime().toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(file);
                        count++;
                    }
                } catch (IOException e) {
                    log.debug("Could not process {}: {}", file.getFileName(), e.getMessage());
                }
            }
            if (count > 0) {
                log.info("Cleaned up {} old files from {}", count, directory.getFileName());
            }
        } catch (IOException e) {
            log.debug("Error during old file cleanup: {}", e.getMessage());
        }
    }
}
