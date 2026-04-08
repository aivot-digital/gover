package de.aivot.GoverBackend.storage.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageProviderFilter implements Filter<StorageProviderEntity> {
    private Integer idIsNot;
    private String name;
    private StorageProviderType type;
    private Boolean readOnlyStorage;
    private Boolean systemProvider;
    private String storageProviderDefinitionKey;
    private Integer storageProviderDefinitionVersion;

    private Map<String, String> additionalProperties = new HashMap<>();

    public static StorageProviderFilter create() {
        return new StorageProviderFilter();
    }

    @Nonnull
    @Override
    public Specification<StorageProviderEntity> build() {
        var filter = SpecificationBuilder
                .create(StorageProviderEntity.class)
                .withNotEquals("id", idIsNot)
                .withContains("name", name)
                .withEquals("type", type)
                .withEquals("readOnlyStorage", readOnlyStorage)
                .withEquals("systemProvider", systemProvider)
                .withEquals("storageProviderDefinitionKey", storageProviderDefinitionKey)
                .withEquals("storageProviderDefinitionVersion", storageProviderDefinitionVersion);

        for (var entry : additionalProperties.entrySet()) {
            filter.withJsonEquals("configuration", List.of(entry.getKey()), entry.getValue());
        }

        return filter.build();
    }

    public String getName() {
        return name;
    }

    public StorageProviderFilter setName(String name) {
        this.name = name;
        return this;
    }

    public StorageProviderType getType() {
        return type;
    }

    public StorageProviderFilter setType(StorageProviderType type) {
        this.type = type;
        return this;
    }

    public Boolean getReadOnlyStorage() {
        return readOnlyStorage;
    }

    public StorageProviderFilter setReadOnlyStorage(Boolean readOnlyStorage) {
        this.readOnlyStorage = readOnlyStorage;
        return this;
    }

    public Boolean getSystemProvider() {
        return systemProvider;
    }

    public StorageProviderFilter setSystemProvider(Boolean systemProvider) {
        this.systemProvider = systemProvider;
        return this;
    }

    public String getStorageProviderDefinitionKey() {
        return storageProviderDefinitionKey;
    }

    public StorageProviderFilter setStorageProviderDefinitionKey(String storageProviderDefinitionKey) {
        this.storageProviderDefinitionKey = storageProviderDefinitionKey;
        return this;
    }

    public StorageProviderFilter setStorageProviderDefinitionVersion(Integer storageProviderDefinitionVersion) {
        this.storageProviderDefinitionVersion = storageProviderDefinitionVersion;
        return this;
    }

    public StorageProviderFilter setIdIsNot(Integer idIsNot) {
        this.idIsNot = idIsNot;
        return this;
    }

    public StorageProviderFilter addAdditionalProperty(String key, String value) {
        this.additionalProperties.put(key, value);
        return this;
    }
}
