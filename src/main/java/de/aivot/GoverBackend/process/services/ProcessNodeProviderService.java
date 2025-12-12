package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.process.models.ProcessNodeProvider;
import kotlin.jvm.internal.Lambda;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProcessNodeProviderService {
    private final Map<String, ProcessNodeProvider> processNodeProviderMap;
    private final List<ProcessNodeProvider> processNodeProviders;

    @Autowired
    public ProcessNodeProviderService(List<ProcessNodeProvider> allProcessNodeProviders) {
        this.processNodeProviders = allProcessNodeProviders;

        this.processNodeProviderMap = new HashMap<>();
        for (ProcessNodeProvider provider : allProcessNodeProviders) {
            this.processNodeProviderMap.put(provider.getKey(), provider);
        }
    }

    public Optional<ProcessNodeProvider> getProcessNodeProvider(String key) {
        if (processNodeProviderMap.containsKey(key)) {
            return Optional.of(processNodeProviderMap.get(key));
        }

        return Optional.empty();
    }

    public List<ProcessNodeProvider> getAllProcessNodeProviders() {
        return processNodeProviders;
    }
}
