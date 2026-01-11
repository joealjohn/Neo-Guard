package dev.neoobfuscator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for async task execution.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${neo.async.core-pool-size:2}")
    private int corePoolSize;

    @Value("${neo.async.max-pool-size:4}")
    private int maxPoolSize;

    @Value("${neo.async.queue-capacity:100}")
    private int queueCapacity;

    @Bean(name = "obfuscationExecutor")
    public Executor obfuscationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("Obfuscate-");
        executor.initialize();
        return executor;
    }
}
