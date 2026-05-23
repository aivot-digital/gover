package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.process.services.ProcessNodeDefinitionService;
import de.aivot.GoverBackend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.GoverBackend.process.services.ProcessNodeService;
import de.aivot.GoverBackend.utils.ApplicationTimeZone;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.UUID;


@Service
public class ProcessWorker {
    public static final String DO_WORK_ON_INSTANCE_QUEUE = "do-work-on-instance-queue";

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessNodeRepository processDefinitionNodeRepository;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final ProcessNodeExecutionResultHandler processNodeExecutionResultHandler;
    private final ProcessDataService processDataService;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;
    private final ProcessNodeService processNodeService;

    @Autowired
    public ProcessWorker(ProcessInstanceRepository processInstanceRepository,
                         ProcessNodeRepository processDefinitionNodeRepository,
                         ProcessNodeDefinitionService processNodeProviderService,
                         ProcessInstanceTaskRepository processInstanceTaskRepository,
                         ProcessNodeExecutionResultHandler processNodeExecutionResultHandler,
                         ProcessDataService processDataService,
                         ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory,
                         ProcessNodeService processNodeService) {
        this.processInstanceRepository = processInstanceRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processNodeProviderService = processNodeProviderService;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processNodeExecutionResultHandler = processNodeExecutionResultHandler;
        this.processDataService = processDataService;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
        this.processNodeService = processNodeService;
    }

    @Bean
    public Queue doWorkOnInstanceQueue() {
        return new Queue(DO_WORK_ON_INSTANCE_QUEUE, true);
    }

    @RabbitListener(queues = DO_WORK_ON_INSTANCE_QUEUE)
    public void listen(WorkerPayload payload) {
        var logger = processNodeExecutionLoggerFactory
                .create(payload.processInstanceId(), null, null, null);

        ProcessInstanceEntity processInstance;
        try {
            // Fetch the process instance
            // If this fails, we cannot continue
            processInstance = processInstanceRepository
                    .findById(payload.processInstanceId)
                    .orElseThrow(() -> new RuntimeException(
                            "Der Vorgang mit der ID „%d“ wurde nicht gefunden."
                                    .formatted(payload.processInstanceId)
                    ));
        } catch (Exception exception) {
            logger.logException(exception);
            return;
        }

        try {
            process(
                    logger,
                    processInstance,
                    payload.previousTaskId(),
                    payload.previousNodeId(),
                    payload.previousNodePortKey(),
                    payload.nextNodeId()
            );
        } catch (ProcessNodeExecutionException exception) {
            logger.logException(exception);
            processInstance.setStatus(ProcessInstanceStatus.Failed);
            processInstanceRepository.save(processInstance);
        } catch (Exception exception) {
            logger.logException(exception);

            processInstance.setStatus(ProcessInstanceStatus.Failed);
            processInstanceRepository.save(processInstance);
        }
    }

    private void process(@Nonnull ProcessNodeExecutionLogger logger,
                         @Nonnull ProcessInstanceEntity processInstance,
                         @Nullable Long previousTaskId,
                         @Nullable Integer previousNodeId,
                         @Nullable String previousNodePortKey,
                         @Nonnull Integer nodeId) throws ProcessNodeExecutionException {

        // Fetch the current node
        var currentNode = processDefinitionNodeRepository
                .findById(nodeId)
                .orElseThrow(() -> new ProcessNodeExecutionExceptionUnknown(
                        "Das Prozesselement mit der ID „%d“ wurde nicht gefunden.",
                        nodeId
                ));

        // Fetch the current node provider
        var currentNodeProvider = processNodeProviderService
                .getProcessNodeDefinition(currentNode)
                .orElseThrow(() -> new ProcessNodeExecutionExceptionUnknown(
                        "Der Prozessknoten-Funktionsanbieter mit dem Schlüssel „%s“ und der Version „%d“ wurde nicht gefunden.",
                        currentNode.getProcessNodeDefinitionKey(),
                        currentNode.getProcessNodeDefinitionVersion()
                ));

        dodo(
                currentNode,
                currentNodeProvider,
                logger,
                processInstance,
                previousTaskId,
                previousNodeId,
                previousNodePortKey
        );
    }

