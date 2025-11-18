package de.aivot.GoverBackend.teams.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.teams.entities.TeamEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

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
