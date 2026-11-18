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
import java.util.concurrent.ConcurrentHashMap;
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

    private final RefreshedGauge processGauge;
    private final RefreshedGauge versionGauge;
    private final RefreshedGauge nodeGauge;
    private final RefreshedGauge unresolvedNodeGauge;
    private final RefreshedGauge nodesSavedWithErrorsGauge;
    private final RefreshedGauge workPendingGauge;
    private final RefreshedGauge workOldestAgeGauge;
    private final RefreshedGauge instanceGauge;
    private final RefreshedGauge taskGauge;

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
        this.processGauge = new RefreshedGauge(MultiGauge.builder("prosuna.processes").register(meterRegistry));
        this.versionGauge = new RefreshedGauge(MultiGauge.builder("prosuna.process.versions").register(meterRegistry));
        this.nodeGauge = new RefreshedGauge(MultiGauge.builder("prosuna.process.nodes").register(meterRegistry));
        this.unresolvedNodeGauge = new RefreshedGauge(MultiGauge.builder("prosuna.process.nodes.unresolved").register(meterRegistry));
        this.nodesSavedWithErrorsGauge = new RefreshedGauge(MultiGauge.builder("prosuna.process.nodes.saved.with.errors").register(meterRegistry));
        this.workPendingGauge = new RefreshedGauge(MultiGauge.builder("prosuna.process.work.pending").register(meterRegistry));
        this.workOldestAgeGauge = new RefreshedGauge(MultiGauge.builder("prosuna.process.work.oldest.age").baseUnit("seconds").register(meterRegistry));
        this.instanceGauge = new RefreshedGauge(MultiGauge.builder("prosuna.process.instances").register(meterRegistry));
        this.taskGauge = new RefreshedGauge(MultiGauge.builder("prosuna.process.tasks").register(meterRegistry));
    }

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void refreshEngineState() {
        try {
            var backlog = workRequests.summarizeBacklog(ProcessWorkRequestWorker.DISPATCHABLE_INSTANCE_STATUSES,
                    ProcessWorkRequestWorker.HELD_TASK_STATUSES);
            var oldest = backlog.getOldestCreated();
            workPendingGauge.publish(List.of(row(backlog.getTotal())));
            workOldestAgeGauge.publish(List.of(row(
                    oldest == null ? 0 : Math.max(0, Duration.between(oldest, time.now()).toSeconds()))));

            var instanceCounts = instances.countRealInstancesByStatus(OPEN_INSTANCE_STATUSES).stream()
                    .collect(Collectors.toMap(count -> count.getStatus(), count -> count.getTotal()));
            instanceGauge.publish(OPEN_INSTANCE_STATUSES.stream()
                    .map(status -> row(instanceCounts.getOrDefault(status, 0L), "status", status.name()))
                    .toList());

            var taskCounts = tasks.countOpenTasksOfRealInstancesByStatus().stream()
                    .collect(Collectors.toMap(count -> count.getStatus(), count -> count.getTotal()));
            taskGauge.publish(OPEN_TASK_STATUSES.stream()
                    .map(status -> row(taskCounts.getOrDefault(status.getDatabaseValue(), 0L), "status", status.name()))
                    .toList());
        } catch (RuntimeException exception) {
            // The gauges keep their last values; the next run tries again.
            logger.warn("Could not refresh the process engine metrics", exception);
        }
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void refreshProcessSetups() {
        try {
            var published = processes.countByPublishedVersionIsNotNull();
            processGauge.publish(List.of(row(published, "state", "published"),
                    row(processes.count() - published, "state", "unpublished")));

            var versionCounts = totalsByStatus(versions.countByStatus());
            versionGauge.publish(Arrays.stream(ProcessVersionStatus.values())
                    .map(status -> row(versionCounts.getOrDefault(status, 0L), "status", status.name()))
                    .toList());

            var nodeRows = new ArrayList<Row>();
            var unresolved = 0L;
            for (var count : nodes.countByDefinition(ProcessVersionStatus.Published)) {
                nodeRows.add(row(count.getTotal(), "definition", count.getDefinitionKey(),
                        "definition_version", String.valueOf(count.getDefinitionVersion())));
                if (definitions.getProcessNodeDefinition(count.getDefinitionKey(), count.getDefinitionVersion()).isEmpty()) {
                    unresolved += count.getTotal();
                }
            }
            nodeGauge.publish(nodeRows);
            unresolvedNodeGauge.publish(List.of(row(unresolved)));

            var errorCounts = totalsByStatus(nodes.countSavedWithErrorsByVersionStatus());
            nodesSavedWithErrorsGauge.publish(Arrays.stream(ProcessVersionStatus.values())
                    .map(status -> row(errorCounts.getOrDefault(status, 0L), "status", status.name()))
                    .toList());
        } catch (RuntimeException exception) {
            // The gauges keep their last values; the next run tries again.
            logger.warn("Could not refresh the process setup metrics", exception);
        }
    }

    private static Map<ProcessVersionStatus, Long> totalsByStatus(List<ProcessVersionStatusCountProjection> counts) {
        return counts.stream().collect(Collectors.toMap(ProcessVersionStatusCountProjection::getStatus,
                ProcessVersionStatusCountProjection::getTotal));
    }

    private static Row row(long value, String... tags) {
        return new Row(Tags.of(tags), value);
    }

    private record Row(@Nonnull Tags tags, long value) {
    }

    /**
     * A gauge whose rows read their values from a map, so a refresh only updates the map. Overwriting the rows instead
     * would remove and re-register their meters; for an unlabeled gauge that swaps its Prometheus collector, and a
     * scrape running at that moment can collect both and fail with a DuplicateLabelsException.
     */
    private static final class RefreshedGauge {
        private final MultiGauge gauge;
        private final Map<Tags, Long> values = new ConcurrentHashMap<>();

        private RefreshedGauge(@Nonnull MultiGauge gauge) {
            this.gauge = gauge;
        }

        private void publish(@Nonnull List<Row> rows) {
            var next = rows.stream().collect(Collectors.toMap(Row::tags, Row::value));
            values.putAll(next);
            // Without overwrite, existing meters stay registered; only new rows are added and vanished rows removed.
            gauge.register(next.keySet().stream()
                    .<MultiGauge.Row<?>>map(tags -> MultiGauge.Row.of(tags, values, current -> current.getOrDefault(tags, 0L)))
                    .toList(), false);
            values.keySet().retainAll(next.keySet());
        }
    }
}
