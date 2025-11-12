package com.dtao.lms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ✅ AsyncConfig
 *
 * Enables asynchronous task execution (for mail, ranking, background jobs, etc.)
 * Safe for production: isolates heavy tasks from main web threads.
 * Adjusts automatically to your server CPU cores.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(cores, 8));  // keep at least 8 threads
        executor.setMaxPoolSize(cores * 4);            // burst capacity
        executor.setQueueCapacity(1000);               // queue up to 1000 pending async tasks
        executor.setThreadNamePrefix("AsyncExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // fallback: run in caller thread
        executor.initialize();

        log.info("✅ Async ThreadPool initialized: core={} max={} queue={}", 
                 executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("MailThread-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        log.info("📧 Mail ThreadPool initialized: core={} max={}", 
                 executor.getCorePoolSize(), executor.getMaxPoolSize());
        return executor;
    }
}
