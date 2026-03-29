package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
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
 * into the compact map structure used throughout the process engine. Template rendering lives in
 * {@link TemplateRenderService} now, but the shared JavaScript data contract still belongs here because process nodes
 * outside the template renderer also depend on the same globals.
 */
@Service
public class ProcessDataService {
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final ProcessNodeRepository processDefinitionNodeRepository;

    public ProcessDataService(ProcessInstanceTaskRepository processInstanceTaskRepository,
                              ProcessNodeRepository processDefinitionNodeRepository) {
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
    }

    /**
     * Registers the canonical process data roots on a JavaScript engine.
     *
     * <p>This method stays public and static because other process-related services already rely on the same process
     * data contract. Only the reserved roots and node snapshots are exported so callers get the expected process scope
     * without accidentally leaking every arbitrary map entry as a global variable.
     */
    public static void fillJsEngineWithData(@Nonnull Map<String, Object> processData, JavascriptEngine engine) {
        engine
                .registerGlobalObject("$", processData.get("$"))
                .registerGlobalObject("$$", processData.get("$$"));

        for (String key : processData.keySet()) {
            if (key.startsWith("_")) {
                engine.registerGlobalObject(key, processData.get(key));
            }
        }
    }

    /**
     * Builds the effective process data snapshot that downstream nodes consume.
     *
     * <p>The result intentionally flattens previous payload, instance metadata, and latest node data into a compact
     * map because that structure can be handed directly to JavaScript evaluation without extra transformation at each
     * call site.
     */
    @Nonnull
    public ProcessExecutionData foldProcessInstanceData(@Nonnull ProcessInstanceEntity instance,
                                                        @Nullable Integer previousNodeId) {
        List<ProcessNodeEntity> nodes = processDefinitionNodeRepository
                .findAllByProcessId(
                        instance.getProcessId()
                );

        List<ProcessInstanceTaskEntity> tasks = processInstanceTaskRepository
                .getLatestTasksByProcessInstanceId(instance.getId());

        ProcessInstanceTaskEntity previousTask = previousNodeId == null ?
                null :
                tasks
                        .stream()
                        .filter(t -> t.getProcessNodeId().equals(previousNodeId))
                        .findFirst()
                        .orElse(null);

        var allData = new ProcessExecutionData();
        allData.put("$", previousTask != null ? previousTask.getProcessData() : instance.getInitialPayload());

        Map<String, Object> instanceData = new HashMap<>();
        // instanceData.put("instance", instance);
        // TODO: add attachments
        // TODO: Specify Instance Data
        allData.put("$$", instanceData);

        for (ProcessInstanceTaskEntity task : tasks) {
            var node = nodes
                    .stream()
                    .filter(n -> n.getId().equals(task.getProcessNodeId()))
                    .findFirst()
                    .orElse(null);

            if (node == null) {
                continue;
            }

            allData.put("_" + node.getDataKey(), task.getNodeData());
        }

        return allData;
    }
}
