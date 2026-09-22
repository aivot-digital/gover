package de.aivot.prosuna.backend.communication.services;

import java.util.function.Supplier;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationAlreadyPendingException;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.communication.entities.CommunicationDeliveryEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.models.CommunicationDeliveryStatus;
import de.aivot.prosuna.backend.communication.repositories.CommunicationDeliveryRepository;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Short, independent transactions keep the attempt and transport receipt durable across failures. */
@Service
public class CommunicationDeliveryStore {
    private final CommunicationDeliveryRepository repository;
    private final ProcessInstanceTaskRepository tasks;
    private final ProcessInstanceRepository instances;

    public CommunicationDeliveryStore(CommunicationDeliveryRepository repository, ProcessInstanceTaskRepository tasks,
                                      ProcessInstanceRepository instances) {
        this.repository = repository;
        this.tasks = tasks;
        this.instances = instances;
    }

    @Nullable
    @Transactional
    public ProcessInstanceTaskEntity claimNextTask(
            @Nonnull UUID deliveryId,
            @Nonnull Supplier<ProcessInstanceTaskEntity> createTask) {
        var delivery = repository.findByIdForUpdate(deliveryId).orElseThrow();
        if (!delivery.getContinuationApplied() || delivery.getNextWork() == null || delivery.getNextTaskId() != null) return null;
        var instance = instances.findByIdForUpdate(delivery.getProcessInstanceId()).orElseThrow();
        if (instance.getStatus() != ProcessInstanceStatus.Running) {
            if (instance.getStatus() == ProcessInstanceStatus.Aborted
                    || instance.getStatus() == ProcessInstanceStatus.Completed) delivery.setNextWork(null);
            return null;
        }
        // The outbox may be delivered more than once. Allocate its successor exactly once under this lock.
        var task = createTask.get();
        delivery.setNextTaskId(task.getId());
        delivery.setNextWork(null);
        repository.saveAndFlush(delivery);
        return task;
    }

    @Nonnull
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommunicationDeliveryEntity begin(@Nonnull CommunicationProviderEntity provider,
                                              @Nullable Long taskId,
                                              @Nonnull Map<String, Object> continuation) {
        Long instanceId = null;
        if (taskId != null) {
            var task = tasks.findByIdForUpdate(taskId).orElseThrow();
            if (task.getStatus() != ProcessTaskStatus.Running || repository.findByTaskId(taskId).isPresent()) {
                throw new CommunicationAlreadyPendingException();
            }
            instanceId = task.getProcessInstanceId();
            task.setStatus(ProcessTaskStatus.AwaitingCommunication).setUpdated(Instant.now());
            tasks.save(task);
        }
        var now = Instant.now();
        return repository.saveAndFlush(new CommunicationDeliveryEntity()
                .setId(UUID.randomUUID()).setProviderId(provider.getId()).setTaskId(taskId).setProcessInstanceId(instanceId)
                .setDefinitionKey(provider.getCommunicationProviderDefinitionKey())
                .setDefinitionVersion(provider.getCommunicationProviderDefinitionVersion())
                .setConfiguration(provider.getConfiguration().clone())
                .setStatus(CommunicationDeliveryStatus.Sending).setReceipt(Map.of()).setContinuation(continuation)
                .setCreated(now).setUpdated(now).setNextCheckAt(now.plusSeconds(300)).setCheckFailures(0));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void submitted(@Nonnull UUID id, @Nonnull Map<String, Object> receipt) {
        var delivery = repository.findByIdForUpdate(id).orElseThrow();
        delivery.setReceipt(receipt).setStatus(CommunicationDeliveryStatus.Submitted)
                .setUpdated(Instant.now()).setNextCheckAt(Instant.now()).setStatusMessage(null);
        repository.saveAndFlush(delivery);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(@Nonnull UUID id, boolean uncertain, @Nonnull String message) {
        var delivery = repository.findByIdForUpdate(id).orElseThrow();
        delivery.setStatus(uncertain ? CommunicationDeliveryStatus.Unknown : CommunicationDeliveryStatus.Failed)
                .setStatusMessage(message).setUpdated(Instant.now()).setNextCheckAt(Instant.now());
        repository.saveAndFlush(delivery);
    }
}
