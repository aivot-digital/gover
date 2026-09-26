package de.aivot.prosuna.backend.lib.models;

import org.springframework.data.jpa.domain.Specification;

public interface Filter<T> {
    Specification<T> build();
}
