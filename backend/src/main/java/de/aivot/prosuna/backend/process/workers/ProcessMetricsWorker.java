package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.projections.ProcessVersionStatusCountProjection;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessWorkRequestRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Publishes the state of the process engine and the process setups as gauges. The values are read on a schedule, so
 * Prometheus scrapes never cause database queries. Every backend instance reports the same values.
 */
@Service
public class ProcessMetricsWorker {
    private static final Logger logger = LoggerFactory.getLogger(ProcessMetricsWorker.class);
    // Completed and aborted instances only ever grow, so they are left out to keep the count cheap.
    private static final List<ProcessInstanceStatus> OPEN_INSTANCE_STATUSES = List.of(ProcessInstanceStatus.Created,
            ProcessInstanceStatus.Running, ProcessInstanceStatus.Paused, ProcessInstanceStatus.Failed);
    private static final List<ProcessTaskStatus> OPEN_TASK_STATUSES = Arrays.stream(ProcessTaskStatus.values())
            .filter(status -> !status.isTerminal())
            .toList();

    private final ProcessRepository processes;
    private final ProcessVersionRepository versions;
    private final ProcessNodeRepository nodes;
    private final ProcessNodeDefinitionService definitions;
    private final ProcessInstanceRepository instances;
    private final ProcessInstanceTaskRepository tasks;
    private final ProcessWorkRequestRepository workRequests;
    private final BusinessTime time;

    private final MultiGauge processGauge;
    private final MultiGauge versionGauge;
    private final MultiGauge nodeGauge;
    private final MultiGauge unresolvedNodeGauge;
    private final MultiGauge nodesSavedWithErrorsGauge;
    private final MultiGauge workPendingGauge;
    private final MultiGauge workOldestAgeGauge;
    private final MultiGauge instanceGauge;
    private final MultiGauge taskGauge;

    public ProcessMetricsWorker(@Nonnull ProcessRepository processes, @Nonnull ProcessVersionRepository versions,
                                @Nonnull ProcessNodeRepository nodes, @Nonnull ProcessNodeDefinitionService definitions,
                                @Nonnull ProcessInstanceRepository instances, @Nonnull ProcessInstanceTaskRepository tasks,
                                @Nonnull ProcessWorkRequestRepository workRequests, @Nonnull BusinessTime time,
                                @Nonnull MeterRegistry meterRegistry) {
        this.processes = processes;
        this.versions = versions;
        this.nodes = nodes;
        this.definitions = definitions;
        this.instances = instances;
        this.tasks = tasks;
        this.workRequests = workRequests;
        this.time = time;
        this.processGauge = MultiGauge.builder("prosuna.processes").register(meterRegistry);
        this.versionGauge = MultiGauge.builder("prosuna.process.versions").register(meterRegistry);
        this.nodeGauge = MultiGauge.builder("prosuna.process.nodes").register(meterRegistry);
        this.unresolvedNodeGauge = MultiGauge.builder("prosuna.process.nodes.unresolved").register(meterRegistry);
        this.nodesSavedWithErrorsGauge = MultiGauge.builder("prosuna.process.nodes.saved.with.errors").register(meterRegistry);
        this.workPendingGauge = MultiGauge.builder("prosuna.process.work.pending").register(meterRegistry);
        this.workOldestAgeGauge = MultiGauge.builder("prosuna.process.work.oldest.age").baseUnit("seconds").register(meterRegistry);
        this.instanceGauge = MultiGauge.builder("prosuna.process.instances").register(meterRegistry);
        this.taskGauge = MultiGauge.builder("prosuna.process.tasks").register(meterRegistry);
    }

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void refreshEngineState() {
        try {
            var backlog = workRequests.summarizeBacklog(ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES,
                    ProcessWorkRequestWorker.HELD_TASK_STATUSES);
            var oldest = backlog.getOldestCreated();
            workPendingGauge.register(List.of(row(backlog.getTotal())), true);
            workOldestAgeGauge.register(List.of(row(
                    oldest == null ? 0 : Math.max(0, Duration.between(oldest, time.now()).toSeconds()))), true);

            var instanceCounts = instances.countRealInstancesByStatus(OPEN_INSTANCE_STATUSES).stream()
                    .collect(Collectors.toMap(count -> count.getStatus(), count -> count.getTotal()));
            instanceGauge.register(OPEN_INSTANCE_STATUSES.stream()
                    .<MultiGauge.Row<?>>map(status -> row(instanceCounts.getOrDefault(status, 0L), "status", status.name()))
                    .toList(), true);

            var taskCounts = tasks.countOpenTasksOfRealInstancesByStatus().stream()
                    .collect(Collectors.toMap(count -> count.getStatus(), count -> count.getTotal()));
            taskGauge.register(OPEN_TASK_STATUSES.stream()
                    .<MultiGauge.Row<?>>map(status -> row(taskCounts.getOrDefault(status.getDatabaseValue(), 0L), "status", status.name()))
                    .toList(), true);
        } catch (RuntimeException exception) {
            // The gauges keep their last values; the next run tries again.
            logger.warn("Could not refresh the process engine metrics", exception);
        }
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void refreshProcessSetups() {
        try {
            var published = processes.countByPublishedVersionIsNotNull();
            processGauge.register(List.of(row(published, "state", "published"),
                    row(processes.count() - published, "state", "unpublished")), true);

            var versionCounts = totalsByStatus(versions.countByStatus());
            versionGauge.register(Arrays.stream(ProcessVersionStatus.values())
                    .<MultiGauge.Row<?>>map(status -> row(versionCounts.getOrDefault(status, 0L), "status", status.name()))
                    .toList(), true);

            var nodeRows = new ArrayList<MultiGauge.Row<?>>();
            var unresolved = 0L;
            for (var count : nodes.countByDefinition(ProcessVersionStatus.Published)) {
                nodeRows.add(row(count.getTotal(), "definition", count.getDefinitionKey(),
                        "definition_version", String.valueOf(count.getDefinitionVersion())));
                if (definitions.getProcessNodeDefinition(count.getDefinitionKey(), count.getDefinitionVersion()).isEmpty()) {
                    unresolved += count.getTotal();
                }
            }
            nodeGauge.register(nodeRows, true);
            unresolvedNodeGauge.register(List.of(row(unresolved)), true);

            var errorCounts = totalsByStatus(nodes.countSavedWithErrorsByVersionStatus());
            nodesSavedWithErrorsGauge.register(Arrays.stream(ProcessVersionStatus.values())
                    .<MultiGauge.Row<?>>map(status -> row(errorCounts.getOrDefault(status, 0L), "status", status.name()))
                    .toList(), true);
        } catch (RuntimeException exception) {
            // The gauges keep their last values; the next run tries again.
            logger.warn("Could not refresh the process setup metrics", exception);
        }
    }

    private static Map<ProcessVersionStatus, Long> totalsByStatus(List<ProcessVersionStatusCountProjection> counts) {
        return counts.stream().collect(Collectors.toMap(ProcessVersionStatusCountProjection::getStatus,
                ProcessVersionStatusCountProjection::getTotal));
    }

    private static MultiGauge.Row<?> row(long value, String... tags) {
        return MultiGauge.Row.of(Tags.of(tags), value);
    }
}
