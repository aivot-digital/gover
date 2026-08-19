package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.services.ProcessDataService;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import de.aivot.gover.backend.utils.RandomUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import de.aivot.gover.backend.utils.Tuple3;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.UUID;


@Service
public class ProcessWorker {
    public static final String DO_WORK_ON_INSTANCE_QUEUE = "do-work-on-instance-queue";
    public static final String RESUME_WORK_ON_INSTANCE_QUEUE = "resume-work-on-instance-queue";

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

    @Bean
    public Queue resumeWorkOnInstanceQueue() {
        return new Queue(RESUME_WORK_ON_INSTANCE_QUEUE, true);
    }

    @RabbitListener(queues = DO_WORK_ON_INSTANCE_QUEUE)
    public void doWorkOnNextNode(DoWorkWorkerPayload payload) {
        ProcessNodeExecutionLogger logger = processNodeExecutionLoggerFactory
                .create(payload.processInstanceId(), null, null, null);

        Tuple3<ProcessInstanceEntity, ProcessNodeEntity, ProcessNodeDefinition<?>> instanceNodeProvider;
        try {
            instanceNodeProvider = fetchInstanceNodeProvider(payload.processInstanceId, payload.nextNodeId);
        } catch (Exception exception) {
            logger.logException(exception);
            return;
        }
        var currentProcessInstance = instanceNodeProvider.first();
        var nextProcessNode = instanceNodeProvider.second();
        var nextProcessNodeDefinition = instanceNodeProvider.third();

        try {
            workOnNextProcessNode(
                    nextProcessNode,
                    nextProcessNodeDefinition,
                    logger,
                    currentProcessInstance,
                    payload.previousTaskId,
                    payload.previousNodeId,
                    payload.previousNodePortKey
            );
        } catch (Exception exception) {
            logger.logException(exception);
            currentProcessInstance.setStatus(ProcessInstanceStatus.Failed);
            processInstanceRepository.save(currentProcessInstance);
        }
    }

    @RabbitListener(queues = RESUME_WORK_ON_INSTANCE_QUEUE)
    public void resumeWorkOnCurrentNode(ResumeWorkWorkerPayload payload) {
        var logger = processNodeExecutionLoggerFactory
                .create(payload.currentProcessInstanceId(), payload.currentTaskId, null, null);


        Tuple3<ProcessInstanceEntity, ProcessNodeEntity, ProcessNodeDefinition<?>> instanceNodeProvider;
        try {
            instanceNodeProvider = fetchInstanceNodeProvider(payload.currentProcessInstanceId, payload.currentNodeId);
        } catch (Exception exception) {
            logger.logException(exception);
            return;
        }
        var currentProcessInstance = instanceNodeProvider.first();
        var currentProcessNode = instanceNodeProvider.second();
        var currentProcessNodeDefinition = instanceNodeProvider.third();

        ProcessInstanceTaskEntity currentTask;
        try {
            currentTask = processInstanceTaskRepository
                    .findById(payload.currentTaskId)
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionUnknown(
                            "Die Aufgabe mit der ID „%d“ wurde nicht gefunden.",
                            payload.currentTaskId
                    ));
        } catch (Exception exception) {
            logger.logException(exception);
            currentProcessInstance.setStatus(ProcessInstanceStatus.Failed);
            processInstanceRepository.save(currentProcessInstance);
            return;
        }

