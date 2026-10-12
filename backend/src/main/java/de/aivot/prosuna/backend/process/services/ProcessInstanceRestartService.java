package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessWorkRequestRepository;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Restarts failed process work. The status changes and the request for the work commit together under the instance
 * lock, so concurrent restarts cannot request the same work twice and a lost message cannot lose a restart.
 * Callers authorize the restart.
 */
@Service
public class ProcessInstanceRestartService {
    private final ProcessInstanceRepository instances;
    private final ProcessInstanceTaskRepository tasks;
    private final ProcessWorkRequestRepository requests;
    private final ProcessWorkRequestService workRequests;
    private final ProcessTaskExecutionService execution;
    private final ProcessNodeExecutionLoggerFactory loggers;
    private final EntityManager entityManager;

    public ProcessInstanceRestartService(@Nonnull ProcessInstanceRepository instances,
                                         @Nonnull ProcessInstanceTaskRepository tasks,
                                         @Nonnull ProcessWorkRequestRepository requests,
                                         @Nonnull ProcessWorkRequestService workRequests,
                                         @Nonnull ProcessTaskExecutionService execution,
                                         @Nonnull ProcessNodeExecutionLoggerFactory loggers,
                                         @Nonnull EntityManager entityManager) {
        this.instances = instances;
        this.tasks = tasks;
        this.requests = requests;
        this.workRequests = workRequests;
        this.execution = execution;
        this.loggers = loggers;
        this.entityManager = entityManager;
    }

    /**
     * Restarts a failed instance by starting it again if it has no task yet, by running its failed latest task again,
     * or by releasing the work it holds, e.g. a transition whose target could not be resolved.
     */
    @Nonnull
    public ProcessInstanceEntity restartFailedInstance(@Nonnull Long instanceId, @Nonnull String userId) throws ResponseException {
        return Objects.requireNonNull(execution.inInstance(instanceId, () -> {
            var instance = instances.findById(instanceId).orElseThrow(ResponseException::notFound);
            // Open EntityManager in View can retain entities loaded before the instance lock was acquired.
            entityManager.refresh(instance);
            if (instance.getStatus() != ProcessInstanceStatus.Failed) {
                throw ResponseException.badRequest("Nur Vorgänge im Status 'Fehlgeschlagen' können neu gestartet werden.");
            }
            var latestTask = tasks.findFirstByProcessInstanceIdOrderByStartedDescIdDesc(instanceId).orElse(null);
            if (latestTask != null) entityManager.refresh(latestTask);
            // Requests of a failed instance are held until it runs again.
            var heldRequests = requests.findIdsByProcessInstanceId(instanceId);
            var now = Instant.now();
            Long restartedTaskId = null;
            String message;
            if (latestTask == null) {
                if (instance.getInitialPayload() == null || instance.getInitialPayload().isEmpty()) {
                    throw ResponseException.conflict("Der Vorgang kann nicht neu gestartet werden, weil keine Initialdaten für einen erneuten Start vorliegen.");
                }
                workRequests.enqueueStart(instanceId);
                message = "Der fehlgeschlagene Vorgang wurde manuell neu gestartet, bevor eine erste Aufgabe angelegt wurde.";
            } else if (latestTask.getStatus() == ProcessTaskStatus.Failed) {
                tasks.save(latestTask.setStatus(ProcessTaskStatus.Restarted).setUpdated(now));
                workRequests.enqueueRestart(instanceId, latestTask.getId());
                restartedTaskId = latestTask.getId();
                message = "Der fehlgeschlagene Vorgang wurde manuell neu gestartet. Die letzte fehlgeschlagene Aufgabe wird erneut ausgeführt.";
            } else if (!heldRequests.isEmpty()) {
                message = "Der fehlgeschlagene Vorgang wurde manuell neu gestartet. Die ausstehenden Verarbeitungsschritte werden erneut ausgeführt.";
            } else {
                throw ResponseException.conflict("Der Vorgang kann derzeit nur neu gestartet werden, wenn die letzte Aufgabe fehlgeschlagen ist oder noch keine Aufgabe angelegt wurde.");
            }
            var restarted = instances.save(instance.setStatus(ProcessInstanceStatus.Running).setUpdated(now));
            workRequests.redispatchAfterCommit(heldRequests);
            loggers.create(instanceId, restartedTaskId, userId, null).logf(
                    ProcessNodeExecutionLogLevel.Info,
                    true,
                    true,
                    "Vorgang neu gestartet",
                    message
            );
            return restarted;
        }));
    }

    /** Runs a failed task again. A failed instance runs again as well. */
    @Nonnull
    public ProcessInstanceTaskEntity rerunFailedTask(@Nonnull Long instanceId, @Nonnull Long taskId) throws ResponseException {
        return Objects.requireNonNull(execution.inInstance(instanceId, () -> {
            var task = tasks.findById(taskId).orElseThrow(ResponseException::notFound);
            var instance = instances.findById(instanceId).orElseThrow(ResponseException::notFound);
            // Open EntityManager in View can retain entities loaded before the instance lock was acquired.
            entityManager.refresh(task);
            entityManager.refresh(instance);
            if (!instanceId.equals(task.getProcessInstanceId())) {
                throw ResponseException.notFound();
            }
            if (task.getStatus() != ProcessTaskStatus.Failed) {
                throw ResponseException.badRequest("Nur Aufgaben im Status 'Fehlgeschlagen' können erneut ausgeführt werden.");
            }
            if (instance.getStatus() == ProcessInstanceStatus.Completed || instance.getStatus() == ProcessInstanceStatus.Aborted) {
                throw ResponseException.conflict("Aufgaben eines abgeschlossenen oder abgebrochenen Vorgangs können nicht erneut ausgeführt werden.");
            }
            if (instance.getStatus() != ProcessInstanceStatus.Running && instance.getStatus() != ProcessInstanceStatus.Failed) {
                throw ResponseException.conflict("Die Aufgabe kann derzeit nicht erneut ausgeführt werden.");
            }
            var now = Instant.now();
            if (instance.getStatus() == ProcessInstanceStatus.Failed) {
                // Requests of a failed instance are held until it runs again.
                workRequests.redispatchAfterCommit(requests.findIdsByProcessInstanceId(instanceId));
                instances.save(instance.setStatus(ProcessInstanceStatus.Running).setUpdated(now));
            }
            var restarted = tasks.save(task.setStatus(ProcessTaskStatus.Restarted).setUpdated(now));
            workRequests.enqueueRestart(instanceId, taskId);
            return restarted;
        }));
    }
}
