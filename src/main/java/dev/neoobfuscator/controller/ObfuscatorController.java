package dev.neoobfuscator.controller;

import dev.neoobfuscator.model.ObfuscationConfig;
import dev.neoobfuscator.model.ObfuscationJob;
import dev.neoobfuscator.service.FileStorageService;
import dev.neoobfuscator.service.ObfuscatorService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * REST API controller for obfuscation operations.
 */
@RestController
@RequestMapping("/api")
public class ObfuscatorController {

    private static final Logger log = LoggerFactory.getLogger(ObfuscatorController.class);

    private final ObfuscatorService obfuscatorService;
    private final FileStorageService fileStorage;
    private final Gson gson = new Gson();

    public ObfuscatorController(ObfuscatorService obfuscatorService, FileStorageService fileStorage) {
        this.obfuscatorService = obfuscatorService;
        this.fileStorage = fileStorage;
    }

    /**
     * Upload and start obfuscation job.
     */
    @PostMapping("/obfuscate")
    public ResponseEntity<Map<String, Object>> obfuscate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "config", required = false) String configJson) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("error", "No file uploaded");
                return ResponseEntity.badRequest().body(response);
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".jar")) {
                response.put("success", false);
                response.put("error", "Only .jar files are supported");
                return ResponseEntity.badRequest().body(response);
            }

            // Parse config or use defaults
            ObfuscationConfig config;
            if (configJson != null && !configJson.isEmpty()) {
                config = gson.fromJson(configJson, ObfuscationConfig.class);
            } else {
                config = ObfuscationConfig.builder().build();
            }

            // Create job
            ObfuscationJob job = obfuscatorService.createJob(file, config);

            // Start async processing
            obfuscatorService.executeObfuscation(job.getId());

            response.put("success", true);
            response.put("jobId", job.getId());
            response.put("status", job.getStatus().name());
            response.put("message", "Obfuscation job started");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error starting obfuscation", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get job status.
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String jobId) {
        Map<String, Object> response = new HashMap<>();

        Optional<ObfuscationJob> optJob = obfuscatorService.getJob(jobId);

        if (optJob.isEmpty()) {
            response.put("success", false);
            response.put("error", "Job not found");
            return ResponseEntity.notFound().build();
        }

        ObfuscationJob job = optJob.get();
        response.put("success", true);
        response.put("jobId", job.getId());
        response.put("status", job.getStatus().name());
        response.put("originalFilename", job.getOriginalFilename());
        response.put("createdAt", job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);
        response.put("completedAt", job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
        response.put("errorMessage", job.getErrorMessage());
        response.put("logs", job.getLogs());

        if (job.getStatus().name().equals("COMPLETED")) {
            response.put("downloadUrl", "/api/download/" + jobId);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Download obfuscated file.
     */
    @GetMapping("/download/{jobId}")
    public ResponseEntity<Resource> download(@PathVariable String jobId) {
        Optional<ObfuscationJob> optJob = obfuscatorService.getJob(jobId);

        if (optJob.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ObfuscationJob job = optJob.get();
        File outputFile = fileStorage.getFile(job.getOutputPath());

        if (!outputFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        String downloadName = job.getOriginalFilename().replace(".jar", "-obfuscated.jar");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .body(new FileSystemResource(outputFile));
    }

    /**
     * Get obfuscation history.
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {

        Map<String, Object> response = new HashMap<>();
        List<ObfuscationJob> jobs = obfuscatorService.getRecentJobs(limit);

        response.put("success", true);
        response.put("jobs", jobs.stream().map(this::jobToMap).toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get available transformers.
     */
    @GetMapping("/transformers")
    public ResponseEntity<Map<String, Object>> getTransformers() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("transformers", obfuscatorService.getTransformers());
        return ResponseEntity.ok(response);
    }

    /**
     * Analyze JAR file to detect main package.
     * Tries plugin.yml, paper-plugin.yml, bungee.yml, MANIFEST.MF first.
     * Falls back to scanning all class files to find the most common package
     * prefix.
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeJar(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty() || !file.getOriginalFilename().toLowerCase().endsWith(".jar")) {
                response.put("success", false);
                response.put("error", "Invalid JAR file");
                return ResponseEntity.badRequest().body(response);
            }

            String mainPackage = "";
            String mainClass = "";
            List<String> allPackages = new ArrayList<>();

            try (InputStream is = file.getInputStream();
                    JarInputStream jis = new JarInputStream(is)) {

                // First check MANIFEST.MF for Main-Class
                java.util.jar.Manifest manifest = jis.getManifest();
                if (manifest != null) {
                    String manifestMain = manifest.getMainAttributes().getValue("Main-Class");
                    if (manifestMain != null && !manifestMain.isEmpty()) {
                        mainClass = manifestMain;
                    }
                }

                // Scan all entries
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    String name = entry.getName();

                    // Check for plugin.yml (Bukkit plugins)
                    if (name.equals("plugin.yml") || name.equals("paper-plugin.yml") || name.equals("bungee.yml")) {
                        StringBuilder content = new StringBuilder();
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = jis.read(buffer)) > 0) {
                            content.append(new String(buffer, 0, len));
                        }

                        // Parse main class from yml
                        String yml = content.toString();
                        for (String line : yml.split("\n")) {
                            line = line.trim();
                            if (line.startsWith("main:")) {
                                mainClass = line.substring(5).trim();
                                mainClass = mainClass.replace("\"", "").replace("'", "");
                                break;
                            }
                        }
                    }

                    // Collect all class file packages (skip common library packages)
                    if (name.endsWith(".class") && !entry.isDirectory()) {
                        // Convert path to package name
                        String className = name.replace("/", ".").replace(".class", "");

                        // Skip common library/framework packages (generic for all Java apps)
                        if (className.startsWith("META-INF.") ||
                                className.startsWith("org.apache.") ||
                                className.startsWith("org.slf4j.") ||
                                className.startsWith("org.log4j.") ||
                                className.startsWith("com.google.") ||
                                className.startsWith("org.jetbrains.") ||
                                className.startsWith("kotlin.") ||
                                className.startsWith("org.intellij.") ||
                                className.startsWith("io.netty.") ||
                                className.startsWith("com.fasterxml.") ||
                                className.startsWith("org.objectweb.") ||
                                className.startsWith("javax.") ||
                                className.startsWith("java.") ||
                                className.startsWith("sun.") ||
                                className.startsWith("jdk.")) {
                            continue;
                        }

                        int lastDot = className.lastIndexOf('.');
                        if (lastDot > 0) {
                            String pkg = className.substring(0, lastDot);
                            allPackages.add(pkg);
                        }
                    }
                }
            }

            // Extract package from main class if found
            if (!mainClass.isEmpty()) {
                int lastDot = mainClass.lastIndexOf('.');
                if (lastDot > 0) {
                    mainPackage = mainClass.substring(0, lastDot);
                }
            }

            // If no main package found from yml/manifest, analyze class packages
            if (mainPackage.isEmpty() && !allPackages.isEmpty()) {
                mainPackage = findMostCommonPackagePrefix(allPackages);
                log.info("Detected package from class analysis: {}", mainPackage);
            }

            response.put("success", true);
            response.put("mainPackage", mainPackage);
            response.put("mainClass", mainClass);
            response.put("totalClasses", allPackages.size());

            return ResponseEntity.ok(response);

        } catch (

        IOException e) {
            log.error("Error analyzing JAR", e);
            response.put("success", false);
            response.put("error", "Failed to analyze JAR: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Find the most common package prefix from a list of package names.
     * This identifies the "root" package of the project.
     */
    private String findMostCommonPackagePrefix(List<String> packages) {
        if (packages.isEmpty())
            return "";

        // Count frequency of each package and its parent packages
        Map<String, Integer> prefixCounts = new HashMap<>();

        for (String pkg : packages) {
            String[] parts = pkg.split("\\.");
            StringBuilder prefix = new StringBuilder();

            for (int i = 0; i < parts.length; i++) {
                if (i > 0)
                    prefix.append(".");
                prefix.append(parts[i]);

                String currentPrefix = prefix.toString();
                prefixCounts.merge(currentPrefix, 1, Integer::sum);
            }
        }

        // Find the best package prefix
        // We want the shortest prefix that contains most of the classes
        int totalClasses = packages.size();
        String bestPrefix = "";
        int bestScore = 0;

        for (Map.Entry<String, Integer> entry : prefixCounts.entrySet()) {
            String prefix = entry.getKey();
            int count = entry.getValue();
            int depth = prefix.split("\\.").length;

            // Score: favor prefixes that cover most classes but aren't too short
            // Minimum depth of 2 (e.g., "com.example" not just "com")
            if (depth >= 2 && count >= totalClasses * 0.7) {
                // Prefer deeper packages that still cover most classes
                int score = count * depth;
                if (score > bestScore) {
                    bestScore = score;
                    bestPrefix = prefix;
                }
            }
        }

        // If no good prefix found, use the most common full package
        if (bestPrefix.isEmpty()) {
            bestPrefix = packages.stream()
                    .collect(java.util.stream.Collectors.groupingBy(p -> p, java.util.stream.Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("");
        }

        return bestPrefix;
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "NeoGuard");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> jobToMap(ObfuscationJob job) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", job.getId());
        map.put("originalFilename", job.getOriginalFilename());
        map.put("status", job.getStatus().name());
        map.put("createdAt", job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);
        map.put("completedAt", job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
        map.put("errorMessage", job.getErrorMessage());

        // Add file size if output exists
        if (job.getOutputPath() != null) {
            File f = new File(job.getOutputPath());
            if (f.exists()) {
                map.put("outputSize", formatFileSize(f.length()));
            }
        }

        return map;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }
}
