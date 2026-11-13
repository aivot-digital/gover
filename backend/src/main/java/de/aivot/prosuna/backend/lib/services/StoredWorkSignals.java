package de.aivot.prosuna.backend.lib.services;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Publishes wake-up signals for background work that is stored in the database.
 *
 * <p>The stored record is the only record of the work. A message only carries its ID or key, so RabbitMQ holds no
 * state that would have to be persisted or backed up. Publishing is best effort: if it fails or a message is lost,
 * the periodic sweep of the owning worker publishes the stored work again, so consumers must tolerate duplicates.
 */
@Service
public class StoredWorkSignals {
    private static final Logger logger = LoggerFactory.getLogger(StoredWorkSignals.class);

    private final RabbitTemplate rabbitTemplate;

    public StoredWorkSignals(@Nonnull RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Publishes the ID or key of stored work. Returns whether the broker accepted the message. */
    public boolean publish(@Nonnull String queue, @Nonnull Serializable key) {
        try {
            rabbitTemplate.convertAndSend(queue, key);
            return true;
        } catch (RuntimeException exception) {
            logger.warn("Could not publish {} to {}; the stored work is published again later", key, queue, exception);
            return false;
        }
    }

    /**
     * Publishes stored work whose message may have been lost. The claim records the publication, e.g. with a
     * conditional update of a timestamp, so several application instances do not publish the same work at once.
     */
    public <K extends Serializable> void republish(@Nonnull String queue, @Nonnull Collection<K> dueKeys,
                                                   @Nonnull Predicate<K> claim) {
        for (var key : dueKeys) {
            if (claim.test(key)) {
                publish(queue, key);
            }
        }
    }
}
