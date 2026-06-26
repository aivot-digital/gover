package de.aivot.gover.backend.destination.filters;

import de.aivot.gover.backend.destination.entities.Destination;
import de.aivot.gover.backend.destination.enums.DestinationType;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

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
