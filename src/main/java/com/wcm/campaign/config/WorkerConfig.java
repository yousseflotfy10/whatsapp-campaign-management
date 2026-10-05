package com.wcm.campaign.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class WorkerConfig {

    private static final Logger logger = LoggerFactory.getLogger(WorkerConfig.class);

    public static final String MESSAGE_WORKER_EXECUTOR = "messageWorkerExecutor";

    @Bean(name = MESSAGE_WORKER_EXECUTOR)
    public Executor messageWorkerExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("message-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        logger.info("Message worker executor initialized with corePoolSize=50, maxPoolSize=50, queueCapacity=1000");

        return executor;
    }
}