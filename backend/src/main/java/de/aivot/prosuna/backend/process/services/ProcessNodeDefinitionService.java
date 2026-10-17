package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProcessNodeDefinitionService {
    private final Map<String, Map<Integer, ProcessNodeDefinition<?>>> processNodeDefinitionMap;
    private final List<ProcessNodeDefinition<?>> processNodeDefinitions;

    @Autowired
    public ProcessNodeDefinitionService(List<ProcessNodeDefinition<?>> allProcessNodeProviders) {
        this.processNodeDefinitions = allProcessNodeProviders;

        this.processNodeDefinitionMap = new HashMap<>();
        for (ProcessNodeDefinition<?> provider : allProcessNodeProviders) {
            processNodeDefinitionMap
                    .computeIfAbsent(provider.getKey(), k -> new HashMap<>())
                    .put(provider.getMajorVersion(), provider);
        }
    }

    public Optional<ProcessNodeDefinition<?>> getProcessNodeDefinition(String key, Integer version) {
        if (processNodeDefinitionMap.containsKey(key)) {
            var versionMap = processNodeDefinitionMap.get(key);
            if (versionMap.containsKey(version)) {
                return Optional.of(versionMap.get(version));
            }
        }
        return Optional.empty();
    }

    public Optional<ProcessNodeDefinition<?>> getProcessNodeDefinition(ProcessNodeEntity entity) {
        return getProcessNodeDefinition(
                entity.getProcessNodeDefinitionKey(),
                entity.getProcessNodeDefinitionVersion()
        );
    }

    public <T extends ProcessNodeDefinition<N>, N> Optional<T> getProcessNodeDefinition(ProcessNodeEntity entity, Class<T> clazz) {
        ProcessNodeDefinition<?> res = getProcessNodeDefinition(entity).orElse(null);

        if (res == null) {
            return Optional.empty();
        }

        if (clazz.isAssignableFrom(res.getClass())) {
            return Optional.of((T) res);
        }

        return Optional.empty();
    }

    public List<ProcessNodeDefinition<?>> getAllProcessNodeDefinitions() {
        return processNodeDefinitions;
    }
}
