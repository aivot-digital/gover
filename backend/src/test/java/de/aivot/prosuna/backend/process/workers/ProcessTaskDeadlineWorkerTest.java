package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.process.ProcessTaskExecutionTestSupport;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.services.ProcessTaskDeadlineService;
import de.aivot.prosuna.backend.process.services.ProcessTaskExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class ProcessTaskDeadlineWorkerTest {
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessTaskExecutionService execution = ProcessTaskExecutionTestSupport.passthrough();
    private final Clock clock = mock(Clock.class);
    private final BusinessTime time = new BusinessTime(ZoneOffset.UTC, clock);
    private final Instant now = Instant.parse("2030-05-01T12:00:00Z");
    private final ProcessTaskDeadlineWorker worker = new ProcessTaskDeadlineWorker(tasks, execution, time, 2);

    @BeforeEach
    void setup() {
        when(clock.instant()).thenReturn(now);
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
