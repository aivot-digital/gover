package de.aivot.prosuna.backend.codeLists.services;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Runs code list synchronizations in the background. Requests are stored with the code list; RabbitMQ only carries
 * the key as a wake-up signal, and a periodic sweep publishes requests again whose messages may have been lost.
 */
@Service
public class CodeListWorker {
    public static final String DO_WORK_ON_CODE_LIST_QUEUE = "do-work-on-code-list-queue";
    private static final Logger logger = LoggerFactory.getLogger(CodeListWorker.class);

    private final CodeListService codeListService;
    private final RabbitTemplate rabbitTemplate;
    private final Duration sweepInterval;
    private final Duration staleThreshold;

    public CodeListWorker(CodeListService codeListService, RabbitTemplate rabbitTemplate,
                          @Value("${prosuna.background-sync.sweep-interval:PT5M}") Duration sweepInterval,
                          @Value("${prosuna.background-sync.stale-threshold:PT1H}") Duration staleThreshold) {
        this.codeListService = codeListService;
        this.rabbitTemplate = rabbitTemplate;
        this.sweepInterval = sweepInterval;
        this.staleThreshold = staleThreshold;
    }

    @Bean
    public Queue doWorkOnCodeListQueue() {
        return new Queue(DO_WORK_ON_CODE_LIST_QUEUE, true);
    }

    @RabbitListener(queues = DO_WORK_ON_CODE_LIST_QUEUE)
    public void listen(@Nonnull String codeListKey) {
        codeListService.syncCodeList(codeListKey);
    }

    public void triggerCodeListUpdate(@Nonnull String codeListKey, boolean keepOutdated) {
        if (codeListService.requestSync(codeListKey, keepOutdated)) {
            publish(codeListKey);
        }
    }

    @Scheduled(fixedDelayString = "${prosuna.background-sync.sweep-interval:PT5M}")
    public void republishDueSyncRequests() {
        var now = Instant.now();
        for (var codeListKey : codeListService.claimDueSyncRequests(now.minus(sweepInterval), now.minus(staleThreshold))) {
            publish(codeListKey);
        }
    }

    private void publish(@Nonnull String codeListKey) {
        try {
            rabbitTemplate.convertAndSend(DO_WORK_ON_CODE_LIST_QUEUE, codeListKey);
        } catch (RuntimeException exception) {
            // The request is stored with the code list, so the next sweep publishes it again.
            logger.warn("Could not publish the synchronization of code list {}", codeListKey, exception);
        }
    }
}
