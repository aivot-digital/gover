package de.aivot.GoverBackend.storage.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntity;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Map;

public class StorageIndexItemFilter implements Filter<StorageIndexItemEntity> {
    private Integer storageProviderId;
    private StorageProviderType storageProviderType;
    private String filename;
    private String mimeType;
    private Boolean onlyMissing;
    private Boolean excludeMissing;
    private Map<String, String> metadata;

    public static StorageIndexItemFilter create() {
        return new StorageIndexItemFilter();
    }

    @Nonnull
    @Override
    public Specification<StorageIndexItemEntity> build() {
        var sb = SpecificationBuilder
                .create(StorageIndexItemEntity.class)
                .withEquals("storageProviderId", storageProviderId)
                .withEquals("storageProviderType", storageProviderType)
                .withContains("filename", filename)
                .withContains("mimeType", mimeType);

        if (Boolean.TRUE.equals(onlyMissing)) {
            sb = sb.withEquals("missing", true);
        }

        if (Boolean.TRUE.equals(excludeMissing)) {
            sb = sb.withEquals("missing", false);
        }

        if (metadata != null) {
            for (var entry : metadata.entrySet()) {
                var parts = entry.getKey().split("\\.");
                sb = sb.withJsonEquals("metadata", Arrays.asList(parts), entry.getValue());
            }
        }

        return sb.build();
    }

    public Integer getStorageProviderId() {
        return storageProviderId;
    }

    public StorageIndexItemFilter setStorageProviderId(Integer storageProviderId) {
        this.storageProviderId = storageProviderId;
        return this;
    }

    public StorageProviderType getStorageProviderType() {
        return storageProviderType;
    }

    public StorageIndexItemFilter setStorageProviderType(StorageProviderType storageProviderType) {
        this.storageProviderType = storageProviderType;
        return this;
    }

    public String getFilename() {
        return filename;
    }

    public StorageIndexItemFilter setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public StorageIndexItemFilter setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public Boolean getOnlyMissing() {
        return onlyMissing;
    }

    public StorageIndexItemFilter setOnlyMissing(Boolean onlyMissing) {
        this.onlyMissing = onlyMissing;
        return this;
    }

    public Boolean getExcludeMissing() {
        return excludeMissing;
    }

    public StorageIndexItemFilter setExcludeMissing(Boolean excludeMissing) {
        this.excludeMissing = excludeMissing;
        return this;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public StorageIndexItemFilter setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }
}
