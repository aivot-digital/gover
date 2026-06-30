package de.aivot.gover.backend.lib.models;

import org.springframework.data.jpa.domain.Specification;

public interface Filter<T> {
    Specification<T> build();
}
