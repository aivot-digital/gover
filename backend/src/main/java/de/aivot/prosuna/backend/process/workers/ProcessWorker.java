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
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
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
import de.aivot.prosuna.backend.process.services.ProcessTaskExecutionService;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.utils.RandomUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import de.aivot.prosuna.backend.utils.Tuple3;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;


@Service
public class ProcessWorker {
    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessNodeRepository processDefinitionNodeRepository;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final ProcessNodeExecutionResultHandler processNodeExecutionResultHandler;
    private final ProcessDataService processDataService;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;
    private final ProcessNodeService processNodeService;
    private final ProcessTaskExecutionService execution;

    @Autowired
    public ProcessWorker(ProcessInstanceRepository processInstanceRepository,
                         ProcessNodeRepository processDefinitionNodeRepository,
                         ProcessNodeDefinitionService processNodeProviderService,
                         ProcessInstanceTaskRepository processInstanceTaskRepository,
                         ProcessNodeExecutionResultHandler processNodeExecutionResultHandler,
                         ProcessDataService processDataService,
                         ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory,
                         ProcessNodeService processNodeService, ProcessTaskExecutionService execution) {
        this.processInstanceRepository = processInstanceRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processNodeProviderService = processNodeProviderService;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processNodeExecutionResultHandler = processNodeExecutionResultHandler;
        this.processDataService = processDataService;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
        this.processNodeService = processNodeService;
        this.execution = execution;
    }

    /**
     * Records an execution failure in a new transaction, because the failed execution's own transaction did not
     * commit. A participating transactional service can, for example, mark the shared execution transaction
     * rollback-only after the worker has already recorded its failure, so the commit discards that failure state.
     * Repeating the work automatically would repeat its side effects, so the instance fails and staff decide.
     */
    public void recordFailedExecution(@Nonnull Long instanceId, @Nullable Long taskId,
                                      @Nonnull RuntimeException exception) {
        try {
            execution.inInstance(instanceId, () -> {
                if (taskId != null) {
                    processInstanceTaskRepository.findById(taskId)
                            .filter(task -> !task.getStatus().isTerminal())
                            .ifPresent(task -> processInstanceTaskRepository.save(task
                                    .setStatus(ProcessTaskStatus.Failed)
                                    .setFinished(Instant.now())));
                }
                var instance = processInstanceRepository.findById(instanceId).orElse(null);
                if (instance != null
                        && instance.getStatus() != ProcessInstanceStatus.Completed
                        && instance.getStatus() != ProcessInstanceStatus.Aborted) {
                    processInstanceRepository.save(instance.setStatus(ProcessInstanceStatus.Failed));
                }
                processNodeExecutionLoggerFactory.create(instanceId, taskId, null, null).logException(exception);
                return null;
            });
        } catch (ResponseException ignored) {
            // The instance no longer exists, so no failure state remains to be recorded.
        }
    }

    /**
     * Creates and runs the task for the payload's next node. The caller must hold the instance lock and has checked
     * that the work is still due, e.g. that the predecessor has no successor yet.
     */
    public void workOnNodeLocked(@Nonnull DoWorkWorkerPayload payload) {
        ProcessNodeExecutionLogger logger = processNodeExecutionLoggerFactory
                .create(payload.processInstanceId(), null, null, null);

        Optional<Tuple3<ProcessInstanceEntity, ProcessNodeEntity, ProcessNodeDefinition<?>>> instanceNodeProvider;
        try {
            instanceNodeProvider = fetchInstanceNodeProvider(payload.processInstanceId, payload.nextNodeId);
        } catch (Exception exception) {
            logger.logException(exception);
            return;
        }
        if (instanceNodeProvider.isEmpty()) {
            return;
        }
        var resolved = instanceNodeProvider.get();
        var currentProcessInstance = resolved.first();
        var nextProcessNode = resolved.second();
        var nextProcessNodeDefinition = resolved.third();

        try {
            workOnNextProcessNode(
                    nextProcessNode,
                    nextProcessNodeDefinition,
                    logger,
                    currentProcessInstance,
                    payload.previousTaskId,
                    payload.previousNodeId,
                    payload.previousNodePortKey,
                    payload.restartForTaskId
            );
        } catch (Exception exception) {
            logger.logException(exception);
            currentProcessInstance.setStatus(ProcessInstanceStatus.Failed);
            processInstanceRepository.save(currentProcessInstance);
        }
    }

