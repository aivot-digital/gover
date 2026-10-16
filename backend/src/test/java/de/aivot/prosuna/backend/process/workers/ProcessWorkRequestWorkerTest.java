package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.process.ProcessTaskExecutionTestSupport;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessWorkRequestEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessWorkRequestRepository;
import de.aivot.prosuna.backend.process.services.ProcessTaskExecutionService;
import de.aivot.prosuna.backend.process.services.ProcessWorkRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessWorkRequestWorkerTest {
    private final ProcessWorkRequestRepository requests = mock(ProcessWorkRequestRepository.class);
    private final ProcessWorkRequestService requestService = mock(ProcessWorkRequestService.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessTaskExecutionService execution = ProcessTaskExecutionTestSupport.passthrough();
    private final ProcessWorker processWorker = mock(ProcessWorker.class);
    private final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
    private final Clock clock = mock(Clock.class);
    private final BusinessTime time = new BusinessTime(ZoneOffset.UTC, clock);
    private final Instant now = Instant.parse("2030-05-01T12:00:00Z");
    private final Instant redispatchBefore = now.minus(Duration.ofMinutes(5));
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(1L).setInitialNodeId(3)
            .setStatus(ProcessInstanceStatus.Running);
    private final ProcessWorkRequestWorker worker = new ProcessWorkRequestWorker(requests, requestService, instances, tasks,
            execution, processWorker, time, connectionFactory, 2, Duration.ofMinutes(5));

    @BeforeEach
    void setup() {
        when(clock.instant()).thenReturn(now);
        when(instances.findById(1L)).thenReturn(Optional.of(instance));
        when(requestService.publish(anyLong())).thenReturn(true);
    }

    @Test
    void transitionCreatesSuccessorOnceAndDuplicateMessagesDoNothing() throws Exception {
        var request = stored(ProcessWorkRequestEntity.transition(1L, 2L, "next", 4, now));
        task(2L, 1L, 3, ProcessTaskStatus.Completed);
        createsSuccessorOf(2L);

        worker.consume(10L);
        worker.consume(10L);

        verify(processWorker).workOnNodeLocked(new ProcessWorker.DoWorkWorkerPayload(1L, 2L, 3, "next", 4, null));
        verify(requests).delete(request);
        verify(execution).inInstance(eq(1L), any());
    }

    @Test
    void transitionContinuesAnExpiredTask() {
        var request = stored(ProcessWorkRequestEntity.transition(1L, 2L, "overdue", 4, now));
        task(2L, 1L, 3, ProcessTaskStatus.DeadlineExceeded);
        createsSuccessorOf(2L);

        worker.consume(10L);

        verify(processWorker).workOnNodeLocked(new ProcessWorker.DoWorkWorkerPayload(1L, 2L, 3, "overdue", 4, null));
        verify(requests).delete(request);
    }

    @Test
    void transitionWithExistingSuccessorIsOnlyRemoved() {
        var request = stored(ProcessWorkRequestEntity.transition(1L, 2L, "next", 4, now));
        task(2L, 1L, 3, ProcessTaskStatus.Completed);
        when(tasks.existsByPreviousProcessInstanceTaskId(2L)).thenReturn(true);

        worker.consume(10L);

        verifyNoInteractions(processWorker);
        verify(requests).delete(request);
    }

    @Test
    void rechecksRequestAfterAcquiringInstanceLock() {
        var request = stored(ProcessWorkRequestEntity.transition(1L, 2L, "next", 4, now));
        when(requests.findById(10L)).thenReturn(Optional.of(request), Optional.empty());

        worker.consume(10L);

        verifyNoInteractions(processWorker);
        verify(requests, never()).delete(any());
    }

    @Test
    void keepsTransitionWhenTargetCannotCreateTask() {
        stored(ProcessWorkRequestEntity.transition(1L, 2L, "next", 4, now));
        task(2L, 1L, 3, ProcessTaskStatus.Completed);

        worker.consume(10L);

        verify(processWorker).workOnNodeLocked(any());
        verify(requests, never()).delete(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "instance", "status"})
    void discardsTransitionThatCannotContinueItsPredecessor(String invalid) {
        var request = stored(ProcessWorkRequestEntity.transition(1L, 2L, "next", 4, now));
        if (!invalid.equals("missing")) {
            task(2L, invalid.equals("instance") ? 9L : 1L, 3,
                    invalid.equals("status") ? ProcessTaskStatus.AwaitingStaff : ProcessTaskStatus.Completed);
        }

        worker.consume(10L);

        verifyNoInteractions(processWorker);
        verify(requests).delete(request);
    }

    @ParameterizedTest
    @EnumSource(value = ProcessInstanceStatus.class, names = {"Paused", "Failed", "Created"})
    void holdsRequestUntilInstanceRunsAgain(ProcessInstanceStatus status) {
        stored(ProcessWorkRequestEntity.transition(1L, 2L, "next", 4, now));
        task(2L, 1L, 3, ProcessTaskStatus.Completed);
        instance.setStatus(status);

        worker.consume(10L);

        verifyNoInteractions(processWorker);
        verify(requests, never()).delete(any());
    }

    @ParameterizedTest
    @EnumSource(value = ProcessInstanceStatus.class, names = {"Completed", "Aborted"})
    void discardsRequestForTerminalInstance(ProcessInstanceStatus status) {
        var request = stored(ProcessWorkRequestEntity.transition(1L, 2L, "next", 4, now));
        instance.setStatus(status);

        worker.consume(10L);

        verifyNoInteractions(processWorker);
        verify(requests).delete(request);
    }

    @Test
    void startCreatesTheFirstTaskOnce() {
        var request = stored(ProcessWorkRequestEntity.start(1L, now));
        doAnswer(call -> {
            when(tasks.existsByProcessInstanceId(1L)).thenReturn(true);
            return null;
        }).when(processWorker).workOnNodeLocked(any());

        worker.consume(10L);

        verify(processWorker).workOnNodeLocked(new ProcessWorker.DoWorkWorkerPayload(1L, null, null, null, 3, null));
        verify(requests).delete(request);
    }

    @Test
    void startOfAnInstanceWithTasksIsOnlyRemoved() {
        var request = stored(ProcessWorkRequestEntity.start(1L, now));
        when(tasks.existsByProcessInstanceId(1L)).thenReturn(true);

        worker.consume(10L);

        verifyNoInteractions(processWorker);
        verify(requests).delete(request);
    }

    @Test
    void restartRunsTheFailedTaskAgainOnce() {
        var request = stored(ProcessWorkRequestEntity.restart(1L, 7L, now));
        task(7L, 1L, 4, ProcessTaskStatus.Restarted).setPreviousProcessInstanceTaskId(2L).setPreviousProcessNodeId(3)
                .setPreviousProcessNodePortKey("next");
        doAnswer(call -> {
            when(tasks.existsByRestartForTaskId(7L)).thenReturn(true);
            return null;
        }).when(processWorker).workOnNodeLocked(any());

        worker.consume(10L);
        when(requests.findById(10L)).thenReturn(Optional.empty());
        worker.consume(10L);

        verify(processWorker).workOnNodeLocked(new ProcessWorker.DoWorkWorkerPayload(1L, 2L, 3, "next", 4, 7L));
        verify(requests).delete(request);
    }

    @ParameterizedTest
    @EnumSource(value = ProcessTaskStatus.class, names = {"Failed", "Completed", "Running"})
    void discardsRestartOfTaskThatWasNotRestarted(ProcessTaskStatus status) {
        var request = stored(ProcessWorkRequestEntity.restart(1L, 7L, now));
        task(7L, 1L, 4, status);

        worker.consume(10L);

        verifyNoInteractions(processWorker);
        verify(requests).delete(request);
    }

    @Test
    void resumeRunsAsTaskExecutionAndRemovesRequest() throws Exception {
        stored(ProcessWorkRequestEntity.resume(1L, 5L, now));
        when(requests.existsById(10L)).thenReturn(true);

        worker.consume(10L);

        verify(execution).executeBackground(eq(1L), eq(5L), any());
        verify(execution, never()).inInstance(any(), any());
        verify(processWorker).resumeLocked(1L, 5L);
        verify(requests).deleteById(10L);
    }

    @Test
    void resumeAlreadyRunByAnotherConsumerDoesNothing() {
        stored(ProcessWorkRequestEntity.resume(1L, 5L, now));

        worker.consume(10L);

        verifyNoInteractions(processWorker);
        verify(requests, never()).deleteById(any());
    }

    @Test
    void resumeAfterDeadlineIsRecordedAndDiscarded() throws Exception {
        var request = stored(ProcessWorkRequestEntity.resume(1L, 5L, now));
        doReturn(false).when(execution).executeBackground(eq(1L), eq(5L), any());
        task(5L, 1L, 4, ProcessTaskStatus.DeadlineExceeded);

        worker.consume(10L);

        verify(processWorker).recordResumeAfterDeadline(1L, 5L);
        verify(processWorker, never()).resumeLocked(any(), any());
        verify(requests).delete(request);
    }

    @ParameterizedTest
    @EnumSource(value = ProcessTaskStatus.class, names = {"Completed", "Failed", "Restarted"})
    void declinedResumeOfEndedTaskIsDiscarded(ProcessTaskStatus status) throws Exception {
        var request = stored(ProcessWorkRequestEntity.resume(1L, 5L, now));
        doReturn(false).when(execution).executeBackground(eq(1L), eq(5L), any());
        task(5L, 1L, 4, status);

        worker.consume(10L);

        verify(processWorker, never()).recordResumeAfterDeadline(any(), any());
        verify(requests).delete(request);
    }

    @Test
    void declinedResumeIsHeldWhileTheInstanceDoesNotRun() throws Exception {
        stored(ProcessWorkRequestEntity.resume(1L, 5L, now));
        doReturn(false).when(execution).executeBackground(eq(1L), eq(5L), any());
        task(5L, 1L, 4, ProcessTaskStatus.AwaitingPayment);
        instance.setStatus(ProcessInstanceStatus.Failed);

        worker.consume(10L);

        verify(requests, never()).delete(any());
        verify(requests, never()).deleteById(any());
    }

    @Test
    void unexpectedFailureFailsTheInstanceInsteadOfRetrying() {
        stored(ProcessWorkRequestEntity.transition(1L, 2L, "next", 4, now));
        task(2L, 1L, 3, ProcessTaskStatus.Completed);
        var failure = new IllegalStateException("constraint violated");
        doThrow(failure).when(processWorker).workOnNodeLocked(any());

        assertDoesNotThrow(() -> worker.consume(10L));

        verify(processWorker).recordFailedExecution(1L, null, failure);
        verify(requests, never()).delete(any());
    }

    @Test
    void rolledBackResumeFailsTheWaitingTask() {
        stored(ProcessWorkRequestEntity.resume(1L, 5L, now));
        when(requests.existsById(10L)).thenReturn(true);
        var failure = new UnexpectedRollbackException("rollback-only");
        doThrow(failure).when(processWorker).resumeLocked(1L, 5L);

        assertDoesNotThrow(() -> worker.consume(10L));

        verify(processWorker).recordFailedExecution(1L, 5L, failure);
    }

    @Test
    void transientFailureKeepsRequestForRedispatch() {
        stored(ProcessWorkRequestEntity.transition(1L, 2L, "next", 4, now));
        task(2L, 1L, 3, ProcessTaskStatus.Completed);
        doThrow(new QueryTimeoutException("database busy")).when(processWorker).workOnNodeLocked(any());

        assertDoesNotThrow(() -> worker.consume(10L));

        verify(processWorker, never()).recordFailedExecution(any(), any(), any());
        verify(requests, never()).delete(any());
    }

    static Stream<RuntimeException> lostConnections() {
        var connectionLost = new SQLException("An I/O error occurred while sending to the backend.", "08006");
        return Stream.of(
                // A connection lost while a statement runs.
                new DataAccessResourceFailureException("could not execute statement",
                        new JDBCConnectionException("could not execute statement", connectionLost)),
                // A connection lost or the server shut down while the transaction commits.
                new TransactionSystemException("Could not commit JPA transaction",
                        new RuntimeException("Unable to commit against JDBC Connection", connectionLost)),
                new JpaSystemException(new RuntimeException(new SQLException("terminating connection due to administrator command", "57P01")))
        );
    }

    @ParameterizedTest
    @MethodSource("lostConnections")
    void lostDatabaseConnectionKeepsRequestForRedispatch(RuntimeException failure) {
        stored(ProcessWorkRequestEntity.resume(1L, 5L, now));
        when(requests.existsById(10L)).thenReturn(true);
        doThrow(failure).when(processWorker).resumeLocked(1L, 5L);

        assertDoesNotThrow(() -> worker.consume(10L));

        verify(processWorker, never()).recordFailedExecution(any(), any(), any());
        verify(requests, never()).deleteById(any());
    }

    @Test
    void commitFailureThatWouldRepeatFailsTheInstance() {
        stored(ProcessWorkRequestEntity.transition(1L, 2L, "next", 4, now));
        task(2L, 1L, 3, ProcessTaskStatus.Completed);
        var failure = new TransactionSystemException("Could not commit JPA transaction",
                new RuntimeException(new SQLException("value too long for type character varying(96)", "22001")));
        doThrow(failure).when(processWorker).workOnNodeLocked(any());

        assertDoesNotThrow(() -> worker.consume(10L));

        verify(processWorker).recordFailedExecution(1L, null, failure);
    }

    @Test
    void failureToRecordAFailureIsNotHandedBackToTheBroker() {
        stored(ProcessWorkRequestEntity.transition(1L, 2L, "next", 4, now));
        task(2L, 1L, 3, ProcessTaskStatus.Completed);
        doThrow(new IllegalStateException("constraint violated")).when(processWorker).workOnNodeLocked(any());
        doThrow(new IllegalStateException("database gone")).when(processWorker).recordFailedExecution(any(), any(), any());

        assertDoesNotThrow(() -> worker.consume(10L));
    }

    @Test
    void firstDispatchPublishesAllPendingRequestsAndLaterOnlyStaleOnes() {
        when(requests.findDispatchableIds(0L, now, ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES, ProcessWorkRequestWorker.HELD_TASK_STATUSES, PageRequest.of(0, 2)))
                .thenReturn(List.of(10L));
        when(requests.claimDispatch(10L, now, now)).thenReturn(1);

        worker.dispatch();
        worker.dispatch();

        verify(requestService).publish(10L);
        verify(requests).findDispatchableIds(0L, redispatchBefore, ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES,
                ProcessWorkRequestWorker.HELD_TASK_STATUSES, PageRequest.of(0, 2));
    }

    @Test
    void newBrokerConnectionPublishesAllPendingRequestsAgain() {
        var listener = ArgumentCaptor.forClass(ConnectionListener.class);
        verify(connectionFactory).addConnectionListener(listener.capture());
        worker.dispatch();

        listener.getValue().onCreate(null);
        worker.dispatch();

        verify(requests, times(2)).findDispatchableIds(0L, now, ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES,
                ProcessWorkRequestWorker.HELD_TASK_STATUSES, PageRequest.of(0, 2));
    }

    @Test
    void publishesOnlyClaimedRequestsOfActionableInstancesInPages() {
        worker.dispatch();
        when(requests.findDispatchableIds(0L, redispatchBefore, ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES,
                ProcessWorkRequestWorker.HELD_TASK_STATUSES, PageRequest.of(0, 2))).thenReturn(List.of(10L, 11L));
        when(requests.findDispatchableIds(11L, redispatchBefore, ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES,
                ProcessWorkRequestWorker.HELD_TASK_STATUSES, PageRequest.of(0, 2))).thenReturn(List.of(12L));
        when(requests.claimDispatch(10L, redispatchBefore, now)).thenReturn(1);
        when(requests.claimDispatch(12L, redispatchBefore, now)).thenReturn(1);

        worker.dispatch();

        assertEquals(List.of(ProcessInstanceStatus.Running, ProcessInstanceStatus.Completed, ProcessInstanceStatus.Aborted),
                ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES);
        verify(requestService).publish(10L);
        verify(requestService, never()).publish(11L);
        verify(requestService).publish(12L);
        verify(requests).findDispatchableIds(12L, redispatchBefore, ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES,
                ProcessWorkRequestWorker.HELD_TASK_STATUSES, PageRequest.of(0, 2));
    }

    @Test
    void failedPublicationReleasesTheClaimAndStopsThePass() {
        when(requests.findDispatchableIds(0L, now, ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES, ProcessWorkRequestWorker.HELD_TASK_STATUSES, PageRequest.of(0, 2)))
                .thenReturn(List.of(10L, 11L));
        when(requests.claimDispatch(anyLong(), eq(now), eq(now))).thenReturn(1);
        when(requestService.publish(10L)).thenReturn(false);

        worker.dispatch();

        verify(requests).releaseDispatch(10L);
        verify(requests, never()).claimDispatch(eq(11L), any(), any());
        verify(requestService, never()).publish(11L);
        verify(requests, never()).delete(any());
        verify(requests, never()).deleteById(any());

        // The incomplete first pass is repeated, so every pending request is published once the broker is back.
        worker.dispatch();

        verify(requests, times(2)).findDispatchableIds(0L, now, ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES,
                ProcessWorkRequestWorker.HELD_TASK_STATUSES, PageRequest.of(0, 2));
    }

    @Test
    void newBrokerConnectionDuringAPassPublishesAllPendingRequestsInTheNextPass() {
        var listener = ArgumentCaptor.forClass(ConnectionListener.class);
        verify(connectionFactory).addConnectionListener(listener.capture());
        worker.dispatch();
        when(requests.findDispatchableIds(0L, redispatchBefore, ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES,
                ProcessWorkRequestWorker.HELD_TASK_STATUSES, PageRequest.of(0, 2))).thenAnswer(call -> {
            listener.getValue().onCreate(null);
            return List.of();
        });

        worker.dispatch();
        worker.dispatch();

        verify(requests, times(2)).findDispatchableIds(0L, now, ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES,
                ProcessWorkRequestWorker.HELD_TASK_STATUSES, PageRequest.of(0, 2));
    }

    @Test
    void requestsOfPausedTasksAreHeld() {
        assertEquals(List.of(ProcessTaskStatus.Paused), ProcessWorkRequestWorker.HELD_TASK_STATUSES);
    }

    @Test
    void incompleteFirstDispatchIsRepeated() {
        when(requests.findDispatchableIds(0L, now, ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES, ProcessWorkRequestWorker.HELD_TASK_STATUSES, PageRequest.of(0, 2)))
                .thenThrow(new QueryTimeoutException("database busy")).thenReturn(List.of());

        assertThrows(QueryTimeoutException.class, worker::dispatch);
        worker.dispatch();

        verify(requests, times(2)).findDispatchableIds(0L, now, ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES,
                ProcessWorkRequestWorker.HELD_TASK_STATUSES, PageRequest.of(0, 2));
    }

    private ProcessWorkRequestEntity stored(ProcessWorkRequestEntity request) {
        ReflectionTestUtils.setField(request, "id", 10L);
        when(requests.findById(10L)).thenReturn(Optional.of(request));
        doAnswer(call -> {
            when(requests.findById(10L)).thenReturn(Optional.empty());
            return null;
        }).when(requests).delete(request);
        return request;
    }

    private ProcessInstanceTaskEntity task(Long id, Long instanceId, Integer nodeId, ProcessTaskStatus status) {
        var task = new ProcessInstanceTaskEntity().setId(id).setProcessInstanceId(instanceId).setProcessNodeId(nodeId)
                .setStatus(status);
        when(tasks.findById(id)).thenReturn(Optional.of(task));
        return task;
    }

    private void createsSuccessorOf(Long taskId) {
        doAnswer(call -> {
            when(tasks.existsByPreviousProcessInstanceTaskId(taskId)).thenReturn(true);
            return null;
        }).when(processWorker).workOnNodeLocked(any());
    }
}
