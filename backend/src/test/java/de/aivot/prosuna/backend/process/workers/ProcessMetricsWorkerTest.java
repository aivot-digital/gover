package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.projections.ProcessInstanceStatusCountProjection;
import de.aivot.prosuna.backend.process.projections.ProcessNodeDefinitionCountProjection;
import de.aivot.prosuna.backend.process.projections.ProcessTaskStatusCountProjection;
import de.aivot.prosuna.backend.process.projections.ProcessVersionStatusCountProjection;
import de.aivot.prosuna.backend.process.projections.ProcessWorkBacklogProjection;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessWorkRequestRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessMetricsWorkerTest {
    private static final Instant NOW = Instant.parse("2030-05-01T12:00:00Z");

    private final ProcessRepository processes = mock(ProcessRepository.class);
    private final ProcessVersionRepository versions = mock(ProcessVersionRepository.class);
    private final ProcessNodeRepository nodes = mock(ProcessNodeRepository.class);
    private final ProcessNodeDefinitionService definitions = mock(ProcessNodeDefinitionService.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessWorkRequestRepository workRequests = mock(ProcessWorkRequestRepository.class);
    private final Clock clock = mock(Clock.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ProcessMetricsWorker worker = new ProcessMetricsWorker(processes, versions, nodes, definitions, instances,
            tasks, workRequests, new BusinessTime(ZoneOffset.UTC, clock), meterRegistry);

    @BeforeEach
    void setup() {
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    void publishesEngineStateAndFillsMissingStatusesWithZero() {
        givenBacklog(3, NOW.minusSeconds(90));
        var running = mock(ProcessInstanceStatusCountProjection.class);
        when(running.getStatus()).thenReturn(ProcessInstanceStatus.Running);
        when(running.getTotal()).thenReturn(5L);
        when(instances.countRealInstancesByStatus(any())).thenReturn(List.of(running));
        var awaitingStaff = mock(ProcessTaskStatusCountProjection.class);
        when(awaitingStaff.getStatus()).thenReturn(ProcessTaskStatus.AwaitingStaff.getDatabaseValue());
        when(awaitingStaff.getTotal()).thenReturn(2L);
        when(tasks.countOpenTasksOfRealInstancesByStatus()).thenReturn(List.of(awaitingStaff));

        worker.refreshEngineState();

        assertEquals(3, gauge("prosuna.process.work.pending"));
        assertEquals(90, gauge("prosuna.process.work.oldest.age"));
        assertEquals(5, gauge("prosuna.process.instances", "status", "Running"));
        assertEquals(0, gauge("prosuna.process.instances", "status", "Failed"));
        assertEquals(2, gauge("prosuna.process.tasks", "status", "AwaitingStaff"));
        assertEquals(0, gauge("prosuna.process.tasks", "status", "Paused"));
    }

    @Test
    void emptyBacklogHasNoAge() {
        givenBacklog(0, null);

        worker.refreshEngineState();

        assertEquals(0, gauge("prosuna.process.work.oldest.age"));
    }

    @Test
    void failedQueryKeepsTheLastValuesAndDoesNotEscape() {
        givenBacklog(3, NOW);
        worker.refreshEngineState();
        when(workRequests.summarizeBacklog(any(), any())).thenThrow(new QueryTimeoutException("database busy"));

        assertDoesNotThrow(worker::refreshEngineState);

        assertEquals(3, gauge("prosuna.process.work.pending"));
    }

    @Test
    void refreshUpdatesTheRegisteredMeterInsteadOfReplacingIt() {
        // Replacing meters lets a concurrent Prometheus scrape see the gauge twice and fail.
        givenBacklog(3, NOW);
        worker.refreshEngineState();
        var meter = meterRegistry.get("prosuna.process.work.pending").gauge();
        givenBacklog(8, NOW);

        worker.refreshEngineState();

        assertSame(meter, meterRegistry.get("prosuna.process.work.pending").gauge());
        assertEquals(8, meter.value());
    }

    @Test
    void removesNodeRowsThatNoLongerExist() {
        var known = nodeCount("known", 1, 7);
        var removedPlugin = nodeCount("removed-plugin", 2, 2);
        when(nodes.countByDefinition(ProcessVersionStatus.Published)).thenReturn(List.of(known, removedPlugin));
        worker.refreshProcessSetups();
        var knownUpdated = nodeCount("known", 1, 9);
        when(nodes.countByDefinition(ProcessVersionStatus.Published)).thenReturn(List.of(knownUpdated));

        worker.refreshProcessSetups();

        assertEquals(9, gauge("prosuna.process.nodes", "definition", "known", "definition_version", "1"));
        assertNull(meterRegistry.find("prosuna.process.nodes").tags("definition", "removed-plugin").gauge());
    }

    @Test
    void publishesProcessSetups() {
        when(processes.countByPublishedVersionIsNotNull()).thenReturn(4L);
        when(processes.count()).thenReturn(6L);
        var drafted = versionCount(ProcessVersionStatus.Drafted, 3);
        when(versions.countByStatus()).thenReturn(List.of(drafted));
        var known = nodeCount("known", 1, 7);
        var removedPlugin = nodeCount("removed-plugin", 2, 2);
        when(nodes.countByDefinition(ProcessVersionStatus.Published)).thenReturn(List.of(known, removedPlugin));
        doReturn(Optional.of(mock(ProcessNodeDefinition.class))).when(definitions).getProcessNodeDefinition("known", 1);
        var draftedWithErrors = versionCount(ProcessVersionStatus.Drafted, 1);
        when(nodes.countSavedWithErrorsByVersionStatus()).thenReturn(List.of(draftedWithErrors));

        worker.refreshProcessSetups();

        assertEquals(4, gauge("prosuna.processes", "state", "published"));
        assertEquals(2, gauge("prosuna.processes", "state", "unpublished"));
        assertEquals(3, gauge("prosuna.process.versions", "status", "Drafted"));
        assertEquals(0, gauge("prosuna.process.versions", "status", "Revoked"));
        assertEquals(7, gauge("prosuna.process.nodes", "definition", "known", "definition_version", "1"));
        assertEquals(2, gauge("prosuna.process.nodes.unresolved"));
        assertEquals(1, gauge("prosuna.process.nodes.saved.with.errors", "status", "Drafted"));
        assertEquals(0, gauge("prosuna.process.nodes.saved.with.errors", "status", "Published"));
    }

    private double gauge(String name, String... tags) {
        return meterRegistry.get(name).tags(tags).gauge().value();
    }

    private void givenBacklog(long total, Instant oldest) {
        var backlog = mock(ProcessWorkBacklogProjection.class);
        when(backlog.getTotal()).thenReturn(total);
        when(backlog.getOldestCreated()).thenReturn(oldest);
        when(workRequests.summarizeBacklog(ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES,
                ProcessWorkRequestWorker.HELD_TASK_STATUSES)).thenReturn(backlog);
    }

    private static ProcessVersionStatusCountProjection versionCount(ProcessVersionStatus status, long total) {
        var count = mock(ProcessVersionStatusCountProjection.class);
        when(count.getStatus()).thenReturn(status);
        when(count.getTotal()).thenReturn(total);
        return count;
    }

    private static ProcessNodeDefinitionCountProjection nodeCount(String key, int version, long total) {
        var count = mock(ProcessNodeDefinitionCountProjection.class);
        when(count.getDefinitionKey()).thenReturn(key);
        when(count.getDefinitionVersion()).thenReturn(version);
        when(count.getTotal()).thenReturn(total);
        return count;
    }
}
