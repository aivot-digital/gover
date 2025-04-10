package de.aivot.GoverBackend.user.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class UserFilter implements Filter<UserEntity> {
    private String name;
    private Boolean deletedInIdp;
    private Boolean disabledInIdp;

    public static UserFilter create() {
        return new UserFilter();
    }

    @Override
    public Specification<UserEntity> build() {
        return SpecificationBuilder
                .create(UserEntity.class)
                .withContains("fullName", name)
                .withEquals("deletedInIdp", deletedInIdp)
                .withEquals("enabled", disabledInIdp == null ? null : !disabledInIdp)
                .build();
    }

    public String getName() {
        return name;
    }

    public UserFilter setName(String name) {
        this.name = name;
        return this;
    }

    public Boolean getDeletedInIdp() {
        return deletedInIdp;
    }

    public UserFilter setDeletedInIdp(Boolean deletedInIdp) {
        this.deletedInIdp = deletedInIdp;
        return this;
    }

    public Boolean getDisabledInIdp() {
        return disabledInIdp;
    }

    public UserFilter setDisabledInIdp(Boolean disabledInIdp) {
        this.disabledInIdp = disabledInIdp;
        return this;
    }
}
