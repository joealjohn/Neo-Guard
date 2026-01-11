package dev.neoobfuscator.service;

import dev.neoobfuscator.model.JobStatus;
import dev.neoobfuscator.model.ObfuscationConfig;
import dev.neoobfuscator.model.ObfuscationJob;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Core obfuscation service that integrates with Skidfuscator.
 */
@Service
public class ObfuscatorService {

    private static final Logger log = LoggerFactory.getLogger(ObfuscatorService.class);

    private final FileStorageService fileStorage;
    private final HistoryService historyService;
    private final Gson gson = new Gson();

    @Value("${neo.skidfuscator-jar:./libs/skidfuscator.jar}")
    private String skidfuscatorJar;

    private String resolvedSkidfuscatorPath;

    public ObfuscatorService(FileStorageService fileStorage, HistoryService historyService) {
        this.fileStorage = fileStorage;
        this.historyService = historyService;
    }

    /**
     * Resolve the Skidfuscator JAR path - handles both development and production
     * scenarios.
     */
    private String getSkidfuscatorPath() {
        if (resolvedSkidfuscatorPath != null) {
            return resolvedSkidfuscatorPath;
        }

        // First, check if the configured path exists (relative or absolute)
        File jarFile = new File(skidfuscatorJar);
        if (jarFile.exists()) {
            resolvedSkidfuscatorPath = jarFile.getAbsolutePath();
            log.info("Using Skidfuscator at configured path: {}", resolvedSkidfuscatorPath);
            return resolvedSkidfuscatorPath;
        }

        // Check user.dir (where the JAR was launched from) first - most reliable for
        // relative paths
        String userDir = System.getProperty("user.dir");
        log.info("user.dir is: {}", userDir);
        File userDirCandidate = new File(userDir, "libs/skidfuscator.jar");
        if (userDirCandidate.exists()) {
            resolvedSkidfuscatorPath = userDirCandidate.getAbsolutePath();
            log.info("Found Skidfuscator in user.dir: {}", resolvedSkidfuscatorPath);
            return resolvedSkidfuscatorPath;
        }

        // Try to resolve relative to the application's JAR/classes location
        try {
            // Get the path of the running JAR or classes directory
            String appPath = ObfuscatorService.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            // On Windows, remove leading slash from path like "/C:/..."
            if (appPath.matches("^/[A-Za-z]:.*")) {
                appPath = appPath.substring(1);
            }

            File appFile = new File(appPath);
            File baseDir = appFile.isDirectory() ? appFile : appFile.getParentFile();

            // Try a few levels up to find libs/skidfuscator.jar (look for pom.xml as
            // project root indicator)
            for (int i = 0; i < 6; i++) {
                if (baseDir == null)
                    break;

                // Check if this directory has libs/skidfuscator.jar
                File candidate = new File(baseDir, "libs/skidfuscator.jar");
                if (candidate.exists()) {
                    resolvedSkidfuscatorPath = candidate.getAbsolutePath();
                    log.info("Found Skidfuscator at: {}", resolvedSkidfuscatorPath);
                    return resolvedSkidfuscatorPath;
                }

                // Also check if we're in target/classes and go up to project root
                File pomFile = new File(baseDir, "pom.xml");
                if (pomFile.exists()) {
                    candidate = new File(baseDir, "libs/skidfuscator.jar");
                    if (candidate.exists()) {
                        resolvedSkidfuscatorPath = candidate.getAbsolutePath();
                        log.info("Found Skidfuscator at project root: {}", resolvedSkidfuscatorPath);
                        return resolvedSkidfuscatorPath;
                    }
                }

                baseDir = baseDir.getParentFile();
            }
        } catch (Exception e) {
            log.warn("Error resolving application path: {}", e.getMessage());
        }

        // Final fallback
        resolvedSkidfuscatorPath = new File("libs/skidfuscator.jar").getAbsolutePath();
        log.warn("Skidfuscator not found, using fallback path: {}", resolvedSkidfuscatorPath);
        return resolvedSkidfuscatorPath;
    }

    /**
     * Create and queue a new obfuscation job.
     */
    public ObfuscationJob createJob(MultipartFile file, ObfuscationConfig config) throws Exception {
        String jobId = UUID.randomUUID().toString();

        // Store uploaded file
        String inputPath = fileStorage.storeUpload(file, jobId);
        String outputPath = fileStorage.getOutputPath(jobId, file.getOriginalFilename());

        // Save config to file
        String configContent = config.toHocon();
        fileStorage.saveConfig(jobId, configContent);

        // Create job record
        ObfuscationJob job = ObfuscationJob.builder()
                .id(jobId)
                .originalFilename(file.getOriginalFilename())
                .status(JobStatus.PENDING)
                .configJson(gson.toJson(config))
                .createdAt(LocalDateTime.now())
                .inputPath(inputPath)
                .outputPath(outputPath)
                .build();

        historyService.save(job);

        log.info("Created obfuscation job: {} for file: {}", jobId, file.getOriginalFilename());

        return job;
    }

