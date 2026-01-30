package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextInit;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.process.services.ProcessNodeDefinitionService;
import de.aivot.GoverBackend.process.services.ProcessNodeExecutionLoggerFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;
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

    @Autowired
    public ProcessWorker(ProcessInstanceRepository processInstanceRepository,
                         ProcessNodeRepository processDefinitionNodeRepository,
                         ProcessNodeDefinitionService processNodeProviderService,
                         ProcessInstanceTaskRepository processInstanceTaskRepository,
                         ProcessNodeExecutionResultHandler processNodeExecutionResultHandler,
                         ProcessDataService processDataService,
                         ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory) {
        this.processInstanceRepository = processInstanceRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processNodeProviderService = processNodeProviderService;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processNodeExecutionResultHandler = processNodeExecutionResultHandler;
        this.processDataService = processDataService;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
    }

    @Bean
    public Queue doWorkOnInstanceQueue() {
        return new Queue(DO_WORK_ON_INSTANCE_QUEUE, true);
    }

    @RabbitListener(queues = DO_WORK_ON_INSTANCE_QUEUE)
    public void listen(WorkerPayload payload) {
        // Fetch the process instance
        // If this fails, we cannot continue
        var processInstance = processInstanceRepository
                .findById(payload.processInstanceId)
                .orElseThrow(() -> new RuntimeException(
                        "Der Vorgang mit der ID „%d“ wurde nicht gefunden."
                                .formatted(payload.processInstanceId)
                ));

        var logger = processNodeExecutionLoggerFactory
                .create(processInstance.getId(), null, null, null);

        try {
            process(
                    logger,
                    processInstance,
                    payload.previousNodeId(),
                    payload.nextNodeId()
            );
        } catch (ProcessNodeExecutionException exception) {
            logger.logException(exception);
        } catch (Exception exception) {
            logger.logException(exception);
        }
    }

    private void process(@Nonnull ProcessNodeExecutionLogger logger,
                         @Nonnull ProcessInstanceEntity processInstance,
                         @Nullable Integer previousNodeId,
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

        var deadline = currentNode.getTimeLimitDays() != null ?
                LocalDateTime.now().plusDays(currentNode.getTimeLimitDays()) :
                null;

        var taskEntity = processInstanceTaskRepository.save(
                new ProcessInstanceTaskEntity(
                        null,
                        UUID.randomUUID(),
                        processInstance.getId(),
                        processInstance.getProcessId(),
                        currentNode.getProcessVersion(),
                        currentNode.getId(),
                        previousNodeId,
                        ProcessTaskStatus.Running,
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        null,
                        null,
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
                ProcessNodeExecutionLogLevel.Debug,
                true,
                false,
                "Aufgabe für das Prozesselement „%s“ (ID: %d) wurde gestartet.",
                currentNode.resolveName(currentNodeProvider),
                currentNode.getId()
        );

        var processData = processDataService
                .foldProcessInstanceData(
                        processInstance,
                        previousNodeId
                );

        var context = new ProcessNodeExecutionContextInit(
                logger,
                currentNode,
                processInstance,
                taskEntity,
                null,
                processData
        );

        var res = currentNodeProvider
                .init(context);

        if (res == null) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Der Prozessknoten-Funktionsanbieter „%s“ für das Prozesselement „%s“ lieferte kein Ergebnis zurück.",
                    currentNodeProvider.getName(),
                    currentNode.resolveName(currentNodeProvider)
            );
        }

        ProcessInstanceTaskEntity previousTask;
        if (previousNodeId != null) {
            previousTask = processInstanceTaskRepository
                    .findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc(
                            processInstance.getId(),
                            previousNodeId
                    );
        } else {
            previousTask = null;
        }

        processNodeExecutionResultHandler
                .handleResult(
                        logger,
                        null,
                        currentNodeProvider,
                        currentNode,
                        processInstance,
                        taskEntity,
                        previousTask,
                        res
                );
    }

    public record WorkerPayload(
            @Nonnull Long processInstanceId,
            @Nullable Integer previousNodeId,
            @Nonnull Integer nextNodeId
    ) implements Serializable {

    }
}
