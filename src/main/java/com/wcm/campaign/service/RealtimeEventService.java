
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

    private static final Logger logger =
            LoggerFactory.getLogger(RealtimeEventService.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        logger.info("New realtime subscription requested");

        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        logger.info(
                "Realtime client subscribed. Active clients={}",
                emitters.size());

        emitter.onCompletion(() -> {
            boolean removed = emitters.remove(emitter);

            logger.info(
                    "Realtime client completed. Removed={}, Active clients={}",
                    removed, emitters.size());
        });

        emitter.onTimeout(() -> {
            boolean removed = emitters.remove(emitter);

            logger.warn(
                    "Realtime client timed out. Removed={}, Active clients={}",
                    removed, emitters.size());
        });

        emitter.onError(error -> {
            boolean removed = emitters.remove(emitter);

            logger.warn(
                    "Realtime client connection error. Removed={}, Active clients={}",
                    removed, emitters.size(), error);
        });

        try {
            logger.debug("Sending initial connection event");

            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .data("connected")
            );

            logger.info("Initial realtime connection event sent successfully");
        } catch (IOException exception) {
            emitters.remove(emitter);

            logger.error(
                    "Failed to send initial realtime connection event. Active clients={}",
                    emitters.size(), exception);

            emitter.completeWithError(exception);
        }

        return emitter;
    }

    public void publishAfterCommit(String resource) {
        if (resource == null || resource.isBlank()) {
            logger.warn("Realtime event not published because resource is missing");
            return;
        }

        boolean transactionActive =
                TransactionSynchronizationManager.isSynchronizationActive();

        logger.info(
                "Realtime publication requested. Resource={}, TransactionSynchronizationActive={}, ActiveClients={}",
                resource, transactionActive, emitters.size());

        if (transactionActive) {
            logger.debug(
                    "Registering realtime publication after transaction commit. Resource={}",
                    resource);

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            logger.info(
                                    "Transaction committed. Publishing realtime event. Resource={}",
                                    resource);

                            publish(resource);
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                logger.warn(
                                        "Transaction did not commit. Realtime event will not be published. Resource={}, TransactionStatus={}",
                                        resource, status);
                            }
                        }
                    }
            );

            return;
        }

        logger.debug(
                "No active transaction synchronization. Publishing immediately. Resource={}",
                resource);

        publish(resource);
    }

    private void publish(String resource) {
        logger.info(
                "Starting realtime publication. Resource={}, ActiveClients={}",
                resource, emitters.size());

        int successfulDeliveries = 0;
        int failedDeliveries = 0;

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("data-updated")
                                .data(resource)
                );

                successfulDeliveries++;

                logger.debug(
                        "Realtime event delivered successfully. Resource={}",
                        resource);
            } catch (IOException | IllegalStateException exception) {
                failedDeliveries++;
                emitters.remove(emitter);

                logger.warn(
                        "Realtime event delivery failed. Resource={}, ClientRemoved={}, RemainingClients={}",
                        resource, true, emitters.size(), exception);
            }
        }

        logger.info(
                "Realtime publication completed. Resource={}, SuccessfulDeliveries={}, FailedDeliveries={}, ActiveClients={}",
                resource,
                successfulDeliveries,
                failedDeliveries,
                emitters.size());
    }
}
