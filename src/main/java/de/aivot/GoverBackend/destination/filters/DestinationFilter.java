package de.aivot.GoverBackend.destination.filters;

import de.aivot.GoverBackend.destination.entities.Destination;
import de.aivot.GoverBackend.destination.enums.DestinationType;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class DestinationFilter implements Filter<Destination> {
    private String name;
    private DestinationType type;

    public static DestinationFilter create() {
        return new DestinationFilter();
    }

    @Nonnull
    @Override
    public Specification<Destination> build() {
        return SpecificationBuilder
                .create(Destination.class)
                .withContains("name", name)
                .withEquals("type", type)
                .build();
    }

    public String getName() {
        return name;
    }

    public DestinationFilter setName(String name) {
        this.name = name;
        return this;
    }

    public DestinationType getType() {
        return type;
    }

    public DestinationFilter setType(DestinationType type) {
        this.type = type;
        return this;
    }
}
