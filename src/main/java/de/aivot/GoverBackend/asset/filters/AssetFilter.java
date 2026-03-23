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
                .build();
    }

}
