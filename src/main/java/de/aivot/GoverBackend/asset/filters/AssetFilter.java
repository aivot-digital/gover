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

    public String getFilename() {
        return filename;
    }

    public AssetFilter setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public AssetFilter setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public AssetFilter setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public AssetFilter setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
        return this;
    }
}
