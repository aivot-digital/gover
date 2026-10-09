package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

/** Serializes execution with expiry before providers, uploads or communications can have side effects. */
@Service
public class ProcessTaskExecutionService {
    private final ProcessInstanceRepository instances;
    private final ProcessInstanceTaskRepository tasks;
    private final ProcessTaskDeadlineService deadlines;
    private final TransactionTemplate transaction;
    private final EntityManager entityManager;

    public ProcessTaskExecutionService(@Nonnull ProcessInstanceRepository instances, @Nonnull ProcessInstanceTaskRepository tasks,
                                        @Nonnull ProcessTaskDeadlineService deadlines, @Nonnull PlatformTransactionManager transactionManager,
                                        @Nonnull EntityManager entityManager) {
        this.instances = instances;
        this.tasks = tasks;
        this.deadlines = deadlines;
        this.transaction = new TransactionTemplate(transactionManager);
        this.entityManager = entityManager;
    }

    @Nullable
    public <T> T inInstance(@Nonnull Long instanceId, @Nonnull Operation<T> operation) throws ResponseException {
        try {
            return transaction.execute(status -> {
                try {
                    instances.lockAccessById(instanceId).orElseThrow(ResponseException::notFound);
                    return operation.run();
                } catch (ResponseException exception) {
                    throw new OperationFailure(exception);
                }
            });
        } catch (OperationFailure failure) {
            throw failure.response;
        }
    }

    @Nullable
    public <T> T execute(@Nonnull Long instanceId, @Nonnull Long taskId, @Nonnull Authorization authorization,
                         @Nonnull Operation<T> operation) throws ResponseException {
        var outcome = inspectAndExecute(instanceId, taskId, authorization, operation);
        // Throw only after committing expiry, so a rejected late HTTP request cannot undo the transition.
        if (outcome.failure != null) throw outcome.failure;
        return outcome.value;
    }

    public void executeBackground(@Nonnull Long instanceId, @Nonnull Long taskId,
                                   @Nonnull Operation<Void> operation) throws ResponseException {
        inspectAndExecute(instanceId, taskId, () -> {}, operation);
    }

    @Nonnull
    private <T> Outcome<T> inspectAndExecute(@Nonnull Long instanceId, @Nonnull Long taskId, @Nonnull Authorization authorization,
                                             @Nonnull Operation<T> operation) throws ResponseException {
        return inInstance(instanceId, () -> {
            var instance = instances.findById(instanceId).orElseThrow(ResponseException::notFound);
            var task = tasks.findById(taskId).orElseThrow(ResponseException::notFound);
            // Open EntityManager in View can retain entities loaded before we acquired the instance lock.
            entityManager.refresh(instance);
            entityManager.refresh(task);
            if (!instanceId.equals(task.getProcessInstanceId())) throw ResponseException.notFound();
            authorization.run();
            deadlines.expireIfDue(instance, task);
            if (task.getStatus() == ProcessTaskStatus.DeadlineExceeded) {
                return new Outcome<>(null, ResponseException.conflictWithDetails(
                        "Die Bearbeitungsfrist dieser Aufgabe ist abgelaufen. Eine Bearbeitung ist nicht mehr möglich.",
                        Map.of("reason", "task_deadline_exceeded")));
            }
            if (instance.getStatus() != ProcessInstanceStatus.Running || task.getStatus().isTerminal()
                    || task.getStatus() == ProcessTaskStatus.Paused) {
                return new Outcome<>(null, ResponseException.conflict("Diese Aufgabe kann derzeit nicht bearbeitet werden."));
            }
            return new Outcome<>(operation.run(), null);
        });
    }

    @FunctionalInterface
    public interface Operation<T> { @Nullable T run() throws ResponseException; }
    @FunctionalInterface
    public interface Authorization { void run() throws ResponseException; }
    private record Outcome<T>(@Nullable T value, @Nullable ResponseException failure) {}
    private static final class OperationFailure extends RuntimeException {
        private final ResponseException response;
        private OperationFailure(ResponseException response) { super(response); this.response = response; }
    }
}
