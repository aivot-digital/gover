package de.aivot.GoverBackend.asset.filters;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class AssetFilter implements Filter<AssetEntity> {
    private String filename;
    private String uploaderId;
    private String contentType;
    private Boolean isPrivate;
    private Integer storageProviderId;

    public static AssetFilter create() {
        return new AssetFilter();
    }

    @Override
    public Specification<AssetEntity> build() {
        return SpecificationBuilder
                .create(AssetEntity.class)
                .withContains("filename", filename)
                .withEquals("uploaderId", uploaderId)
                .withContains("contentType", contentType)
                .withEquals("isPrivate", isPrivate)
                .withEquals("storageProviderId", storageProviderId)
                .build();
    }

    public Integer getStorageProviderId() {
        return storageProviderId;
    }

    public AssetFilter setStorageProviderId(Integer storageProviderId) {
        this.storageProviderId = storageProviderId;
        return this;
    }
}