    /**
     * Execute obfuscation asynchronously.
     */
    @Async("obfuscationExecutor")
    public void executeObfuscation(String jobId) {
        log.info("Starting obfuscation for job: {}", jobId);

        Optional<ObfuscationJob> optJob = historyService.findById(jobId);
        if (optJob.isEmpty()) {
            log.error("Job not found: {}", jobId);
            return;
        }

        ObfuscationJob job = optJob.get();
        historyService.updateStatus(jobId, JobStatus.PROCESSING, null);

        StringBuilder logs = new StringBuilder();

        try {
            // Build command
            List<String> command = buildCommand(job);
            log.info("Executing: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.directory(new File("."));

            Process process = pb.start();

            // Capture output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.append(line).append("\n");
                    log.debug("[Skidfuscator] {}", line);

                    // Update logs periodically
                    if (logs.length() % 1000 == 0) {
                        historyService.updateLogs(jobId, logs.toString());
                    }
                }
            }

            // Wait for completion with timeout
            boolean completed = process.waitFor(10, TimeUnit.MINUTES);

            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("Obfuscation timed out after 10 minutes");
            }

            int exitCode = process.exitValue();
            historyService.updateLogs(jobId, logs.toString());

            if (exitCode == 0 && new File(job.getOutputPath()).exists()) {
                historyService.updateStatus(jobId, JobStatus.COMPLETED, null);
                log.info("Obfuscation completed successfully for job: {}", jobId);
            } else {
                String error = "Skidfuscator exited with code: " + exitCode;
                historyService.updateStatus(jobId, JobStatus.FAILED, error);
                log.error("Obfuscation failed for job: {} - {}", jobId, error);
            }

        } catch (Exception e) {
            log.error("Obfuscation error for job: {}", jobId, e);
            historyService.updateLogs(jobId, logs.toString());
            historyService.updateStatus(jobId, JobStatus.FAILED, e.getMessage());
        }
    }

    /**
     * Build the Skidfuscator command.
     */
    private List<String> buildCommand(ObfuscationJob job) {
        List<String> cmd = new ArrayList<>();

        // Java executable
        cmd.add("java");

        // JVM args
        cmd.add("-Xmx2G");
        cmd.add("-Dterminal.jline=false");
        cmd.add("-Dterminal.ansi=true");

        // Skidfuscator JAR
        cmd.add("-jar");
        cmd.add(getSkidfuscatorPath());

        // Command
        cmd.add("obfuscate");

        // Input file
        cmd.add(job.getInputPath());

        // Output file
        cmd.add("-o=" + job.getOutputPath());

        // Config file
        String configPath = fileStorage.getConfigPath(job.getId());
        cmd.add("-cfg=" + configPath);

        return cmd;
    }

    /**
     * Get job status.
     */
    public Optional<ObfuscationJob> getJob(String jobId) {
        return historyService.findById(jobId);
    }

    /**
     * Get recent jobs.
     */
    public List<ObfuscationJob> getRecentJobs(int limit) {
        return historyService.findRecent(limit);
    }

    /**
     * Get all jobs.
     */
    public List<ObfuscationJob> getAllJobs() {
        return historyService.findAll();
    }

    /**
     * Get available transformers.
     */
    public List<TransformerInfo> getTransformers() {
        List<TransformerInfo> transformers = new ArrayList<>();

        transformers.add(new TransformerInfo("stringEncryption", "String Encryption",
                "Encrypts all string literals to prevent static analysis", true));
        transformers.add(new TransformerInfo("numberEncryption", "Number Encryption",
                "Encrypts numeric constants for additional protection", true));
        transformers.add(new TransformerInfo("flowCondition", "Flow Condition",
                "Adds opaque predicates to obfuscate control flow", true));
        transformers.add(new TransformerInfo("flowException", "Flow Exception",
                "Uses exception handling to obscure program flow", true));
        transformers.add(new TransformerInfo("flowRange", "Flow Range",
                "Range-based flow obfuscation for complex logic", true));
        transformers.add(new TransformerInfo("flowSwitch", "Flow Switch",
                "Switch-based flow flattening technique", true));
        transformers.add(new TransformerInfo("ahegao", "Ahegao Mode",
                "Adds trolling elements to deter casual inspection", false));

        return transformers;
    }

    public record TransformerInfo(String id, String name, String description, boolean defaultEnabled) {
    }
}
