package de.aivot.gover.backend.userRoles.filters;

import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.userRoles.entities.SystemRoleEntity;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class SystemRoleFilter implements Filter<SystemRoleEntity> {
    private String name;

    public static UserRoleFilter create() {
        return new UserRoleFilter();
    }

    @Override
    public Specification<SystemRoleEntity> build() {
        return SpecificationBuilder
                .create(SystemRoleEntity.class)
                .withContains("name", name)
                .build();
    }

    public String getName() {
        return name;
    }

    public SystemRoleFilter setName(String name) {
        this.name = name;
        return this;
    }
}