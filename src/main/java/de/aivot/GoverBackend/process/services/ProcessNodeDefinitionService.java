package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProcessNodeDefinitionService {
    private final Map<String, Map<Integer, ProcessNodeDefinition>> processNodeDefinitionMap;
    private final List<ProcessNodeDefinition> processNodeDefinitions;

    @Autowired
    public ProcessNodeDefinitionService(List<ProcessNodeDefinition> allProcessNodeProviders) {
        this.processNodeDefinitions = allProcessNodeProviders;

        this.processNodeDefinitionMap = new HashMap<>();
        for (ProcessNodeDefinition provider : allProcessNodeProviders) {
            processNodeDefinitionMap
                    .computeIfAbsent(provider.getKey(), k -> new HashMap<>())
                    .put(provider.getVersion(), provider);
        }
    }

    public Optional<ProcessNodeDefinition> getProcessNodeDefinition(String key, Integer version) {
        if (processNodeDefinitionMap.containsKey(key)) {
            var versionMap = processNodeDefinitionMap.get(key);
            if (versionMap.containsKey(version)) {
                return Optional.of(versionMap.get(version));
            }
        }
        return Optional.empty();
    }

    public List<ProcessNodeDefinition> getAllProcessNodeDefinitions() {
        return processNodeDefinitions;
    }
}
