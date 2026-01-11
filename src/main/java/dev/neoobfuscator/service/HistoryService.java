package dev.neoobfuscator.service;

import dev.neoobfuscator.model.JobStatus;
import dev.neoobfuscator.model.ObfuscationJob;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing obfuscation job history in SQLite.
 */
@Service
public class HistoryService {

    private final JdbcTemplate jdbcTemplate;
    private final Gson gson = new Gson();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public HistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        // Create table if not exists
        jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS obfuscation_jobs (
                        id TEXT PRIMARY KEY,
                        original_filename TEXT NOT NULL,
                        status TEXT NOT NULL,
                        config_json TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        completed_at TEXT,
                        input_path TEXT,
                        output_path TEXT,
                        error_message TEXT,
                        logs TEXT
                    )
                """);
    }

    public void save(ObfuscationJob job) {
        String sql = """
                    INSERT OR REPLACE INTO obfuscation_jobs
                    (id, original_filename, status, config_json, created_at, completed_at, input_path, output_path, error_message, logs)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(sql,
                job.getId(),
                job.getOriginalFilename(),
                job.getStatus().name(),
                job.getConfigJson(),
                job.getCreatedAt() != null ? job.getCreatedAt().format(FORMATTER) : null,
                job.getCompletedAt() != null ? job.getCompletedAt().format(FORMATTER) : null,
                job.getInputPath(),
                job.getOutputPath(),
                job.getErrorMessage(),
                job.getLogs());
    }

    public Optional<ObfuscationJob> findById(String id) {
        String sql = "SELECT * FROM obfuscation_jobs WHERE id = ?";
        List<ObfuscationJob> jobs = jdbcTemplate.query(sql, new JobRowMapper(), id);
        return jobs.isEmpty() ? Optional.empty() : Optional.of(jobs.get(0));
    }

    public List<ObfuscationJob> findAll() {
        String sql = "SELECT * FROM obfuscation_jobs ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new JobRowMapper());
    }

    public List<ObfuscationJob> findRecent(int limit) {
        String sql = "SELECT * FROM obfuscation_jobs ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, new JobRowMapper(), limit);
    }

    public void updateStatus(String id, JobStatus status, String errorMessage) {
        String sql = "UPDATE obfuscation_jobs SET status = ?, error_message = ?, completed_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), errorMessage,
                status == JobStatus.COMPLETED || status == JobStatus.FAILED ? LocalDateTime.now().format(FORMATTER)
                        : null,
                id);
    }

    public void updateLogs(String id, String logs) {
        String sql = "UPDATE obfuscation_jobs SET logs = ? WHERE id = ?";
        jdbcTemplate.update(sql, logs, id);
    }

    public void delete(String id) {
        String sql = "DELETE FROM obfuscation_jobs WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private static class JobRowMapper implements RowMapper<ObfuscationJob> {
        @Override
        public ObfuscationJob mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ObfuscationJob.builder()
                    .id(rs.getString("id"))
                    .originalFilename(rs.getString("original_filename"))
                    .status(JobStatus.valueOf(rs.getString("status")))
                    .configJson(rs.getString("config_json"))
                    .createdAt(parseDateTime(rs.getString("created_at")))
                    .completedAt(parseDateTime(rs.getString("completed_at")))
                    .inputPath(rs.getString("input_path"))
                    .outputPath(rs.getString("output_path"))
                    .errorMessage(rs.getString("error_message"))
                    .logs(rs.getString("logs"))
                    .build();
        }

        private LocalDateTime parseDateTime(String value) {
            if (value == null || value.isEmpty())
                return null;
            try {
                return LocalDateTime.parse(value, FORMATTER);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
