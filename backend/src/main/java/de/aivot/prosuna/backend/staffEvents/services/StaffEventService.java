package de.aivot.prosuna.backend.staffEvents.services;

import de.aivot.prosuna.backend.staffEvents.models.StaffEvent;
import de.aivot.prosuna.backend.staffEvents.models.StaffEventAudience;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sends {@link StaffEvent}s to the event streams of the connected staff users.
 *
 * <p>A stream is held by the application instance that accepted its request, so every event goes through a RabbitMQ
 * fanout exchange. Each instance binds its own temporary queue to it and forwards every event to those of its streams
 * whose users may receive it.
 */
@Service
public class StaffEventService {
    private static final Logger logger = LoggerFactory.getLogger(StaffEventService.class);
    static final String EXCHANGE = "prosuna.staff-events";
    // The stream outlives the access token that authorized it. Each event is checked against the current permissions
    // instead, and the clients authenticate again when they reconnect.
    static final Duration MAX_STREAM_DURATION = Duration.ofMinutes(30);

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;
    private final Map<String, StaffEventAudience> audiences;
    private final UserRepository users;
    // Each stream with the ID of its user.
    private final Map<SseEmitter, String> streams = new ConcurrentHashMap<>();

    public StaffEventService(@Nonnull RabbitTemplate rabbitTemplate, @Nonnull JsonMapper jsonMapper,
                             @Nonnull List<StaffEventAudience> audiences, @Nonnull UserRepository users) {
        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
        this.audiences = audiences.stream().collect(Collectors.toMap(StaffEventAudience::type, Function.identity()));
        this.users = users;
    }

    @Nonnull
    public SseEmitter subscribe(@Nonnull String userId) {
        var stream = new SseEmitter(MAX_STREAM_DURATION.toMillis());
        stream.onCompletion(() -> streams.remove(stream));
        stream.onError(error -> streams.remove(stream));
        stream.onTimeout(stream::complete);
        streams.put(stream, userId);
        return stream;
    }

    /**
     * Sends the event once the surrounding transaction commits, or right away outside a transaction. A transaction
     * often writes the same row several times, e.g. a worker step, so its events are sent once each.
     */
    @EventListener
    public void publish(@Nonnull StaffEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            send(event);
            return;
        }

        TransactionSynchronizationManager.getSynchronizations().stream()
                .filter(PendingEvents.class::isInstance)
                .map(PendingEvents.class::cast)
                .findFirst()
                .orElseGet(() -> {
                    var pending = new PendingEvents();
                    TransactionSynchronizationManager.registerSynchronization(pending);
                    return pending;
                })
                .events.add(event);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(value = EXCHANGE, type = ExchangeTypes.FANOUT)))
    public void deliver(@Nonnull String message) {
        StaffEvent event;
        try {
            event = jsonMapper.readValue(message, StaffEvent.class);
        } catch (JacksonException exception) {
            logger.warn("Discarding the malformed staff event {}", message, exception);
            return;
        }

        var recipients = findRecipients(event);
        streams.forEach((stream, userId) -> {
            if (recipients.contains(userId)) {
                send(stream, SseEmitter.event().data(event, MediaType.APPLICATION_JSON));
            }
        });
    }

    /**
     * Proxies close idle connections, and only a write detects a client that went away without closing its stream.
     * The streams of users who were disabled meanwhile end here. Users disabled or deleted directly in the identity
     * provider are only known after the next run of the UserSyncService.
     */
    @Scheduled(fixedDelay = 25, timeUnit = TimeUnit.SECONDS)
    public void sendHeartbeats() {
        if (streams.isEmpty()) {
            return;
        }

        var activeUserIds = Set.copyOf(users.findActiveIdsByIdIn(Set.copyOf(streams.values())));
        streams.forEach((stream, userId) -> {
            if (activeUserIds.contains(userId)) {
                send(stream, SseEmitter.event().comment(""));
            } else {
                stream.complete();
            }
        });
    }

    @Nonnull
    private Set<String> findRecipients(@Nonnull StaffEvent event) {
        if (event.userId() != null) {
            return Set.of(event.userId());
        }

        var audience = audiences.get(event.type());
        if (audience == null) {
            logger.warn("Discarding the staff event {}, because its type has no audience", event);
            return Set.of();
        }

        var userIds = Set.copyOf(streams.values());
        if (userIds.isEmpty()) {
            return Set.of();
        }
        try {
            return Set.copyOf(audience.filter(event, userIds));
        } catch (RuntimeException exception) {
            // Rethrowing would make the broker redeliver the event over and over.
            logger.warn("Discarding the staff event {}, because its recipients could not be determined", event,
                    exception);
            return Set.of();
        }
    }

    private void send(@Nonnull StaffEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, "", jsonMapper.writeValueAsString(event));
        } catch (RuntimeException exception) {
            // Events are hints for open views, so a lost event must not fail the operation that caused it.
            logger.warn("Could not publish the staff event {}", event, exception);
        }
    }

    private void send(@Nonnull SseEmitter stream, @Nonnull SseEmitter.SseEventBuilder event) {
        try {
            stream.send(event);
        } catch (IOException | IllegalStateException exception) {
            // The client went away or the stream already ended; the servlet container completes it.
            streams.remove(stream);
        }
    }

    /** The distinct events of a transaction, sent once it commits. */
    private final class PendingEvents implements TransactionSynchronization {
        private final Set<StaffEvent> events = new LinkedHashSet<>();

        @Override
        public void afterCommit() {
            events.forEach(StaffEventService.this::send);
        }
    }
}
