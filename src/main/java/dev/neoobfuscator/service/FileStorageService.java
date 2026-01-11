package dev.neoobfuscator.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service for file storage operations.
 */
@Service
public class FileStorageService {

    @Value("${neo.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${neo.output-dir:./output}")
    private String outputDir;

    @Value("${neo.config-dir:./configs}")
    private String configDir;

    @PostConstruct
    public void init() throws IOException {
        // Create absolute paths
        uploadPath = Paths.get(uploadDir).toAbsolutePath();
        outputPath = Paths.get(outputDir).toAbsolutePath();
        configPath = Paths.get(configDir).toAbsolutePath();

        // Create directories if they don't exist
        Files.createDirectories(uploadPath);
        Files.createDirectories(outputPath);
        Files.createDirectories(configPath);
        Files.createDirectories(Paths.get("./data").toAbsolutePath());
        Files.createDirectories(Paths.get("./libs").toAbsolutePath());
    }

    private Path uploadPath;
    private Path outputPath;
    private Path configPath;

    /**
     * Store uploaded file and return the path.
     */
    public String storeUpload(MultipartFile file, String jobId) throws IOException {
        String filename = jobId + "_" + sanitizeFilename(file.getOriginalFilename());
        Path path = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), path);
        return path.toAbsolutePath().toString();
    }

    /**
     * Get output file path for a job.
     */
    public String getOutputPath(String jobId, String originalFilename) {
        String baseName = originalFilename.replace(".jar", "");
        String filename = jobId + "_" + baseName + "-obfuscated.jar";
        return outputPath.resolve(filename).toString();
    }

    /**
     * Get config file path for a job.
     */
    public String getConfigPath(String jobId) {
        return configPath.resolve(jobId + ".hocon").toString();
    }

    /**
     * Save config content to file.
     */
    public void saveConfig(String jobId, String content) throws IOException {
        Path path = Paths.get(getConfigPath(jobId));
        Files.writeString(path, content);
    }

    /**
     * Get file by path.
     */
    public File getFile(String path) {
        return new File(path);
    }

    /**
     * Check if output file exists.
     */
    public boolean outputExists(String jobId, String originalFilename) {
        return Files.exists(Paths.get(getOutputPath(jobId, originalFilename)));
    }

    /**
     * Delete files for a job.
     */
    public void deleteJobFiles(String jobId, String originalFilename) throws IOException {
        // Delete input
        Files.deleteIfExists(uploadPath.resolve(jobId + "_" + sanitizeFilename(originalFilename)));

        // Delete output
        String baseName = originalFilename.replace(".jar", "");
        Files.deleteIfExists(outputPath.resolve(jobId + "_" + baseName + "-obfuscated.jar"));

        // Delete config
        Files.deleteIfExists(configPath.resolve(jobId + ".hocon"));
    }

    private String sanitizeFilename(String filename) {
        if (filename == null)
            return "unknown.jar";
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
