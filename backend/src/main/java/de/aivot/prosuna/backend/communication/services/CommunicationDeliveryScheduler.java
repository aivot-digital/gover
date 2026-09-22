package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.process.workers.ProcessWorker;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import de.aivot.prosuna.backend.communication.repositories.CommunicationDeliveryRepository;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class CommunicationDeliveryScheduler {
    public static final String POLL_QUEUE = "communication-delivery-poll-queue";

    @Bean
    public Queue communicationDeliveryPollQueue() {
        return new Queue(POLL_QUEUE, true);
    }

    @RabbitListener(queues = POLL_QUEUE)
    public void checkDelivery(UUID id) {
        try {
            monitor.check(id);
        } catch (Exception e) {
            // The durable due timestamp schedules another attempt; do not hot-loop a rejected queue message.
            LoggerFactory.getLogger(getClass()).error("Failed to reconcile communication delivery {}", id, e);
        }
    }

    private final CommunicationDeliveryRepository deliveries;
    private final CommunicationDeliveryMonitor monitor;
    private final RabbitTemplate rabbit;
    private final JsonMapper mapper;

    public CommunicationDeliveryScheduler(CommunicationDeliveryRepository deliveries, CommunicationDeliveryMonitor monitor,
                                           RabbitTemplate rabbit,
                                           JsonMapper mapper) {
        this.deliveries = deliveries;
        this.monitor = monitor;
        this.rabbit = rabbit;
        this.mapper = mapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        for (var delivery : deliveries.findPendingWork()) {
            try {
                var payload = mapper.convertValue(delivery.getNextWork(), ProcessWorker.DoWorkWorkerPayload.class);
                rabbit.convertAndSend(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE, payload);
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("Failed to dispatch communication continuation {}", delivery.getId(), e);
            }
        }
        for (var id : deliveries.findDueIds(Instant.now())) {
            try {
                rabbit.convertAndSend(POLL_QUEUE, id);
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("Failed to reconcile communication delivery {}", id, e);
            }
        }
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void removeExpiredTests() {
        deliveries.deleteByProcessInstanceIdIsNullAndCreatedBefore(Instant.now().minus(7, ChronoUnit.DAYS));
    }
}
