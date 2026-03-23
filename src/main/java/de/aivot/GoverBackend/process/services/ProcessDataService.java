package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Service
public class ProcessDataService {
    private static final Pattern jsPattern = Pattern
            .compile("\\{\\{([^{}]+)\\}\\}");

    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final ProcessNodeRepository processDefinitionNodeRepository;
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;

    public ProcessDataService(ProcessInstanceTaskRepository processInstanceTaskRepository,
                              ProcessNodeRepository processDefinitionNodeRepository,
                              JavascriptEngineFactoryService javascriptEngineFactoryService) {
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
    }

    @Nullable
    public String interpolate(@Nonnull Map<String, Object> processData,
                              @Nullable String string) {
        if (string == null) {
            return null;
        }

        var result = string;
                //.replace("\\_", "_"); // Remove escaping for underscores because we think if markdown

        List<String> matches = jsPattern
                .matcher(result)
                .results()
                .map(MatchResult::group)
                .toList();

        for (String jsSnippet : matches) {
            String interpolated;
            try (var engine = javascriptEngineFactoryService.getEngine()) {
                fillJsEngineWithData(processData, engine);

                var code = JavascriptCode
                        .of(jsSnippet);

                interpolated = engine
                        .evaluateCode(code)
                        .toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (interpolated != null) {
                result = result
                        .replace(jsSnippet, interpolated);
            }
        }

        return result;
    }

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
