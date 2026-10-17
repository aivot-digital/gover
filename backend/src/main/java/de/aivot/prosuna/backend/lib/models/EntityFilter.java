package de.aivot.prosuna.backend.lib.models;

import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.LinkedList;
import java.util.List;

public abstract class EntityFilter<T> implements Filter<T> {
    private final List<Specification<T>> additionalSpecifications = new LinkedList<>();

    public abstract SpecificationBuilder<T> createSpecBuilder();

    public Specification<T> build() {
        var builder = createSpecBuilder();

        for (var spec : additionalSpecifications) {
            builder = builder.withSpecification(spec);
        }

        return builder.build();
    }

    public EntityFilter<T> addAdditionalSpecification(Specification<T> specification) {
        additionalSpecifications.add(specification);
        return this;
    }
}
