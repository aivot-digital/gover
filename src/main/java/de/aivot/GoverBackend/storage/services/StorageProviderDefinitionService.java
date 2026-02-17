package de.aivot.GoverBackend.storage.services;

import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StorageProviderDefinitionService {
    private final Map<String, Map<Integer, StorageProviderDefinition<?>>> storageProviderDefinitions;

    @Autowired
    public StorageProviderDefinitionService(List<StorageProviderDefinition<?>> allProviders) {
        this.storageProviderDefinitions = allProviders
                .stream()
                .collect(
                        java.util.stream.Collectors.groupingBy(
                                StorageProviderDefinition::getKey,
                                java.util.stream.Collectors.toMap(
                                        StorageProviderDefinition::getVersion,
                                        provider -> provider
                                )
                        )
                );
    }

    public Optional<StorageProviderDefinition<?>> retrieveProviderDefinition(String key, Integer version) {
        var versions = storageProviderDefinitions.get(key);
        if (versions == null) {
            return Optional.empty();
        }
        var provider = versions.get(version);
        return Optional.ofNullable(provider);
    }
}
