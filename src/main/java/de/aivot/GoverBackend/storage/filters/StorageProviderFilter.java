package de.aivot.GoverBackend.storage.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

public class StorageProviderFilter implements Filter<StorageProviderEntity> {
    private String name;
    private StorageProviderType type;

    public static StorageProviderFilter create() {
        return new StorageProviderFilter();
    }

    @Nonnull
    @Override
    public Specification<StorageProviderEntity> build() {
        return SpecificationBuilder
                .create(StorageProviderEntity.class)
                .withContains("name", name)
                .withEquals("type", type)
                .build();
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
}
