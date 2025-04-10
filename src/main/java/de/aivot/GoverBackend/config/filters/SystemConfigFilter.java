package de.aivot.GoverBackend.config.filters;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class SystemConfigFilter implements Filter<SystemConfigEntity> {
    private String key;
    private Boolean publicConfig;

    public static SystemConfigFilter create() {
        return new SystemConfigFilter();
    }

    @Override
    public Specification<SystemConfigEntity> build() {
        return SpecificationBuilder
                .create(SystemConfigEntity.class)
                .withContains("key", key)
                .withEquals("publicConfig", publicConfig)
                .build();
    }

    public String getKey() {
        return key;
    }

    public SystemConfigFilter setKey(String key) {
        this.key = key;
        return this;
    }

    public Boolean getPublicConfig() {
        return publicConfig;
    }

    public SystemConfigFilter setPublicConfig(Boolean publicConfig) {
        this.publicConfig = publicConfig;
        return this;
    }
}
