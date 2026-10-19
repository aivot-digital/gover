package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.properties.AiChatTraceProperties;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AiChatTracePersistenceServiceTest {
    private final AiChatSessionRepository repository = mock(AiChatSessionRepository.class);
    private final AiChatTraceProperties properties = new AiChatTraceProperties();
    private final AiChatTracePersistenceService service = new AiChatTracePersistenceService(repository, properties);
    private final AiChatTraceContext context = new AiChatTraceContext("owner", "session", "turn");

    @Test
    void appendsAnEventWithTheNextSequence() {
        var session = mock(AiChatSessionEntity.class);
        when(session.getMessages()).thenReturn(List.of(Map.of("sequence", 3L, "eventType", "existing")));
        when(session.getUpdated()).thenReturn(Instant.now());
        when(repository.findByUserIdAndSessionIdForUpdate("owner", "session")).thenReturn(Optional.of(session));

        service.append(context, Map.of("eventType", "model_request"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, Object>>> events = ArgumentCaptor.forClass(List.class);
        verify(session).setMessages(events.capture());
        assertThat(events.getValue()).hasSize(2);
        assertThat(events.getValue().getLast())
                .containsEntry("eventType", "model_request")
                .containsEntry("sequence", 4L);
        verify(repository).saveAndFlush(session);
    }

    @Test
    void startsAnewSequenceWhenTheExistingTraceExpired() {
        properties.setRetentionDays(7);
        var session = mock(AiChatSessionEntity.class);
        when(session.getMessages()).thenReturn(List.of(Map.of("sequence", 8L, "eventType", "old")));
        when(session.getUpdated()).thenReturn(Instant.now().minus(8, ChronoUnit.DAYS));
        when(repository.findByUserIdAndSessionIdForUpdate("owner", "session")).thenReturn(Optional.of(session));

        service.append(context, Map.of("eventType", "turn_started"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, Object>>> events = ArgumentCaptor.forClass(List.class);
        verify(session).setMessages(events.capture());
        assertThat(events.getValue()).singleElement().satisfies(event -> assertThat(event)
                .containsEntry("eventType", "turn_started")
                .containsEntry("sequence", 1L));
    }
}
