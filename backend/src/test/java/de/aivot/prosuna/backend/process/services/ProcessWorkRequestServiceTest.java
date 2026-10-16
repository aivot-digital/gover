package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.services.StoredWorkSignals;
import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.process.entities.ProcessWorkRequestEntity;
import de.aivot.prosuna.backend.process.enums.ProcessWorkRequestKind;
import de.aivot.prosuna.backend.process.repositories.ProcessWorkRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real transactions decide when request IDs are published. */
class ProcessWorkRequestServiceTest {
    private final Instant now = Instant.parse("2030-05-01T12:00:00Z");
    private final ProcessWorkRequestRepository requests = mock(ProcessWorkRequestRepository.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final AtomicLong nextId = new AtomicLong(10);
    private ProcessWorkRequestService service;
    private TransactionTemplate transaction;

    @BeforeEach
    void setup() {
        var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);
        when(requests.save(any())).thenAnswer(call -> {
            ProcessWorkRequestEntity request = call.getArgument(0);
            ReflectionTestUtils.setField(request, "id", nextId.getAndIncrement());
            return request;
        });
        service = new ProcessWorkRequestService(requests, new StoredWorkSignals(rabbit), new BusinessTime(ZoneOffset.UTC, clock));
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
    }

    @Test
    void publishesTheRequestOnlyAfterTheCommit() {
        transaction.executeWithoutResult(status -> {
            service.enqueueTransition(1L, 2L, "next", 4);
            verifyNoInteractions(rabbit);
        });

        verify(rabbit).convertAndSend(ProcessWorkRequestService.QUEUE, 10L);
        var request = ArgumentCaptor.forClass(ProcessWorkRequestEntity.class);
        verify(requests).save(request.capture());
        assertEquals(ProcessWorkRequestKind.Transition, request.getValue().getKind());
        assertEquals(2L, request.getValue().getTaskId());
        assertEquals("next", request.getValue().getViaPort());
        assertEquals(4, request.getValue().getToNodeId());
        // The after-commit publication counts as dispatch, so the dispatcher waits for the redispatch interval.
        assertEquals(now, request.getValue().getLastDispatched());
    }

    @Test
    void rolledBackTransactionPublishesNothing() {
        transaction.executeWithoutResult(status -> {
            service.enqueueStart(1L);
            status.setRollbackOnly();
        });

        verify(requests).save(any());
        verifyNoInteractions(rabbit);
    }

    @Test
    void nestedScopePublishesWhenTheOuterTransactionCommits() {
        transaction.executeWithoutResult(outer -> {
            transaction.executeWithoutResult(inner -> service.enqueueRestart(1L, 7L));
            verifyNoInteractions(rabbit);
        });

        verify(rabbit).convertAndSend(ProcessWorkRequestService.QUEUE, 10L);
    }

    @Test
    void failedPublicationDoesNotFailTheCommittedChange() {
        doThrow(new AmqpConnectException(new IllegalStateException("broker unavailable"))).when(rabbit)
                .convertAndSend(ProcessWorkRequestService.QUEUE, 10L);

        assertDoesNotThrow(() -> transaction.executeWithoutResult(status -> {
            service.enqueueResume(1L, 5L);
            service.enqueueResume(1L, 6L);
        }));

        verify(rabbit).convertAndSend(ProcessWorkRequestService.QUEUE, 11L);
    }

    @Test
    void rejectsRequestsOutsideATransaction() {
        assertThrows(IllegalStateException.class, () -> service.enqueueTransition(1L, 2L, "next", 4));
        assertThrows(IllegalStateException.class, () -> service.enqueueResume(1L, 5L));
        verifyNoInteractions(requests, rabbit);
    }

    @Test
    void keepsOnlyOnePendingStartTransitionAndRestart() {
        when(requests.existsByKindAndProcessInstanceId(ProcessWorkRequestKind.Start, 1L)).thenReturn(true);
        when(requests.existsByKindAndTaskId(ProcessWorkRequestKind.Transition, 2L)).thenReturn(true);
        when(requests.existsByKindAndTaskId(ProcessWorkRequestKind.Restart, 7L)).thenReturn(true);

        transaction.executeWithoutResult(status -> {
            service.enqueueStart(1L);
            service.enqueueTransition(1L, 2L, "next", 4);
            service.enqueueRestart(1L, 7L);
        });

        verify(requests, never()).save(any());
        verifyNoInteractions(rabbit);
    }

    @Test
    void repeatedResumeRequestsAreAllRecorded() {
        transaction.executeWithoutResult(status -> {
            service.enqueueResume(1L, 5L);
            service.enqueueResume(1L, 5L);
        });

        verify(requests, times(2)).save(any());
        verify(rabbit).convertAndSend(ProcessWorkRequestService.QUEUE, 10L);
        verify(rabbit).convertAndSend(ProcessWorkRequestService.QUEUE, 11L);
    }

    @Test
    void redispatchesHeldRequestsAfterTheCommit() {
        transaction.executeWithoutResult(status -> {
            service.redispatchAfterCommit(List.of(3L, 4L));
            verifyNoInteractions(rabbit);
        });

        verify(rabbit).convertAndSend(ProcessWorkRequestService.QUEUE, 3L);
        verify(rabbit).convertAndSend(ProcessWorkRequestService.QUEUE, 4L);
    }
}
