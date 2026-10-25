package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionBrokenImplementation;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.workers.ProcessTaskDeadlineResultHandler;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessTaskDeadlineService {
    public static final List<ProcessTaskStatus> EXPIRABLE_STATUSES = List.of(ProcessTaskStatus.Running,
            ProcessTaskStatus.InProgress, ProcessTaskStatus.AwaitingStaff, ProcessTaskStatus.AwaitingCustomer,
            ProcessTaskStatus.AwaitingPayment);
    private final ProcessNodeRepository nodes;
    private final ProcessNodeService nodeService;
    private final ProcessEdgeRepository edges;
    private final ProcessInstanceRepository instances;
    private final ProcessInstanceTaskRepository tasks;
    private final ProcessNodeExecutionLoggerFactory loggers;
    private final ProcessTaskDeadlineResultHandler handler;
    private final BusinessTime time;

    public ProcessTaskDeadlineService(@Nonnull ProcessNodeRepository nodes, @Nonnull ProcessNodeService nodeService,
                                      @Nonnull ProcessEdgeRepository edges, @Nonnull ProcessInstanceRepository instances,
                                      @Nonnull ProcessInstanceTaskRepository tasks, @Nonnull ProcessNodeExecutionLoggerFactory loggers,
                                      @Nonnull ProcessTaskDeadlineResultHandler handler, @Nonnull BusinessTime time) {
        this.nodes = nodes;
        this.nodeService = nodeService;
        this.edges = edges;
        this.instances = instances;
        this.tasks = tasks;
        this.loggers = loggers;
        this.handler = handler;
        this.time = time;
    }

    /** Called only after acquiring the owning instance's transaction lock. */
    public void expireIfDue(@Nonnull ProcessInstanceEntity instance, @Nonnull ProcessInstanceTaskEntity task) {
        var now = time.now();
        if (instance.getStatus() != ProcessInstanceStatus.Running || !EXPIRABLE_STATUSES.contains(task.getStatus())
                || task.getDeadline() == null || task.getDeadlineKey() == null || !task.getDeadlineStrict()
                || now.isBefore(task.getDeadline())) {
            return;
        }
        var logger = loggers.create(instance.getId(), task.getId(), null, null);
        try {
            var node = nodes.findById(task.getProcessNodeId()).orElseThrow(() ->
                    new ProcessNodeExecutionExceptionBrokenImplementation("Das Prozesselement der fälligen Aufgabe wurde nicht gefunden."));
            var setting = node.getDeadlines().get(task.getDeadlineKey());
            if (setting == null || setting.days() == null || !setting.strict()) {
                // The element no longer has a hard deadline for this phase. Treat the deadline as soft from now on,
                // so the deadline worker stops selecting the task on every scan.
                tasks.save(task.setDeadlineStrict(false));
                return;
            }
            var resolution = nodeService.resolvePorts(node);
            if (!resolution.problems().isEmpty()) {
                throw new ProcessNodeExecutionExceptionBrokenImplementation(String.join(" ", resolution.problems()));
            }
            var definition = resolution.deadlineDefinitions().stream()
                    .filter(candidate -> candidate.key().equals(task.getDeadlineKey())).findFirst()
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionBrokenImplementation("Die aktive Frist wird vom Prozesselement nicht angeboten."));
            var port = resolution.ports().stream()
                    .filter(candidate -> task.getDeadlineKey().equals(candidate.deadlineKey())).findFirst()
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionBrokenImplementation("Für die automatische Beendigung bei Fristüberschreitung fehlt der Prozesspfad."));
            var edge = edges.findByFromNodeIdAndViaPort(node.getId(), port.key()).orElseThrow(() ->
                    new ProcessNodeExecutionExceptionBrokenImplementation("Für den Prozesspfad bei Fristüberschreitung „%s“ fehlt die Verbindung.", port.label()));
            if (nodes.findById(edge.getToNodeId()).isEmpty()) {
                throw new ProcessNodeExecutionExceptionBrokenImplementation("Das Ziel des Fristausgangs wurde nicht gefunden.");
            }
            handler.handle(task, definition, edge, now, logger);
        } catch (ProcessNodeExecutionExceptionBrokenImplementation exception) {
            task.setStatus(ProcessTaskStatus.Failed).setFinished(now);
            instance.setStatus(ProcessInstanceStatus.Failed);
            tasks.save(task);
            instances.save(instance);
            logger.logException(exception);
        }
    }
}
