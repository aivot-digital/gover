package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.process.entities.ProcessDefinitionNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.repositories.ProcessDefinitionNodeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Service
public class ProcessDataService {
    private static final Pattern lastAccessPattern = Pattern
            .compile("\\$\\.(meta|data)\\.[a-zA-Z0-9_.]+");
    private static final Pattern directAccessPattern = Pattern
            .compile("\\$[a-zA-Z0-9_]+\\.(meta|data)\\.[a-zA-Z0-9_.]+");

    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final ProcessDefinitionNodeRepository processDefinitionNodeRepository;

    public ProcessDataService(ProcessInstanceTaskRepository processInstanceTaskRepository,
                              ProcessDefinitionNodeRepository processDefinitionNodeRepository) {
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
    }

    @Nullable
    public String interpolate(@Nonnull Map<String, Object> processData,
                              @Nullable String string) {
        if (string == null) {
            return null;
        }

        var result = string;

        List<String> matches = new LinkedList<>();
        lastAccessPattern
                .matcher(string)
                .results()
                .map(MatchResult::group)
                .forEach(matches::add);

        directAccessPattern
                .matcher(string)
                .results()
                .map(MatchResult::group)
                .forEach(matches::add);

        for (String path : matches) {
            Object value = processData;
            String[] keys = path.split("\\.");

            for (String key : keys) {
                if (value instanceof Map<?, ?> map) {
                    value = map.get(key);
                } else {
                    value = null;
                    break;
                }
            }

            result = result.replace(path, value != null ? value.toString() : "null");
        }

        return result;
    }

    @Nonnull
    public Map<String, Object> foldProcessInstanceData(@Nonnull ProcessInstanceEntity instance,
                                                       @Nullable Integer previousNodeId) {
        List<ProcessDefinitionNodeEntity> nodes = processDefinitionNodeRepository
                .findAllByProcessDefinitionIdAndProcessDefinitionVersion(
                        instance.getProcessDefinitionId(),
                        instance.getProcessDefinitionVersion()
                );

        List<ProcessInstanceTaskEntity> tasks = processInstanceTaskRepository
                .getLatestTasksByProcessInstanceId(instance.getId());

        ProcessInstanceTaskEntity previousTask = previousNodeId == null ?
                null :
                tasks
                        .stream()
                        .filter(t -> t.getProcessDefinitionNodeId().equals(previousNodeId))
                        .findFirst()
                        .orElse(null);

        Map<String, Object> previousData = new HashMap<>();
        previousData.put("data", previousTask != null ? previousTask.getWorkingData() : instance.getInitialPayload());
        previousData.put("meta", previousTask != null ? previousTask.getMetaData() : null);

        Map<String, Object> instanceData = new HashMap<>();
        instanceData.put("$", previousData);

        for (ProcessInstanceTaskEntity task : tasks) {
            var node = nodes
                    .stream()
                    .filter(n -> n.getId().equals(task.getProcessDefinitionNodeId()))
                    .findFirst()
                    .orElse(null);

            if (node == null) {
                continue;
            }

            Map<String, Object> taskData = new HashMap<>();
            taskData.put("data", task.getWorkingData());
            taskData.put("meta", task.getMetaData());
            instanceData.put("$" + node.getDataKey(), taskData);
        }

        return instanceData;
    }
}
