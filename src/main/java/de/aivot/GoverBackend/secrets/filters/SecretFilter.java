package de.aivot.GoverBackend.secrets.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class SecretFilter implements Filter<SecretEntity> {
    private String name;

    public static SecretFilter create() {
        return new SecretFilter();
    }

    @Nonnull
    @Override
    public Specification<SecretEntity> build() {
        return SpecificationBuilder
                .create(SecretEntity.class)
                .withContains("name", name)
                .build();
    }

    public String getName() {
        return name;
    }

    public SecretFilter setName(String name) {
        this.name = name;
        return this;
    }
}
