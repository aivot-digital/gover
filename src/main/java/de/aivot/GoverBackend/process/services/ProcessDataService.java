package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the effective process data snapshot used while executing process nodes.
 *
 * <p>This service intentionally stays focused on one responsibility: collecting the different process data sources
 * into the compact map structure used throughout the process engine. Template rendering lives in {@link TemplateRenderService} now, but the shared JavaScript data contract still
 * belongs here because process nodes outside the template renderer also depend on the same globals.
 */
@Service
public class ProcessDataService {
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final ProcessNodeRepository processDefinitionNodeRepository;
    private final ProcessInstanceAttachmentRepository processInstanceAttachmentRepository;

    public ProcessDataService(ProcessInstanceTaskRepository processInstanceTaskRepository,
                              ProcessNodeRepository processDefinitionNodeRepository, ProcessInstanceAttachmentRepository processInstanceAttachmentRepository) {
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processInstanceAttachmentRepository = processInstanceAttachmentRepository;
    }

    /**
     * Registers the canonical process data roots on a JavaScript engine.
     *
     * <p>This method stays public and static because other process-related services already rely on the same process
     * data contract. Only the reserved roots and node snapshots are exported so callers get the expected process scope without accidentally leaking every arbitrary map entry as a
     * global variable.
     */
    public static void fillJsEngineWithData(@Nonnull Map<String, Object> processData, JavascriptEngine engine) {
        engine
                .registerGlobalObject("$", processData.get("$"))
                .registerGlobalObject("$$", processData.get("$$"))
                .registerGlobalObject("_", processData.get("_"));
    }

    /**
     * Builds the effective process data snapshot that downstream nodes consume.
     *
     * <p>The result intentionally flattens previous payload, instance metadata, and latest node data into a compact
     * map because that structure can be handed directly to JavaScript evaluation without extra transformation at each call site.
     */
    @Nonnull
    public ProcessExecutionData foldProcessInstanceData(@Nonnull ProcessInstanceEntity instance,
                                                        @Nullable Integer previousNodeId) {
        var nodes = processDefinitionNodeRepository
                .findAllByProcessId(
                        instance.getProcessId()
                );

        var tasks = processInstanceTaskRepository
                .getLatestTasksByProcessInstanceId(instance.getId());

        var previousTask = previousNodeId == null ?
                null :
                tasks
                        .stream()
                        .filter(t -> t.getProcessNodeId().equals(previousNodeId))
                        .findFirst()
                        .orElse(null);

        var previousNode = previousTask == null ?
                null :
                processDefinitionNodeRepository
                        .findById(previousTask.getProcessNodeId())
                        .orElse(null);

        var allData = new ProcessExecutionData();

        allData.put("$", getProcessData(instance, previousTask));
        allData.put("$$", getInstanceData(instance, previousNode));
        allData.put("_", getNodeData(tasks, nodes));

        return allData;
    }

    @Nonnull
    private Map<String, Object> getProcessData(@Nonnull ProcessInstanceEntity instance,
                                               @Nullable ProcessInstanceTaskEntity previousTask) {
        return previousTask != null ? previousTask.getProcessData() : instance.getInitialPayload();
    }

    @Nonnull
    private Map<String, Object> getInstanceData(@Nonnull ProcessInstanceEntity instance,
                                                @Nullable ProcessNodeEntity previousNode) {
        var initialNode = processDefinitionNodeRepository
                .findById(instance.getInitialNodeId())
                .orElseThrow(() -> new RuntimeException("Initial node not found for process instance " + instance.getId()));

        List<ProcessInstanceAttachmentEntity> allAttachments = processInstanceAttachmentRepository
                .findAllByProcessInstanceId(instance.getId());

        Map<String, Object> instanceData = new HashMap<>();

        instanceData.put("accessKey", instance.getAccessKey());
        instanceData.put("started", instance.getStarted());
        instanceData.put("initialPayload", instance.getInitialPayload());
        instanceData.put("assignedFileNumbers", instance.getAssignedFileNumbers());
        instanceData.put("identities", instance.getIdentities());
        instanceData.put("assignedUserId", instance.getAssignedUserId());
        instanceData.put("initialNodeDataKey", initialNode.getDataKey());
        instanceData.put("previousNodeDataKey", previousNode != null ? previousNode.getDataKey() : null);
        instanceData.put("attachments", allAttachments
                .stream()
                .map((att) -> Map.of(
                        "filename", att.getFileName(),
                        "storageProviderId", att.getStorageProviderId(),
                        "storagePathFromRoot", att.getStoragePathFromRoot()
                ))
                .toList());

        return instanceData;
    }

    @Nonnull
    private Map<String, Object> getNodeData(@Nonnull List<ProcessInstanceTaskEntity> tasks,
                                            @Nonnull List<ProcessNodeEntity> nodes) {
        Map<String, Object> nodeData = new HashMap<>();

        for (ProcessInstanceTaskEntity task : tasks) {
            var node = nodes
                    .stream()
                    .filter(n -> n.getId().equals(task.getProcessNodeId()))
                    .findFirst()
                    .orElse(null);

            if (node == null) {
                continue;
            }

            nodeData.put(node.getDataKey(), task.getNodeData());
        }

        return nodeData;
    }
}
