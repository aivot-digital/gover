package de.aivot.prosuna.backend.codeLists.services;

import de.aivot.prosuna.backend.lib.services.StoredWorkSignals;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
    private final StoredWorkSignals signals;
    private final Duration sweepInterval;
    private final Duration staleThreshold;

    public CodeListWorker(CodeListService codeListService, StoredWorkSignals signals,
                          @Value("${prosuna.background-sync.sweep-interval:PT5M}") Duration sweepInterval,
                          @Value("${prosuna.background-sync.stale-threshold:PT1H}") Duration staleThreshold) {
        this.codeListService = codeListService;
        this.signals = signals;
        this.sweepInterval = sweepInterval;
        this.staleThreshold = staleThreshold;
    }

    @Bean
    public Queue doWorkOnCodeListQueue() {
        return new Queue(DO_WORK_ON_CODE_LIST_QUEUE, true);
    }

    @RabbitListener(queues = DO_WORK_ON_CODE_LIST_QUEUE)
    public void listen(@Nonnull String codeListKey) {
        try {
            if (codeListService.syncCodeList(codeListKey, Instant.now().minus(staleThreshold))) {
                signals.publish(DO_WORK_ON_CODE_LIST_QUEUE, codeListKey);
            }
        } catch (RuntimeException exception) {
            // The request remains stored, so it is published again later instead of being redelivered at once.
            logger.error("Could not synchronize code list {}", codeListKey, exception);
        }
    }

    public void triggerCodeListUpdate(@Nonnull String codeListKey, boolean keepOutdated) {
        if (codeListService.requestSync(codeListKey, keepOutdated)) {
            signals.publish(DO_WORK_ON_CODE_LIST_QUEUE, codeListKey);
        }
    }

    @Scheduled(fixedDelayString = "${prosuna.background-sync.sweep-interval:PT5M}")
    public void republishDueSyncRequests() {
        var now = Instant.now();
        var requestedBefore = now.minus(sweepInterval);
        signals.republish(DO_WORK_ON_CODE_LIST_QUEUE, codeListService.findPendingSyncKeys(requestedBefore),
                codeListKey -> codeListService.claimSyncRedispatch(codeListKey, requestedBefore, now));
        // Interrupted runs are taken over by the consumer's claim. The sweep only reads them, so it never waits for
        // the lock of a run that is still recording its result.
        signals.republish(DO_WORK_ON_CODE_LIST_QUEUE, codeListService.findInterruptedSyncKeys(now.minus(staleThreshold)),
                codeListKey -> true);
    }
}
