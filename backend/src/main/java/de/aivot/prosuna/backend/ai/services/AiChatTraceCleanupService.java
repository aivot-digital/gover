package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.properties.AiChatTraceProperties;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
public class AiChatTraceCleanupService {
    private static final Logger logger = LoggerFactory.getLogger(AiChatTraceCleanupService.class);

    private final AiChatSessionRepository repository;
    private final AiChatTraceProperties properties;
    private final AtomicBoolean cleanupRunning = new AtomicBoolean(false);

    public AiChatTraceCleanupService(@Nonnull AiChatSessionRepository repository,
                                     @Nonnull AiChatTraceProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void clearExpiredTracesOnStartup() {
        clearExpiredTraces("startup");
    }

    @Transactional
    @Scheduled(cron = "0 15 3 * * *", zone = "${prosuna.timezone}")
    public void clearExpiredTracesNightly() {
        clearExpiredTraces("daily-schedule");
    }

    @Transactional
    public void clearExpiredTraces(@Nonnull String trigger) {
        if (!cleanupRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            var cutoff = Instant.now().minus(properties.getRetentionDays(), ChronoUnit.DAYS);
            var cleared = repository.clearMessagesUpdatedBefore(cutoff);
            if (cleared > 0) {
                logger.info("Cleared {} expired AI chat trace(s) (trigger={}).", cleared, trigger);
            }
        } finally {
            cleanupRunning.set(false);
        }
    }
}
