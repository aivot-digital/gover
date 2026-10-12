package de.aivot.prosuna.backend.codeLists.services;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CodeListWorkerTest {
    private final CodeListService service = mock(CodeListService.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final CodeListWorker worker = new CodeListWorker(service, rabbit, Duration.ofMinutes(5), Duration.ofHours(1));

    @Test
    void publishesARecordedRequest() {
        when(service.requestSync("test", false)).thenReturn(true);

        worker.triggerCodeListUpdate("test", false);

        verify(rabbit).convertAndSend(CodeListWorker.DO_WORK_ON_CODE_LIST_QUEUE, "test");
    }

    @Test
    void doesNotPublishWhenTheListHasNoSynchronizedSource() {
        worker.triggerCodeListUpdate("manual", true);

        verifyNoInteractions(rabbit);
    }

    @Test
    void unavailableBrokerDoesNotFailTheRecordedRequest() {
        when(service.requestSync("test", true)).thenReturn(true);
        doThrow(new AmqpConnectException(new IllegalStateException("broker unavailable"))).when(rabbit)
                .convertAndSend(CodeListWorker.DO_WORK_ON_CODE_LIST_QUEUE, "test");

        assertDoesNotThrow(() -> worker.triggerCodeListUpdate("test", true));
    }

    @Test
    void sweepPublishesDueRequestsAgain() {
        when(service.claimDueSyncRequests(any(), any())).thenReturn(List.of("a", "b"));
        var before = Instant.now();

        worker.republishDueSyncRequests();

        verify(service).claimDueSyncRequests(argThat(requestedBefore ->
                !requestedBefore.isBefore(before.minus(Duration.ofMinutes(5)))
                        && requestedBefore.isBefore(Instant.now().minus(Duration.ofMinutes(4)))), argThat(startedBefore ->
                startedBefore.isBefore(Instant.now().minus(Duration.ofMinutes(59)))));
        verify(rabbit).convertAndSend(CodeListWorker.DO_WORK_ON_CODE_LIST_QUEUE, "a");
        verify(rabbit).convertAndSend(CodeListWorker.DO_WORK_ON_CODE_LIST_QUEUE, "b");
    }

    @Test
    void listenerRunsTheRequestedSynchronization() {
        worker.listen("test");

        verify(service).syncCodeList("test");
        verifyNoInteractions(rabbit);
    }
}
