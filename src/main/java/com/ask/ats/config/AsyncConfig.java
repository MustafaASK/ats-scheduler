package com.ask.ats.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The type Async config.
 */
@Configuration
public class AsyncConfig {

    @Value("${curately.async.maxPoolSize}")
    private int maxPoolSize;

    @Value("${curately.async.corePoolSize}")
    private int corePoolSize;

    @Value("${curately.async.queueCapacity}")
    private int queueCapacity;

    /**
     * Task executor executor.
     *
     * @return the executor
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("AsyncExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
