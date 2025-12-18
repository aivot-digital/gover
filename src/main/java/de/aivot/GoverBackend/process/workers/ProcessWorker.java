package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultError;
import de.aivot.GoverBackend.process.repositories.ProcessDefinitionNodeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.process.services.ProcessNodeProviderService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(ProcessWorker.class);

    public static final String DO_WORK_ON_INSTANCE_QUEUE = "do-work-on-instance-queue";

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessDefinitionNodeRepository processDefinitionNodeRepository;
    private final ProcessNodeProviderService processNodeProviderService;
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final ProcessNodeExecutionResultHandler processNodeExecutionResultHandler;
    private final ProcessDataService processDataService;

    @Autowired
    public ProcessWorker(ProcessInstanceRepository processInstanceRepository,
                         ProcessDefinitionNodeRepository processDefinitionNodeRepository,
                         ProcessNodeProviderService processNodeProviderService,
                         ProcessInstanceTaskRepository processInstanceTaskRepository,
                         ProcessNodeExecutionResultHandler processNodeExecutionResultHandler, ProcessDataService processDataService) {
        this.processInstanceRepository = processInstanceRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processNodeProviderService = processNodeProviderService;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processNodeExecutionResultHandler = processNodeExecutionResultHandler;
        this.processDataService = processDataService;
    }

    @Bean
    public Queue doWorkOnInstanceQueue() {
        return new Queue(DO_WORK_ON_INSTANCE_QUEUE, true);
    }

    @RabbitListener(queues = DO_WORK_ON_INSTANCE_QUEUE)
    public void listen(WorkerPayload payload) {
        try {
            process(
                    payload.processInstanceId(),
                    payload.previousNodeId(),
                    payload.nextNodeId()
            );
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("Error processing payload for Process Instance ID: {}")
                    .addArgument(payload.processInstanceId())
                    .setCause(e)
                    .log();
        }
    }

    private void process(@Nonnull Long processInstanceId, @Nullable Integer previousNodeId, @Nonnull Integer nodeId) {
        var processInstance = processInstanceRepository
                .findById(processInstanceId)
                .orElseThrow(() -> new RuntimeException("Process Instance not found"));

        var currentNode = processDefinitionNodeRepository
                .findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Process Definition Node not found"));

        var currentNodeProvider = processNodeProviderService
                .getProcessNodeProvider(currentNode.getCodeKey())
                .orElseThrow(() -> new RuntimeException("Process Node Provider not found"));

        var taskEntity = processInstanceTaskRepository.save(
                new ProcessInstanceTaskEntity(
                        null,
                        UUID.randomUUID(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionId(),
                        processInstance.getProcessDefinitionVersion(),
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
                        null
                )
        );

        try {
            var processData = processDataService
                    .foldProcessInstanceData(
                            processInstance,
                            previousNodeId
                    );

            ProcessNodeExecutionResult res;
            try {
                res = currentNodeProvider
                        .init(
                                processInstance,
                                currentNode,
                                processData
                        );
            } catch (Exception e) {
                res = new ProcessNodeExecutionResultError()
                        .setMessage(e.getLocalizedMessage());
            }

            if (res == null) {
                res = new ProcessNodeExecutionResultError()
                        .setMessage("Process Node returned null result");
            }

            ProcessInstanceTaskEntity previousTask;
            if (previousNodeId != null) {
                previousTask = processInstanceTaskRepository
                        .findFirstByProcessInstanceIdAndProcessDefinitionNodeIdOrderByStartedDesc(
                                processInstance.getId(),
                                previousNodeId
                        );
            } else {
                previousTask = null;
            }


            processNodeExecutionResultHandler
                    .handleResult(
                            currentNode,
                            processInstance,
                            taskEntity,
                            previousTask,
                            res
                    );
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("Error executing process node ID: {} for Process Instance ID: {}")
                    .addArgument(currentNode.getId())
                    .addArgument(processInstance.getId())
                    .setCause(e)
                    .log();

            processNodeExecutionResultHandler
                    .handleResult(
                            currentNode,
                            processInstance,
                            taskEntity,
                            null,
                            new ProcessNodeExecutionResultError()
                                    .setMessage(e.getLocalizedMessage())
                    );
        }
    }

    public record WorkerPayload(
            @Nonnull Long processInstanceId,
            @Nullable Integer previousNodeId,
            @Nonnull Integer nextNodeId
    ) implements Serializable {

    }
}