        try {
            resumeWorkOnCurrentProcessTask(
                    logger,
                    currentProcessInstance,
                    currentTask,
                    currentProcessNode,
                    currentProcessNodeDefinition
            );
        } catch (Exception exception) {
            logger.logException(exception);
            currentProcessInstance.setStatus(ProcessInstanceStatus.Failed);
            processInstanceRepository.save(currentProcessInstance);
        }

    }

    private Tuple3<ProcessInstanceEntity, ProcessNodeEntity, ProcessNodeDefinition<?>> fetchInstanceNodeProvider(@Nonnull Long processInstanceId, @Nonnull Integer nodeId) throws Exception {
        // Fetch the process instance
        // If this fails, we cannot continue
        ProcessInstanceEntity instance = processInstanceRepository
                .findById(processInstanceId)
                .orElseThrow(() -> new RuntimeException(
                        "Der Vorgang mit der ID „%d“ wurde nicht gefunden."
                                .formatted(processInstanceId)
                ));

        ProcessNodeEntity node;
        try {
            // Fetch the current node
            node = processDefinitionNodeRepository
                    .findById(nodeId)
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionUnknown(
                            "Das Prozesselement mit der ID „%d“ wurde nicht gefunden.",
                            nodeId
                    ));
        } catch (Exception exception) {
            instance.setStatus(ProcessInstanceStatus.Failed);
            processInstanceRepository.save(instance);
            throw exception;
        }

        ProcessNodeDefinition<?> provider;
        try {
            provider = processNodeProviderService
                    .getProcessNodeDefinition(node)
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionUnknown(
                            "Der Prozessknoten-Funktionsanbieter mit dem Schlüssel „%s“ und der Version „%d“ wurde nicht gefunden.",
                            node.getProcessNodeDefinitionKey(),
                            node.getProcessNodeDefinitionVersion()
                    ));
        } catch (Exception exception) {
            instance.setStatus(ProcessInstanceStatus.Failed);
            processInstanceRepository.save(instance);
            throw exception;
        }

        return new Tuple3<>(
                instance,
                node,
                provider
        );
    }

    private <NodeConfig> void workOnNextProcessNode(@Nonnull ProcessNodeEntity currentNode,
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
                        RandomUtils.generateRandomString(ProcessInstanceTaskEntity.ACCESS_KEY_LENGTH),
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

        var context = getRuntimeContext(logger, processInstance, taskEntity, currentNode, currentNodeProvider);

        ProcessNodeExecutionResult initResult;
        try {
            initResult = currentNodeProvider.init(context);
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

        handleResult(currentNode, currentNodeProvider, logger, processInstance, previousTaskId, previousNodeId, initResult, taskEntity);
    }

    private <NodeConfig> void resumeWorkOnCurrentProcessTask(
            ProcessNodeExecutionLogger logger,
            ProcessInstanceEntity processInstance,
            ProcessInstanceTaskEntity taskEntity,
            ProcessNodeEntity currentNode,
            ProcessNodeDefinition<NodeConfig> currentNodeProvider
    ) throws ProcessNodeExecutionException {
        logger.logf(
                ProcessNodeExecutionLogLevel.Info,
                true,
                false,
                "Aufgabe " + StringUtils.quote(currentNode.resolveName(currentNodeProvider)) + " wieder aufgenommen",
                "Die Aufgabe für das Prozesselement „%s“ (ID: %d) wurde wieder aufgenommen. Die Aufgabe wird vom System weiter bearbeitet und gegebenenfalls an eine Mitarbeiter:in zugewiesen.",
                currentNode.resolveName(currentNodeProvider),
                currentNode.getId()
        );

        var context = getRuntimeContext(logger, processInstance, taskEntity, currentNode, currentNodeProvider);

        ProcessNodeExecutionResult resumeResult;
        try {
            resumeResult = currentNodeProvider.resume(context);
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

        handleResult(currentNode, currentNodeProvider, logger, processInstance, taskEntity.getPreviousProcessInstanceTaskId(), taskEntity.getPreviousProcessNodeId(), resumeResult, taskEntity);
    }

    @Nonnull
    private <NodeConfig> ProcessNodeExecutionInitContext<NodeConfig> getRuntimeContext(@Nonnull ProcessNodeExecutionLogger logger,
                                                                                       @Nonnull ProcessInstanceEntity processInstance,
                                                                                       @Nonnull ProcessInstanceTaskEntity taskEntity,
                                                                                       @Nonnull ProcessNodeEntity currentNode,
                                                                                       @Nonnull ProcessNodeDefinition<NodeConfig> currentNodeProvider) throws ProcessNodeExecutionExceptionUnknown {
        var processData = processDataService
                .foldProcessInstanceData(
                        processInstance,
                        taskEntity.getPreviousProcessNodeId(),
                        taskEntity
                );

        ProcessNodeService.ProcessConfigurationDetails<NodeConfig> configuration;
        try {
            configuration = processNodeService
                    .deriveConfiguration(currentNode, currentNodeProvider, null, false);
        } catch (ResponseException e) {
            var ex = new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Konfiguration des Prozessknotens %s konnte nicht abgeleitet werden.",
                    StringUtils.quote(currentNode.resolveName(currentNodeProvider))
            );
            logger.logException(ex);
            throw ex;
        }

        return new ProcessNodeExecutionInitContext<>(
                logger,
                currentNode,
                processInstance,
                taskEntity,
                null,
                processData,
                configuration.configuration()
        );
    }

    private <NodeConfig> void handleResult(@Nonnull ProcessNodeEntity currentNode,
                                           @Nonnull ProcessNodeDefinition<NodeConfig> currentNodeProvider,
                                           @Nonnull ProcessNodeExecutionLogger logger,
                                           @Nonnull ProcessInstanceEntity processInstance,
                                           @Nullable Long previousTaskId,
                                           @Nullable Integer previousNodeId,
                                           @Nullable ProcessNodeExecutionResult result,
                                           @Nonnull ProcessInstanceTaskEntity taskEntity) throws ProcessNodeExecutionException {
        if (result == null) {
            taskEntity.setStatus(ProcessTaskStatus.Failed);
            taskEntity.setFinished(Instant.now());
            processInstanceTaskRepository.save(taskEntity);
            var ex = new ProcessNodeExecutionExceptionUnknown(
                    "Der Prozessknoten-Funktionsanbieter %s für das Prozesselement %s lieferte kein Ergebnis zurück.",
                    StringUtils.quote(currentNodeProvider.getName()),
                    StringUtils.quote(currentNode.resolveName(currentNodeProvider))
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
                            result
                    );
        } catch (Exception e) {
            taskEntity.setStatus(ProcessTaskStatus.Failed);
            taskEntity.setFinished(Instant.now());
            processInstanceTaskRepository.save(taskEntity);
            logger.logException(e);
            throw e;
        }
    }

    public record DoWorkWorkerPayload(
            @Nonnull Long processInstanceId,
            @Nullable Long previousTaskId,
            @Nullable Integer previousNodeId,
            @Nullable String previousNodePortKey,
            @Nonnull Integer nextNodeId
    ) implements Serializable {

    }

    public record ResumeWorkWorkerPayload(
            @Nonnull Long currentProcessInstanceId,
            @Nonnull Long currentTaskId,
            @Nonnull Integer currentNodeId
    ) implements Serializable {

    }
}
