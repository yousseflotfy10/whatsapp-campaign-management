package com.wcm.campaign.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RealtimeEventService {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeEventService.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        logger.info("New realtime client subscribed. Active clients: {}", emitters.size());

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            logger.info("Realtime client disconnected. Active clients: {}", emitters.size());
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            logger.info("Realtime client connection timed out. Active clients: {}", emitters.size());
        });

        emitter.onError(error -> {
            emitters.remove(emitter);
            logger.warn("Realtime client connection error. Active clients: {}", emitters.size());
        });

        try {
            emitter.send(SseEmitter.event().name("connected").data("connected"));
        } catch (IOException exception) {
            emitters.remove(emitter);
            logger.warn("Failed to send realtime connection event", exception);
        }

        return emitter;
    }

    public void publishAfterCommit(String resource) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publish(resource);
                        }
                    }
            );
            return;
        }

        publish(resource);
    }

    private void publish(String resource) {

        logger.info("Publishing realtime event: {}", resource);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("data-updated")
                                .data(resource)
                );
            } catch (IOException exception) {
                emitters.remove(emitter);
                logger.warn("Failed to send realtime event to client", exception);
            }
        }
    }
}