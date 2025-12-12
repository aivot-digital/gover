package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.process.entities.ProcessDefinitionNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.repositories.ProcessDefinitionEdgeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;

@Service
public class ProcessNodeExecutionResultHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProcessNodeExecutionResultHandler.class);

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ProcessDefinitionEdgeRepository processDefinitionEdgeRepository;

    @Autowired
    public ProcessNodeExecutionResultHandler(RabbitTemplate rabbitTemplate,
                                             ProcessInstanceRepository processInstanceRepository,
                                             ProcessInstanceTaskRepository processInstanceTaskRepository,
                                             ProcessDefinitionEdgeRepository processDefinitionEdgeRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processDefinitionEdgeRepository = processDefinitionEdgeRepository;
    }

    public void handleResult(
            @Nonnull ProcessDefinitionNodeEntity currentNode,
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull ProcessInstanceTaskEntity processInstanceTask,
            @Nullable ProcessInstanceTaskEntity previousTask,
            @Nonnull ProcessNodeExecutionResult executionResult
    ) {
        switch (executionResult) {
            case ProcessNodeExecutionResultError err -> {
                // TODO: Handle Error Result
                logger
                        .atError()
                        .setMessage("Error in Process Instance ID: {}, Node ID: {}: {}")
                        .addArgument(processInstance.getId())
                        .addArgument(currentNode.getId())
                        .addArgument(err.getMessage())
                        .log();

                processInstanceTask.setStatus(ProcessTaskStatus.Failed);
                processInstanceTask.setFinished(LocalDateTime.now());
                processInstanceTaskRepository.save(processInstanceTask);

                processInstance.setStatus(ProcessInstanceStatus.Failed);
                processInstance.setFinished(LocalDateTime.now());
                processInstanceRepository.save(processInstance);
            }
            case ProcessNodeExecutionResultTaskCompleted taskCompleted -> {
                var outEdge = processDefinitionEdgeRepository
                        .findByFromNodeIdAndViaPort(currentNode.getId(), taskCompleted.getViaPort());

                if (outEdge.isEmpty()) {
                    // TODO: Handle Error Result
                    logger
                            .atError()
                            .setMessage("No outgoing edge found for Process Instance ID: {}, Node ID: {}, via port: {}")
                            .addArgument(processInstance.getId())
                            .addArgument(currentNode.getId())
                            .addArgument(taskCompleted.getViaPort())
                            .log();

                    processInstanceTask.setStatus(ProcessTaskStatus.Failed);
                    processInstanceTask.setFinished(LocalDateTime.now());
                    processInstanceTaskRepository.save(processInstanceTask);

                    processInstance.setStatus(ProcessInstanceStatus.Failed);
                    processInstance.setFinished(LocalDateTime.now());
                    processInstanceRepository.save(processInstance);
                } else {
                    var newMetadata = taskCompleted.getMetadata();
                    if (newMetadata == null) {
                        newMetadata = new HashMap<>();
                    }
                    processInstanceTask.setMetaData(newMetadata);

                    var newWorkingData = taskCompleted.getOutput();
                    if (newWorkingData == null) {
                        newWorkingData = previousTask != null ?
                                previousTask.getWorkingData() :
                                processInstance.getInitialPayload();
                    }
                    processInstanceTask.setWorkingData(newWorkingData);

                    processInstanceTask.setStatus(ProcessTaskStatus.Completed);
                    processInstanceTask.setFinished(LocalDateTime.now());

                    processInstanceTaskRepository.save(processInstanceTask);

                    var nextPayload = new ProcessWorker.WorkerPayload(
                            processInstance.getId(),
                            currentNode.getId(),
                            outEdge.get().getToNodeId()
                    );

                    rabbitTemplate.convertAndSend(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE, nextPayload);
                }
            }
            case ProcessNodeExecutionResultInstanceCompleted instanceCompleted -> {
                var newMetadata = instanceCompleted.getMetadata();
                if (newMetadata == null) {
                    newMetadata = new HashMap<>();
                }
                processInstanceTask.setMetaData(newMetadata);

                var newWorkingData = instanceCompleted.getOutput();
                if (newWorkingData == null) {
                    newWorkingData = previousTask != null ?
                            previousTask.getWorkingData() :
                            processInstance.getInitialPayload();
                }
                processInstanceTask.setWorkingData(newWorkingData);

                processInstanceTask.setStatus(ProcessTaskStatus.Completed);
                processInstanceTask.setFinished(LocalDateTime.now());
                processInstanceTaskRepository.save(processInstanceTask);

                processInstance.setStatus(ProcessInstanceStatus.Completed);
                processInstance.setFinished(LocalDateTime.now());
                processInstanceRepository.save(processInstance);
            }
            case ProcessNodeExecutionResultTaskAssigned assigned -> {
                var newMetadata = assigned.getMetadata();
                if (newMetadata == null) {
                    newMetadata = new HashMap<>();
                }
                processInstanceTask.setMetaData(newMetadata);

                var newWorkingData = assigned.getOutput();
                if (newWorkingData == null) {
                    newWorkingData = previousTask != null ?
                            previousTask.getWorkingData() :
                            processInstance.getInitialPayload();
                }
                processInstanceTask.setWorkingData(newWorkingData);
                processInstanceTask.setAssignedUserId(assigned.getAssignedUserId());
                processInstanceTaskRepository.save(processInstanceTask);
                // TODO: send notification
            }
            default ->
                    throw new IllegalStateException("Unexpected node execution result: " + executionResult.getClass().getName());
        }
    }
}
