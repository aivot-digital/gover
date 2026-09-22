package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.process.workers.ProcessWorker;
import de.aivot.prosuna.backend.communication.entities.CommunicationDeliveryEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.models.*;
import de.aivot.prosuna.backend.communication.repositories.CommunicationDeliveryRepository;
import de.aivot.prosuna.backend.process.enums.*;
import de.aivot.prosuna.backend.process.models.CommunicationContinuation;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.prosuna.backend.process.workers.ProcessNodeExecutionResultHandler;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class CommunicationDeliveryMonitor {
    private final CommunicationDeliveryRepository deliveries;
    private final CommunicationProviderDefinitionService definitions;
    private final CommunicationProviderConfigurationService configurations;
    private final ProcessInstanceTaskRepository tasks;
    private final ProcessInstanceRepository instances;
    private final ProcessNodeRepository nodes;
    private final ProcessNodeDefinitionService nodeDefinitions;
    private final ProcessNodeExecutionResultHandler resultHandler;
    private final ProcessNodeExecutionLoggerFactory loggers;
    private final UserService users;
    private final JsonMapper mapper;
    private final ProcessWorker worker;

    public CommunicationDeliveryMonitor(CommunicationDeliveryRepository deliveries,
                                        CommunicationProviderDefinitionService definitions,
                                        CommunicationProviderConfigurationService configurations,
                                        ProcessInstanceTaskRepository tasks, ProcessInstanceRepository instances,
                                        ProcessNodeRepository nodes, ProcessNodeDefinitionService nodeDefinitions,
                                        ProcessNodeExecutionResultHandler resultHandler,
                                        ProcessNodeExecutionLoggerFactory loggers, UserService users, JsonMapper mapper, ProcessWorker worker) {
        this.deliveries = deliveries;
        this.definitions = definitions;
        this.configurations = configurations;
        this.tasks = tasks;
        this.instances = instances;
        this.nodes = nodes;
        this.nodeDefinitions = nodeDefinitions;
        this.resultHandler = resultHandler;
        this.loggers = loggers;
        this.users = users;
        this.mapper = mapper;
        this.worker = worker;
    }

    @Transactional(rollbackFor = Exception.class)
    public void check(@Nonnull UUID id) throws Exception {
        var delivery = deliveries.findByIdForUpdate(id).orElse(null);
        var now = Instant.now();
        if (delivery == null || delivery.getNextCheckAt() == null || delivery.getNextCheckAt().isAfter(now)) return;

        if (delivery.getTaskId() != null) {
            var task = tasks.findByIdForUpdate(delivery.getTaskId()).orElseThrow();
            var instance = instances.findByIdForUpdate(task.getProcessInstanceId()).orElseThrow();
            if (task.getStatus() != ProcessTaskStatus.AwaitingCommunication
                    || instance.getStatus() == ProcessInstanceStatus.Aborted
                    || instance.getStatus() == ProcessInstanceStatus.Completed) {
                delivery.setNextCheckAt(null);
                if (delivery.getStatus() != CommunicationDeliveryStatus.Accepted
                        && delivery.getStatus() != CommunicationDeliveryStatus.Rejected) {
                    delivery.setStatus(CommunicationDeliveryStatus.Cancelled);
                }
                return;
            }
        }

        var before = delivery.getStatus();
        if (before == CommunicationDeliveryStatus.Sending) {
            delivery.setStatus(CommunicationDeliveryStatus.Unknown)
                    .setStatusMessage("Der Versandstatus ist ungeklärt. Bitte prüfen Sie den Versand vor einer Wiederholung.");
        } else if ((before == CommunicationDeliveryStatus.Submitted || before == CommunicationDeliveryStatus.Unknown)
                && !delivery.getReceipt().isEmpty()) {
            try {
                var result = query(delivery);
                delivery.setStatus(result.status()).setReceipt(result.details()).setCheckFailures(0).setStatusMessage(null);
            } catch (Exception e) {
                // Polling failures say nothing about delivery and must never enable another send.
                delivery.setCheckFailures(delivery.getCheckFailures() + 1)
                        .setStatusMessage("Die Zustellbestätigung konnte nicht abgefragt werden. Die Prüfung wird wiederholt.");
            }
        }
        delivery.setUpdated(now);
        delivery.setNextCheckAt(now.plusSeconds(nextDelay(delivery, now)));

        if (delivery.getStatus() != before) {
            log(delivery, delivery.getStatus() == CommunicationDeliveryStatus.Rejected ? ProcessNodeExecutionLogLevel.Error : ProcessNodeExecutionLogLevel.Info,
                    label(delivery.getStatus()), label(delivery.getStatus()));
        }
        if (!delivery.getOverdueNotified() && Duration.between(delivery.getCreated(), now).toHours() >= 24
                && delivery.getStatus() == CommunicationDeliveryStatus.Submitted) {
            delivery.setOverdueNotified(true);
            log(delivery, ProcessNodeExecutionLogLevel.Warn, "Zustellbestätigung steht aus",
                    "Seit mehr als 24 Stunden liegt keine Zustellbestätigung vor. Die Prüfung wird fortgesetzt.");
        }

        if (delivery.getStatus() == CommunicationDeliveryStatus.Unknown && delivery.getReceipt().isEmpty()) {
            delivery.setNextCheckAt(null);
            log(delivery, ProcessNodeExecutionLogLevel.Error, "Versandstatus ungeklärt",
                    "Ohne Submission-ID ist keine automatische Prüfung möglich. Bitte klären Sie den Versand vor einer Wiederholung.");
        }
        if (delivery.getStatus() == CommunicationDeliveryStatus.Accepted) {
            complete(delivery);
        } else if (delivery.getStatus() == CommunicationDeliveryStatus.Rejected || delivery.getStatus() == CommunicationDeliveryStatus.Failed) {
            delivery.setNextCheckAt(null);
            if (delivery.getTaskId() != null) {
                var task = tasks.findByIdForUpdate(delivery.getTaskId()).orElseThrow();
                task.setStatus(ProcessTaskStatus.Failed).setFinished(now).setUpdated(now);
                tasks.save(task);
                var instance = instances.findByIdForUpdate(task.getProcessInstanceId()).orElseThrow();
                instance.setStatus(ProcessInstanceStatus.Failed);
                instances.save(instance);
                if (delivery.getStatus() == CommunicationDeliveryStatus.Failed) {
                    log(delivery, ProcessNodeExecutionLogLevel.Error, "Nachrichtenversand fehlgeschlagen",
                            delivery.getStatusMessage() == null ? "Die Nachricht konnte nicht übergeben werden." : delivery.getStatusMessage());
                }
            }
        }
        deliveries.save(delivery);
    }

    static long nextDelay(CommunicationDeliveryEntity delivery, Instant now) {
        if (delivery.getCheckFailures() > 0) return Math.min(300L, 10L << Math.min(5, delivery.getCheckFailures() - 1));
        return Duration.between(delivery.getCreated(), now).toMinutes() < 5 ? 10 : 60;
    }

    @SuppressWarnings("unchecked")
    private <C> CommunicationSendResult query(CommunicationDeliveryEntity delivery) throws Exception {
        var definition = definitions.retrieveProviderDefinition(delivery.getDefinitionKey(), delivery.getDefinitionVersion()).orElseThrow();
        if (!(definition instanceof DeliveryTrackingCommunicationProvider<?, ?>)) throw new IllegalStateException("Keine Statusabfrage verfügbar.");
        var tracker = (DeliveryTrackingCommunicationProvider<C, ?>) definition;
        var snapshot = new CommunicationProviderEntity();
        snapshot.setConfiguration(delivery.getConfiguration());
        return tracker.checkDelivery(configurations.mapProviderConfiguration(snapshot, tracker), delivery.getReceipt());
    }

    private void complete(CommunicationDeliveryEntity delivery) throws Exception {
        if (delivery.getTaskId() == null || delivery.getContinuationApplied()) {
            delivery.setNextCheckAt(null);
            return;
        }
        var task = tasks.findByIdForUpdate(delivery.getTaskId()).orElseThrow();
        var instance = instances.findByIdForUpdate(task.getProcessInstanceId()).orElseThrow();
        if (instance.getStatus() != ProcessInstanceStatus.Running) return;
        var continuation = mapper.convertValue(delivery.getContinuation(), CommunicationContinuation.class);
        var node = nodes.findById(task.getProcessNodeId()).orElseThrow();
        var provider = nodeDefinitions.getProcessNodeDefinition(node).orElseThrow();
        var previous = continuation.previousTaskId() == null ? null : tasks.findById(continuation.previousTaskId()).orElse(null);
        var user = continuation.userId() == null ? null : users.retrieve(continuation.userId()).orElse(null);
        var logger = loggers.create(instance.getId(), task.getId(), continuation.userId(), null);
        java.util.function.Consumer<ProcessWorker.DoWorkWorkerPayload> nextWork = payload -> {
            var durablePayload = new ProcessWorker.DoWorkWorkerPayload(
                    payload.processInstanceId(), payload.previousTaskId(), payload.previousNodeId(),
                    payload.previousNodePortKey(), payload.nextNodeId(), delivery.getId());
            delivery.setNextWork(mapper.convertValue(durablePayload, Map.class));
        };
        resultHandler.handleConfirmedCommunication(logger, user, provider, node, instance, task, previous,
                continuation.restore(CommunicationDeliveryView.publicReceipt(delivery.getReceipt())), continuation.additionalIdentities(), nextWork);
        if ("payment".equals(continuation.kind())) {
            worker.resumeAfterConfirmedCommunication(logger, instance, task, node, provider, nextWork);
        }
        delivery.setContinuationApplied(true).setNextCheckAt(null);
    }

    private void log(CommunicationDeliveryEntity delivery, ProcessNodeExecutionLogLevel level, String title, String message) {
        if (delivery.getProcessInstanceId() == null) return;
        loggers.create(delivery.getProcessInstanceId(), delivery.getTaskId(), null, null)
                .logf(level, false, true, title, Map.of("deliveryId", delivery.getId(), "sendResult", CommunicationDeliveryView.publicReceipt(delivery.getReceipt())), "%s", message);
    }

    @Nonnull
    public static String label(@Nonnull CommunicationDeliveryStatus status) {
        return switch (status) {
            case Sending -> "Nachricht wird übergeben";
            case Submitted -> "Wartet auf Zustellbestätigung";
            case Accepted -> "Zustellung bestätigt";
            case Rejected -> "Zustellung abgelehnt";
            case Unknown -> "Versandstatus ungeklärt";
            case Failed -> "Nachrichtenversand fehlgeschlagen";
            case Cancelled -> "Zustellprüfung beendet";
        };
    }
}
