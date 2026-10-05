package com.wcm.campaign.service;

import com.wcm.campaign.config.WorkerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Service
public class MessageWorkerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageWorkerService.class);

    private final MessageWorkerProcessor workerProcessor;
    private final Executor messageWorkerExecutor;

    public MessageWorkerService(
            MessageWorkerProcessor workerProcessor,
            @Qualifier(WorkerConfig.MESSAGE_WORKER_EXECUTOR) Executor messageWorkerExecutor
    ) {
        this.workerProcessor = workerProcessor;
        this.messageWorkerExecutor = messageWorkerExecutor;
    }

    public void submit(Long messageId) {

        if (messageId == null) {
            logger.warn("Message submission failed: message ID is null");
            throw new IllegalArgumentException("Message ID cannot be null");
        }

        logger.info("Submitting message {} to worker pool", messageId);

        messageWorkerExecutor.execute(() -> {
            try {
                workerProcessor.processMessage(messageId);
            } catch (Exception exception) {
                logger.error("Worker error for message {}", messageId, exception);
            }
        });
    }
}