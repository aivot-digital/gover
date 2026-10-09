package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.properties.AiChatTraceProperties;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AiChatTracePersistenceService {
    private final AiChatSessionRepository repository;
    private final AiChatTraceProperties properties;

    public AiChatTracePersistenceService(@Nonnull AiChatSessionRepository repository,
                                         @Nonnull AiChatTraceProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public void append(@Nonnull AiChatTraceContext context, @Nonnull Map<String, Object> event) {
        var session = repository
                .findByUserIdAndSessionIdForUpdate(context.userId(), context.sessionId())
                .orElseThrow(() -> new IllegalStateException("AI chat session is unavailable"));

        var events = new ArrayList<>(session.getMessages());
        var updated = session.getUpdated();
        var cutoff = Instant.now().minus(properties.getRetentionDays(), ChronoUnit.DAYS);
        if (updated != null && updated.isBefore(cutoff)) {
            events.clear();
        }

        var storedEvent = new LinkedHashMap<>(event);
        storedEvent.put("sequence", nextSequence(events));
        events.add(storedEvent);
        session.setMessages(events);
        repository.saveAndFlush(session);
    }

    private long nextSequence(@Nonnull ArrayList<Map<String, Object>> events) {
        long sequence = 0;
        for (var event : events) {
            if (event.get("sequence") instanceof Number number) {
                sequence = Math.max(sequence, number.longValue());
            }
        }
        return sequence + 1;
    }
}
