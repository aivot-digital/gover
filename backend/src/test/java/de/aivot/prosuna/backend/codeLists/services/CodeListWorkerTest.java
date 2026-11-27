package de.aivot.prosuna.backend.codeLists.services;

import de.aivot.prosuna.backend.lib.services.StoredWorkSignals;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.QueryTimeoutException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CodeListWorkerTest {
    private final CodeListService service = mock(CodeListService.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final CodeListWorker worker = new CodeListWorker(service, new StoredWorkSignals(rabbit), Duration.ofMinutes(5),
            Duration.ofHours(1));

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
    void sweepPublishesClaimedPendingRequestsAndInterruptedRunsAgain() {
        when(service.findPendingSyncKeys(any())).thenReturn(List.of("a", "b"));
        when(service.claimSyncRedispatch(eq("a"), any(), any())).thenReturn(true);
        when(service.findInterruptedSyncKeys(any())).thenReturn(List.of("c"));
        var before = Instant.now();

        worker.republishDueSyncRequests();

        verify(service).findPendingSyncKeys(argThat(requestedBefore ->
                !requestedBefore.isBefore(before.minus(Duration.ofMinutes(5)))
                        && requestedBefore.isBefore(Instant.now().minus(Duration.ofMinutes(4)))));
        verify(service).findInterruptedSyncKeys(argThat(startedBefore ->
                !startedBefore.isBefore(before.minus(Duration.ofHours(1)))
                        && startedBefore.isBefore(Instant.now().minus(Duration.ofMinutes(59)))));
        verify(rabbit).convertAndSend(CodeListWorker.DO_WORK_ON_CODE_LIST_QUEUE, "a");
        verify(rabbit, never()).convertAndSend(CodeListWorker.DO_WORK_ON_CODE_LIST_QUEUE, "b");
        verify(rabbit).convertAndSend(CodeListWorker.DO_WORK_ON_CODE_LIST_QUEUE, "c");
    }

    @Test
    void listenerRunsTheRequestedSynchronizationWithTheStaleThreshold() {
        var before = Instant.now();

        worker.listen("test");

        verify(service).syncCodeList(eq("test"), argThat(staleBefore ->
                !staleBefore.isBefore(before.minus(Duration.ofHours(1)))
                        && staleBefore.isBefore(Instant.now().minus(Duration.ofMinutes(59)))));
        verifyNoInteractions(rabbit);
    }

    @Test
    void failedSynchronizationIsNotHandedBackToTheBroker() {
        when(service.syncCodeList(eq("test"), any())).thenThrow(new QueryTimeoutException("database busy"));

        assertDoesNotThrow(() -> worker.listen("test"));
    }

    @Test
    void listenerPublishesANewerRequestThatSupersededTheRun() {
        when(service.syncCodeList(eq("test"), any())).thenReturn(true);

        worker.listen("test");

        verify(rabbit).convertAndSend(CodeListWorker.DO_WORK_ON_CODE_LIST_QUEUE, "test");
    }
}
