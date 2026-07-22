package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.javascript.services.JavascriptEngine;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.gover.backend.process.repositories.ProcessNodeRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final ProcessInstanceAttachmentSetRepository processInstanceAttachmentSetRepository;

    public ProcessDataService(ProcessInstanceTaskRepository processInstanceTaskRepository,
                              ProcessNodeRepository processDefinitionNodeRepository,
                              ProcessInstanceAttachmentRepository processInstanceAttachmentRepository,
                              ProcessInstanceAttachmentSetRepository processInstanceAttachmentSetRepository) {
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processInstanceAttachmentRepository = processInstanceAttachmentRepository;
        this.processInstanceAttachmentSetRepository = processInstanceAttachmentSetRepository;
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
                                                        @Nullable Integer previousNodeId,
                                                        @Nonnull ProcessInstanceTaskEntity currentTask) {
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
        allData.put("$$", getInstanceData(instance, previousNode, tasks, nodes, currentTask));
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
                                                @Nullable ProcessNodeEntity previousNode,
                                                @Nonnull List<ProcessInstanceTaskEntity> tasks,
                                                @Nonnull List<ProcessNodeEntity> nodes,
                                                @Nonnull ProcessInstanceTaskEntity currentTask) {
        var initialNode = processDefinitionNodeRepository
                .findById(instance.getInitialNodeId())
                .orElseThrow(() -> new RuntimeException("Initial node not found for process instance " + instance.getId()));

        List<ProcessInstanceAttachmentEntity> allAttachments = processInstanceAttachmentRepository
                .findAllByProcessInstanceId(instance.getId());
        List<ProcessInstanceAttachmentSetEntity> allAttachmentSets = processInstanceAttachmentSetRepository
                .findAllByProcessInstanceId(instance.getId());

        Map<String, Object> instanceData = new HashMap<>();

        instanceData.put("accessKey", instance.getAccessKey());
        instanceData.put("caseNumber", instance.getCaseNumber());
        instanceData.put("started", instance.getStarted());
        instanceData.put("initialPayload", instance.getInitialPayload());
        instanceData.put("assignedFileNumbers", instance.getAssignedFileNumbers());
        instanceData.put("identities", instance.getIdentities());
        instanceData.put("assignedUserId", instance.getAssignedUserId());
        instanceData.put("initialNodeDataKey", initialNode.getDataKey());
        instanceData.put("previousNodeDataKey", previousNode != null ? previousNode.getDataKey() : null);
        instanceData.put("attachmentSets", getAttachmentSetData(allAttachmentSets, allAttachments));
        instanceData.put("taskMetadata", getTaskMetaData(tasks, nodes));
        instanceData.put("currentTaskId", currentTask.getId());

        return instanceData;
    }

    @Nonnull
    private Map<String, Object> getAttachmentSetData(@Nonnull List<ProcessInstanceAttachmentSetEntity> attachmentSets,
                                                     @Nonnull List<ProcessInstanceAttachmentEntity> attachments) {
        var attachmentsBySetId = new HashMap<Integer, List<ProcessInstanceAttachmentEntity>>();
        for (var attachment : attachments) {
            attachmentsBySetId
                    .computeIfAbsent(attachment.getAttachmentSetId(), ignored -> new ArrayList<>())
                    .add(attachment);
        }

        var result = new LinkedHashMap<String, Object>();
        for (var attachmentSet : attachmentSets) {
            @SuppressWarnings("unchecked")
            var attachmentSetData = (Map<String, Object>) result.computeIfAbsent(
                    attachmentSet.getDataKey(),
                    ignored -> createAttachmentSetGroupData(attachmentSet)
            );
            var attachmentData = attachmentsBySetId
                    .getOrDefault(attachmentSet.getId(), List.of())
                    .stream()
                    .sorted((a, b) -> {
                        var positionComparison = a.getPosition().compareTo(b.getPosition());
                        return positionComparison == 0 ? a.getKey().compareTo(b.getKey()) : positionComparison;
                    })
                    .map(this::getAttachmentData)
                    .toList();

            @SuppressWarnings("unchecked")
            var sets = (List<Map<String, Object>>) attachmentSetData.get("sets");
            sets.add(getAttachmentSetItemData(attachmentSet, attachmentData));

            @SuppressWarnings("unchecked")
            var allSetAttachments = (List<Map<String, Object>>) attachmentSetData.get("attachments");
            allSetAttachments.addAll(attachmentData);
        }
        return result;
    }

    @Nonnull
    private Map<String, Object> createAttachmentSetGroupData(@Nonnull ProcessInstanceAttachmentSetEntity attachmentSet) {
        var attachmentSetData = new LinkedHashMap<String, Object>();
        attachmentSetData.put("name", attachmentSet.getName());
        attachmentSetData.put("dataKey", attachmentSet.getDataKey());
        attachmentSetData.put("sets", new ArrayList<Map<String, Object>>());
        attachmentSetData.put("attachments", new ArrayList<Map<String, Object>>());
        return attachmentSetData;
    }

    @Nonnull
    private Map<String, Object> getAttachmentSetItemData(@Nonnull ProcessInstanceAttachmentSetEntity attachmentSet,
                                                         @Nonnull List<Map<String, Object>> attachments) {
        var attachmentSetData = new LinkedHashMap<String, Object>();
        attachmentSetData.put("id", attachmentSet.getId());
        attachmentSetData.put("name", attachmentSet.getName());
        attachmentSetData.put("dataKey", attachmentSet.getDataKey());
        attachmentSetData.put("processInstanceTaskId", attachmentSet.getProcessInstanceTaskId());
        attachmentSetData.put("attachments", attachments);
        return attachmentSetData;
    }

    @Nonnull
    private Map<String, Object> getAttachmentData(@Nonnull ProcessInstanceAttachmentEntity attachment) {
        return Map.of(
                "filename", attachment.getFileName(),
                "storageProviderId", attachment.getStorageProviderId(),
                "storagePathFromRoot", attachment.getStoragePathFromRoot()
        );
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

    private Map<String, Object> getTaskMetaData(@Nonnull List<ProcessInstanceTaskEntity> tasks,
                                                @Nonnull List<ProcessNodeEntity> nodes) {
        Map<String, Object> taskMetadata = new HashMap<>();

        Map<Integer, ProcessNodeEntity> nodesById = new HashMap<>();
        for (ProcessNodeEntity node : nodes) {
            nodesById.put(node.getId(), node);
        }

        for (ProcessInstanceTaskEntity task : tasks) {
            var node = nodesById.get(task.getProcessNodeId());

            if (node == null) {
                continue;
            }

            var metadata = new HashMap<String, Object>();
            metadata.put("nodeId", node.getId());
            metadata.put("taskId", task.getId());
            metadata.put("assignedUserId", task.getAssignedUserId());
            metadata.put("started", task.getStarted());
            metadata.put("updated", task.getUpdated());
            metadata.put("finished", task.getFinished());
            metadata.put("runtime", task.getRuntime());
            metadata.put("previousProcessNodeId", task.getPreviousProcessNodeId());
            metadata.put("previousProcessNodePortKey", task.getPreviousProcessNodePortKey());
            metadata.put("previousProcessInstanceTaskId", task.getPreviousProcessInstanceTaskId());

            taskMetadata.put(node.getDataKey(), metadata);
        }

        return taskMetadata;
    }
}