    private <NodeConfig> void dodo(@Nonnull ProcessNodeEntity currentNode,
                                   @Nonnull ProcessNodeDefinition<NodeConfig> currentNodeProvider,
                                   @Nonnull ProcessNodeExecutionLogger logger,
                                   @Nonnull ProcessInstanceEntity processInstance,
                                   @Nullable Long previousTaskId,
                                   @Nullable Integer previousNodeId,
                                   @Nullable String previousNodePortKey) throws ProcessNodeExecutionException {
        var deadline = currentNode.getTimeLimitDays() != null ?
                // Preserve the local same-wall-clock-time behavior when task deadlines cross DST changes.
                ZonedDateTime.now(ApplicationTimeZone.getZoneId()).plusDays(currentNode.getTimeLimitDays()).toInstant() :
                null;
        var startedAt = Instant.now();

        var taskEntity = processInstanceTaskRepository.save(
                new ProcessInstanceTaskEntity(
                        null,
                        UUID.randomUUID(),
                        processInstance.getId(),
                        processInstance.getProcessId(),
                        currentNode.getProcessVersion(),
                        currentNode.getId(),
                        previousTaskId,
                        previousNodeId,
                        previousNodePortKey,
                        ProcessTaskStatus.Running,
                        null,
                        startedAt,
                        startedAt,
                        null,
                        null,
                        new HashMap<>(),
                        new HashMap<>(),
                        new HashMap<>(),
                        new HashMap<>(),
                        null,
                        deadline,
                        null,
                        null,
                        null
                )
        );

        logger = logger
                .withTaskId(taskEntity.getId());

        logger.logf(
                ProcessNodeExecutionLogLevel.Info,
                true,
                false,
                "Aufgabe " + StringUtils.quote(currentNode.resolveName(currentNodeProvider)) + " gestartet",
                "Die Aufgabe für das Prozesselement „%s“ (ID: %d) wurde gestartet. Die Aufgabe wird nun vom System verarbeitet und gegebenenfalls an eine Mitarbeiter:in zugewiesen.",
                currentNode.resolveName(currentNodeProvider),
                currentNode.getId()
        );

        var processData = processDataService
                .foldProcessInstanceData(
                        processInstance,
                        previousNodeId
                );

        ProcessNodeService.ProcessConfigurationDetails<NodeConfig> configuration;
        try {
            configuration = processNodeService
                    .deriveConfiguration(currentNode, currentNodeProvider,  null,false);
        } catch (ResponseException e) {
            var ex = new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Konfiguration des Prozessknotens „%s“ konnte nicht abgeleitet werden.",
                    currentNode.resolveName(currentNodeProvider)
            );
            logger.logException(ex);
            throw ex;
        }

        var context = new ProcessNodeExecutionInitContext<NodeConfig>(
                logger,
                currentNode,
                processInstance,
                taskEntity,
                null,
                processData,
                configuration.configuration()
        );

        ProcessNodeExecutionResult initResult;
        try {
            initResult = currentNodeProvider
                    .init(context);
        } catch (ProcessNodeExecutionException e) {
            taskEntity.setStatus(ProcessTaskStatus.Failed);
            taskEntity.setFinished(Instant.now());
            processInstanceTaskRepository.save(taskEntity);
            logger.logException(e);
            throw e;
        } catch (Exception e) {
            taskEntity.setStatus(ProcessTaskStatus.Failed);
            taskEntity.setFinished(Instant.now());
            processInstanceTaskRepository.save(taskEntity);
            var ex = new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Der Prozessknoten-Funktionsanbieter „%s“ für das Prozesselement „%s“ konnte die Aufgabe nicht initialisieren.",
                    currentNodeProvider.getName(),
                    currentNode.resolveName(currentNodeProvider)
            );
            logger.logException(ex);
            throw ex;
        }

        if (initResult == null) {
            taskEntity.setStatus(ProcessTaskStatus.Failed);
            taskEntity.setFinished(Instant.now());
            processInstanceTaskRepository.save(taskEntity);
            var ex = new ProcessNodeExecutionExceptionUnknown(
                    "Der Prozessknoten-Funktionsanbieter „%s“ für das Prozesselement „%s“ lieferte kein Ergebnis zurück.",
                    currentNodeProvider.getName(),
                    currentNode.resolveName(currentNodeProvider)
            );
            logger.logException(ex);
            throw ex;
        }

        ProcessInstanceTaskEntity previousTask;
        if (previousTaskId != null) {
            previousTask = processInstanceTaskRepository
                    .findById(previousTaskId)
                    .orElse(null);
        } else if (previousNodeId != null) {
            previousTask = processInstanceTaskRepository
                    .findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc(
                            processInstance.getId(),
                            previousNodeId
                    )
                    .orElse(null);
        } else {
            previousTask = null;
        }

        try {
            processNodeExecutionResultHandler
                    .handleResult(
                            logger,
                            null,
                            currentNodeProvider,
                            currentNode,
                            processInstance,
                            taskEntity,
                            previousTask,
                            initResult
                    );
        } catch (Exception e) {
            taskEntity.setStatus(ProcessTaskStatus.Failed);
            taskEntity.setFinished(Instant.now());
            processInstanceTaskRepository.save(taskEntity);
            logger.logException(e);
            throw e;
        }
    }

    public record WorkerPayload(
            @Nonnull Long processInstanceId,
            @Nullable Long previousTaskId,
            @Nullable Integer previousNodeId,
            @Nullable String previousNodePortKey,
            @Nonnull Integer nextNodeId
    ) implements Serializable {

    }
}
