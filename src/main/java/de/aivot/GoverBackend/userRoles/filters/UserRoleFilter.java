package de.aivot.GoverBackend.userRoles.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class UserRoleFilter implements Filter<UserRoleEntity> {
    private String name;

    public static UserRoleFilter create() {
        return new UserRoleFilter();
    }

    @Override
    public Specification<UserRoleEntity> build() {
        return SpecificationBuilder
                .create(UserRoleEntity.class)
                .withContains("name", name)
                .build();
    }

    public String getName() {
        return name;
    }

    public UserRoleFilter setName(String name) {
        this.name = name;
        return this;
    }
}
