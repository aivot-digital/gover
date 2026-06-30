package de.aivot.gover.backend.teams.filters;

import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.teams.entities.TeamEntity;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class TeamFilter implements Filter<TeamEntity> {
    private String name;

    public static TeamFilter create() {
        return new TeamFilter();
    }

    @Nonnull
    @Override
    public Specification<TeamEntity> build() {
        return SpecificationBuilder
                .create(TeamEntity.class)
                .withContains("name", name)
                .build();
    }

    public String getName() {
        return name;
    }

    public TeamFilter setName(String name) {
        this.name = name;
        return this;
    }
}
