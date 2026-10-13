package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.process.ProcessTaskExecutionTestSupport;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessTaskDeadlineContinuationEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessTaskDeadlineContinuationRepository;
import de.aivot.prosuna.backend.process.services.ProcessTaskDeadlineService;
import de.aivot.prosuna.backend.process.services.ProcessTaskExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessTaskDeadlineWorkerTest {
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessTaskDeadlineContinuationRepository pending = mock(ProcessTaskDeadlineContinuationRepository.class);
    private final ProcessTaskExecutionService execution = ProcessTaskExecutionTestSupport.passthrough();
    private final ProcessWorker nextWorker = mock(ProcessWorker.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final Clock clock = mock(Clock.class);
    private final BusinessTime time = new BusinessTime(ZoneOffset.UTC, clock);
    private final Instant now = Instant.parse("2030-05-01T12:00:00Z");
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(1L).setStatus(ProcessInstanceStatus.Running);
    private final ProcessTaskDeadlineContinuationEntity continuation = new ProcessTaskDeadlineContinuationEntity(1L, 2L, 3, 4, "overdue", now);
    private final ProcessTaskDeadlineWorker worker = new ProcessTaskDeadlineWorker(tasks, instances, pending, execution, nextWorker, rabbit, time, 2,
            Duration.ofMinutes(5));

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(continuation, "id", 10L);
        when(instances.findById(1L)).thenReturn(Optional.of(instance));
        when(pending.findById(10L)).thenReturn(Optional.of(continuation));
        when(clock.instant()).thenReturn(now);
    }

    @Test
    void continuationCreatesChildOnceAndDuplicateMessagesDoNothing() throws Exception {
        doAnswer(call -> {
            when(tasks.existsByPreviousProcessInstanceTaskId(2L)).thenReturn(true);
            return null;
        }).when(nextWorker).doWorkAfterDeadline(any());
        doAnswer(call -> {
            when(pending.findById(10L)).thenReturn(Optional.empty());
            return null;
        }).when(pending).delete(continuation);

        worker.continueAfterDeadline(10L);
        worker.continueAfterDeadline(10L);

        verify(nextWorker).doWorkAfterDeadline(new ProcessWorker.DoWorkWorkerPayload(1L, 2L, 3, "overdue", 4, null));
        verify(pending).delete(continuation);
        verify(execution).inInstance(eq(1L), any());
    }

    @Test
    void rechecksPendingRecordAfterAcquiringInstanceLock() throws Exception {
        when(pending.findById(10L)).thenReturn(Optional.of(continuation), Optional.empty());
        worker.continueAfterDeadline(10L);
        verifyNoInteractions(nextWorker);
        verify(pending, never()).delete(any());
    }

    @Test
    void keepsContinuationWhenTargetCannotCreateTask() throws Exception {
        worker.continueAfterDeadline(10L);
        verify(nextWorker).doWorkAfterDeadline(any());
        verify(pending, never()).delete(any());
    }

    @Test
    void keepsContinuationWhenTaskCreationThrows() throws Exception {
        doThrow(new IllegalStateException("database unavailable")).when(nextWorker).doWorkAfterDeadline(any());
        assertThrows(IllegalStateException.class, () -> worker.continueAfterDeadline(10L));
        verify(pending, never()).delete(any());
    }

    @ParameterizedTest
    @EnumSource(value = ProcessInstanceStatus.class, names = {"Paused", "Failed", "Created"})
    void holdsContinuationUntilInstanceRunsAgain(ProcessInstanceStatus status) throws Exception {
        instance.setStatus(status);
        worker.continueAfterDeadline(10L);
        verifyNoInteractions(nextWorker);
        verify(pending, never()).delete(any());
    }

    @ParameterizedTest
    @EnumSource(value = ProcessInstanceStatus.class, names = {"Completed", "Aborted"})
    void discardsContinuationForTerminalInstance(ProcessInstanceStatus status) throws Exception {
        instance.setStatus(status);
        worker.continueAfterDeadline(10L);
        verifyNoInteractions(nextWorker);
        verify(pending).delete(continuation);
    }

    @Test
    void retriesPublishingWithoutRemovingDurableRecord() {
        when(pending.findDispatchable(0L, now.minus(Duration.ofMinutes(5)),
                ProcessTaskDeadlineWorker.DISPATCHABLE_INSTANCE_STATUSES, PageRequest.of(0, 2))).thenReturn(List.of(continuation));
        doThrow(new IllegalStateException("broker unavailable")).doNothing().when(rabbit)
                .convertAndSend(ProcessTaskDeadlineWorker.CONTINUATION_QUEUE, 10L);
        worker.dispatchContinuations();
        verify(pending, never()).markDispatched(anyLong(), any());
        worker.dispatchContinuations();
        verify(rabbit, times(2)).convertAndSend(ProcessTaskDeadlineWorker.CONTINUATION_QUEUE, 10L);
        verify(pending).markDispatched(10L, now);
        verify(pending, never()).delete(any());
    }

    @Test
    void publishesOnlyContinuationsOfActionableInstancesThatWereNotPublishedRecently() {
        var second = new ProcessTaskDeadlineContinuationEntity(1L, 5L, 3, 4, "overdue", now);
        ReflectionTestUtils.setField(second, "id", 11L);
        var dispatchedBefore = now.minus(Duration.ofMinutes(5));
        when(pending.findDispatchable(0L, dispatchedBefore, ProcessTaskDeadlineWorker.DISPATCHABLE_INSTANCE_STATUSES,
                PageRequest.of(0, 2))).thenReturn(List.of(continuation, second));
        when(pending.findDispatchable(11L, dispatchedBefore, ProcessTaskDeadlineWorker.DISPATCHABLE_INSTANCE_STATUSES,
                PageRequest.of(0, 2))).thenReturn(List.of());

        worker.dispatchContinuations();

        assertEquals(List.of(ProcessInstanceStatus.Running, ProcessInstanceStatus.Completed, ProcessInstanceStatus.Aborted),
                ProcessTaskDeadlineWorker.DISPATCHABLE_INSTANCE_STATUSES);
        verify(rabbit).convertAndSend(ProcessTaskDeadlineWorker.CONTINUATION_QUEUE, 10L);
        verify(rabbit).convertAndSend(ProcessTaskDeadlineWorker.CONTINUATION_QUEUE, 11L);
        verify(pending).markDispatched(10L, now);
        verify(pending).markDispatched(11L, now);
    }

    @Test
    void scannerAdvancesPastSoftDeadlinesAndIndividualFailures() throws Exception {
        when(tasks.findDueDeadlineIds(0L, now, ProcessTaskDeadlineService.EXPIRABLE_STATUSES, PageRequest.of(0, 2)))
                .thenReturn(List.of(2L, 3L));
        when(tasks.findDueDeadlineIds(3L, now, ProcessTaskDeadlineService.EXPIRABLE_STATUSES, PageRequest.of(0, 2)))
                .thenReturn(List.of(4L));
        for (var id : List.of(2L, 3L, 4L)) when(tasks.findInstanceIdById(id)).thenReturn(Optional.of(1L));
        doThrow(new IllegalStateException("one task fails")).when(execution).executeBackground(eq(1L), eq(3L), any());

        worker.checkDeadlines();

        for (var id : List.of(2L, 3L, 4L)) verify(execution).executeBackground(eq(1L), eq(id), any());
        verify(tasks).findDueDeadlineIds(4L, now, ProcessTaskDeadlineService.EXPIRABLE_STATUSES, PageRequest.of(0, 2));
    }
}