    /** Resumes the waiting task. The caller must hold the instance lock and has checked that the task can run. */
    public void resumeLocked(@Nonnull Long instanceId, @Nonnull Long taskId) {
        var task = processInstanceTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        resumeWorkOnCurrentNodeLocked(new ResumeWorkWorkerPayload(instanceId, taskId, task.getProcessNodeId()));
    }

    /**
     * An update for a task that its hard deadline has already ended is not processed. Record it in the history, so
     * staff can see that a late notification, such as a payment, has arrived and decide on further steps.
     */
    void recordResumeAfterDeadline(@Nonnull Long instanceId, @Nonnull Long taskId) {
        var task = processInstanceTaskRepository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != ProcessTaskStatus.DeadlineExceeded) {
            return;
        }
        var details = new LinkedHashMap<String, Object>();
        details.put("deadline", task.getDeadline());
        details.put("deadlineKey", task.getDeadlineKey());
        processNodeExecutionLoggerFactory.create(instanceId, taskId, null, null).logf(
                ProcessNodeExecutionLogLevel.Warn,
                false,
                true,
                true,
                task.getAssignedUserId(),
                task.getAssignedCustomerIdentityId(),
                null,
                "Rückmeldung nach Fristablauf",
                details,
                "Nach Ablauf der Frist (%s) ist eine Rückmeldung zu dieser Aufgabe eingegangen, beispielsweise zu einer Zahlung. " +
                        "Sie wurde nicht mehr verarbeitet, weil die Aufgabe bereits wegen Fristüberschreitung beendet war. " +
                        "Prüfen Sie, ob weitere Schritte erforderlich sind.",
                ExecutionSummaryMarkdown.timestamp(task.getDeadline())
        );
    }

    private void resumeWorkOnCurrentNodeLocked(ResumeWorkWorkerPayload payload) {
        var logger = processNodeExecutionLoggerFactory
                .create(payload.currentProcessInstanceId(), payload.currentTaskId, null, null);


        Optional<Tuple3<ProcessInstanceEntity, ProcessNodeEntity, ProcessNodeDefinition<?>>> instanceNodeProvider;
        try {
            instanceNodeProvider = fetchInstanceNodeProvider(payload.currentProcessInstanceId, payload.currentNodeId);
        } catch (Exception exception) {
            logger.logException(exception);
            return;
        }
        if (instanceNodeProvider.isEmpty()) {
            return;
        }
        var resolved = instanceNodeProvider.get();
        var currentProcessInstance = resolved.first();
        var currentProcessNode = resolved.second();
        var currentProcessNodeDefinition = resolved.third();

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

    private Optional<Tuple3<ProcessInstanceEntity, ProcessNodeEntity, ProcessNodeDefinition<?>>> fetchInstanceNodeProvider(@Nonnull Long processInstanceId, @Nonnull Integer nodeId) throws Exception {
        // Fetch the process instance
        // If this fails, we cannot continue
        ProcessInstanceEntity instance = processInstanceRepository
                .findById(processInstanceId)
                .orElseThrow(() -> new RuntimeException(
                        "Der Vorgang mit der ID „%d“ wurde nicht gefunden."
                                .formatted(processInstanceId)
                ));

        if (instance.getStatus() == ProcessInstanceStatus.Completed
                || instance.getStatus() == ProcessInstanceStatus.Aborted
                || instance.getStatus() == ProcessInstanceStatus.Paused) {
            // Ignore queued work that arrived after the process reached a terminal state.
            return Optional.empty();
        }

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
                            "Der Prozesselementanbieter mit dem Schlüssel „%s“ und der Version „%d“ wurde nicht gefunden.",
                            node.getProcessNodeDefinitionKey(),
                            node.getProcessNodeDefinitionVersion()
                    ));
        } catch (Exception exception) {
            instance.setStatus(ProcessInstanceStatus.Failed);
            processInstanceRepository.save(instance);
            throw exception;
        }

        return Optional.of(new Tuple3<>(
                instance,
                node,
                provider
        ));
    }

    private <NodeConfig> void workOnNextProcessNode(@Nonnull ProcessNodeEntity currentNode,
                                                    @Nonnull ProcessNodeDefinition<NodeConfig> currentNodeProvider,
                                                    @Nonnull ProcessNodeExecutionLogger logger,
                                                    @Nonnull ProcessInstanceEntity processInstance,
                                                    @Nullable Long previousTaskId,
                                                    @Nullable Integer previousNodeId,
                                                    @Nullable String previousNodePortKey,
                                                    @Nullable Long restartForTaskId) throws ProcessNodeExecutionException {
        if (restartForTaskId != null) {
            var previousAttempt = processInstanceTaskRepository.findById(restartForTaskId)
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionUnknown(
                            "Die Aufgabe mit der ID „%d“ wurde nicht gefunden.", restartForTaskId));
            // The status of the previous attempt is checked by the caller; only its reference must match here.
            if (!processInstance.getId().equals(previousAttempt.getProcessInstanceId())
                    || !currentNode.getId().equals(previousAttempt.getProcessNodeId())) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        "Die Aufgabe mit der ID „%d“ gehört nicht zu diesem Vorgang und Prozesselement.", restartForTaskId);
            }
        }

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
                        null,
                        null,
                        null,
                        null,
                        null
                ).setRestartForTaskId(restartForTaskId)
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

        ProcessNodeExecutionResult initResult;
        ProcessNodeExecutionInitContext<NodeConfig> context;
        try {
            context = getRuntimeContext(logger, processInstance, taskEntity, currentNode, currentNodeProvider);
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
                    "Der Prozesselementanbieter „%s“ für das Prozesselement „%s“ konnte die Aufgabe nicht initialisieren.",
                    currentNodeProvider.getName(),
                    currentNode.resolveName(currentNodeProvider)
            );
            logger.logException(ex);
            throw ex;
        }

        handleResult(currentNode, currentNodeProvider, context.getConfigurationOfExecutingNode(), logger, processInstance, previousTaskId, previousNodeId, initResult, taskEntity);
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

        ProcessNodeExecutionResult resumeResult;
        ProcessNodeExecutionInitContext<NodeConfig> context;
        try {
            context = getRuntimeContext(logger, processInstance, taskEntity, currentNode, currentNodeProvider);
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
                    "Der Prozesselementanbieter „%s“ für das Prozesselement „%s“ konnte die Aufgabe nicht initialisieren.",
                    currentNodeProvider.getName(),
                    currentNode.resolveName(currentNodeProvider)
            );
            logger.logException(ex);
            throw ex;
        }

        handleResult(currentNode, currentNodeProvider, context.getConfigurationOfExecutingNode(), logger, processInstance, taskEntity.getPreviousProcessInstanceTaskId(), taskEntity.getPreviousProcessNodeId(), resumeResult, taskEntity);
    }

    @Nonnull
    private <NodeConfig> ProcessNodeExecutionInitContext<NodeConfig> getRuntimeContext(@Nonnull ProcessNodeExecutionLogger logger,
                                                                                       @Nonnull ProcessInstanceEntity processInstance,
                                                                                       @Nonnull ProcessInstanceTaskEntity taskEntity,
                                                                                       @Nonnull ProcessNodeEntity currentNode,
                                                                                       @Nonnull ProcessNodeDefinition<NodeConfig> currentNodeProvider) throws ProcessNodeExecutionException {
        var processData = processDataService
                .foldProcessInstanceData(
                        processInstance,
                        taskEntity.getPreviousProcessNodeId(),
                        taskEntity
                );

        ProcessNodeService.ProcessConfigurationDetails<NodeConfig> configuration;
        try {
            configuration = processNodeService
                    .deriveRuntimeConfiguration(currentNode, currentNodeProvider, null, false, processData);
        } catch (ResponseException e) {
            var ex = new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Konfiguration des Prozesselements %s konnte nicht abgeleitet werden.",
                    StringUtils.quote(currentNode.resolveName(currentNodeProvider))
            );
            logger.logException(ex);
            throw ex;
        }

        if (configuration.derivedRuntimeElementData().hasAnyError()) {
            var ex = new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die dynamische Konfiguration des Prozesselements %s konnte nicht aufgelöst werden.",
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
                configuration.configuration(),
                configuration.derivedRuntimeElementData().getEffectiveValues()
        );
    }

    private <NodeConfig> void handleResult(@Nonnull ProcessNodeEntity currentNode,
                                           @Nonnull ProcessNodeDefinition<NodeConfig> currentNodeProvider,
                                           @Nonnull NodeConfig configurationOfExecutingNode,
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
                    "Der Prozesselementanbieter %s für das Prozesselement %s lieferte kein Ergebnis zurück.",
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
                            configurationOfExecutingNode,
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
            @Nonnull Integer nextNodeId,
            @Nullable Long restartForTaskId
    ) {

    }

    public record ResumeWorkWorkerPayload(
            @Nonnull Long currentProcessInstanceId,
            @Nonnull Long currentTaskId,
            @Nonnull Integer currentNodeId
    ) {

    }
}
