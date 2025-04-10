package de.aivot.GoverBackend.config.filters;

import de.aivot.GoverBackend.config.entities.UserConfigEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class UserConfigFilter implements Filter<UserConfigEntity> {
    private String key;
    private String userId;
    private Boolean publicConfig;

    public static UserConfigFilter create() {
        return new UserConfigFilter();
    }

    @Override
    public Specification<UserConfigEntity> build() {
        return SpecificationBuilder
                .create(UserConfigEntity.class)
                .withContains("key", key)
                .withEquals("userId", userId)
                .withEquals("publicConfig", publicConfig)
                .build();
    }

    public String getKey() {
        return key;
    }

    public UserConfigFilter setKey(String key) {
        this.key = key;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public UserConfigFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getPublicConfig() {
        return publicConfig;
    }

    public UserConfigFilter setPublicConfig(Boolean publicConfig) {
        this.publicConfig = publicConfig;
        return this;
    }
}
