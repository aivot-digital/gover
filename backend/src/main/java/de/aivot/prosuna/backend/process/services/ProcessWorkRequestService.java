package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.lib.services.StoredWorkSignals;
import de.aivot.prosuna.backend.process.entities.ProcessWorkRequestEntity;
import de.aivot.prosuna.backend.process.enums.ProcessWorkRequestKind;
import de.aivot.prosuna.backend.process.repositories.ProcessWorkRequestRepository;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

/**
 * Records pending process work in the transaction that requires it and publishes the request IDs after the commit.
 *
 * <p>The database row is the only record of the work. Publishing is a best-effort wake-up signal: if it fails or the
 * message is lost, the dispatcher in {@code ProcessWorkRequestWorker} publishes the request again. A rolled-back
 * transaction publishes nothing.
 */
@Service
public class ProcessWorkRequestService {
    public static final String QUEUE = "process-work-request-queue";

    private final ProcessWorkRequestRepository requests;
    private final StoredWorkSignals signals;
    private final BusinessTime time;

    public ProcessWorkRequestService(@Nonnull ProcessWorkRequestRepository requests, @Nonnull StoredWorkSignals signals,
                                     @Nonnull BusinessTime time) {
        this.requests = requests;
        this.signals = signals;
        this.time = time;
    }

    /** Requests the first task of a started instance. The caller must hold the instance lock. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueStart(@Nonnull Long processInstanceId) {
        requireTransaction();
        if (requests.existsByKindAndProcessInstanceId(ProcessWorkRequestKind.Start, processInstanceId)) return;
        enqueue(ProcessWorkRequestEntity.start(processInstanceId, time.now()));
    }

    /** Requests the successor of a completed or expired task. The caller must hold the instance lock. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueTransition(@Nonnull Long processInstanceId, @Nonnull Long predecessorTaskId,
                                  @Nonnull String viaPort, @Nonnull Integer toNodeId) {
        requireTransaction();
        if (requests.existsByKindAndTaskId(ProcessWorkRequestKind.Transition, predecessorTaskId)) return;
        enqueue(ProcessWorkRequestEntity.transition(processInstanceId, predecessorTaskId, viaPort, toNodeId, time.now()));
    }

    /** Requests another attempt of a failed task. The caller must hold the instance lock. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueRestart(@Nonnull Long processInstanceId, @Nonnull Long failedTaskId) {
        requireTransaction();
        if (requests.existsByKindAndTaskId(ProcessWorkRequestKind.Restart, failedTaskId)) return;
        enqueue(ProcessWorkRequestEntity.restart(processInstanceId, failedTaskId, time.now()));
    }

    /**
     * Requests resuming a waiting task. Resuming re-reads the current state, so repeated requests are harmless and
     * the caller does not need the instance lock.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueResume(@Nonnull Long processInstanceId, @Nonnull Long taskId) {
        requireTransaction();
        enqueue(ProcessWorkRequestEntity.resume(processInstanceId, taskId, time.now()));
    }

    /** Publishes held requests again once the current transaction has released their instance. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void redispatchAfterCommit(@Nonnull Collection<Long> requestIds) {
        requireTransaction();
        publishAfterCommit(List.copyOf(requestIds));
    }

    /** Publishes a request ID now. Returns whether the broker accepted the message. */
    public boolean publish(@Nonnull Long requestId) {
        return signals.publish(QUEUE, requestId);
    }

    private void enqueue(@Nonnull ProcessWorkRequestEntity request) {
        var saved = requests.save(request);
        publishAfterCommit(List.of(saved.getId()));
    }

    private void publishAfterCommit(@Nonnull List<Long> requestIds) {
        if (requestIds.isEmpty()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // A request that cannot be published stays pending, so the dispatcher publishes it again later.
                for (var requestId : requestIds) {
                    publish(requestId);
                }
            }
        });
    }

    private static void requireTransaction() {
        // Also enforced without a Spring proxy: publishing before a commit could run work for rolled-back changes.
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Process work can only be requested within a transaction.");
        }
    }
}
