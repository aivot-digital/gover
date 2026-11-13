package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.properties.AiChatTraceProperties;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiChatTraceCleanupServiceTest {
    @Test
    void clearsTraceDataOlderThanTheConfiguredRetention() {
        var repository = mock(AiChatSessionRepository.class);
        var properties = new AiChatTraceProperties();
        properties.setRetentionDays(5);
        var service = new AiChatTraceCleanupService(repository, properties);
        var earliestCutoff = Instant.now().minus(5, ChronoUnit.DAYS);

        service.clearExpiredTraces("test");

        var cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(repository).clearMessagesUpdatedBefore(cutoff.capture());
        assertThat(cutoff.getValue())
                .isAfterOrEqualTo(earliestCutoff)
                .isBeforeOrEqualTo(Instant.now().minus(5, ChronoUnit.DAYS));
    }
}
