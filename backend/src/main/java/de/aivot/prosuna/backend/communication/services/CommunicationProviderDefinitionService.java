package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;

@Service
public class CommunicationProviderDefinitionService {
    private final Map<String, Map<Integer, CommunicationProviderDefinition<?, ?>>> communicationProviderDefinitions;

    @Autowired
    public CommunicationProviderDefinitionService(List<CommunicationProviderDefinition<?, ?>> allProviders) {
        this.communicationProviderDefinitions = allProviders
                .stream()
                .collect(
                        java.util.stream.Collectors.groupingBy(
                                CommunicationProviderDefinition::getKey,
                                java.util.stream.Collectors.toMap(
                                        CommunicationProviderDefinition::getMajorVersion,
                                        provider -> provider
                                )
                        )
                );
    }

    public Optional<CommunicationProviderDefinition<?, ?>> retrieveProviderDefinition(String key, Integer version) {
        var versions = communicationProviderDefinitions.get(key);
        if (versions == null) {
            return Optional.empty();
        }
        var provider = versions.get(version);
        return Optional.ofNullable(provider);
    }

    public List<CommunicationProviderDefinition<?, ?>> getAllProviderDefinitions() {
        return communicationProviderDefinitions
                .values()
                .stream()
                .flatMap(versions -> versions.values().stream())
                .sorted(Comparator.comparing(CommunicationProviderDefinition::getName))
                .toList();
    }
}
