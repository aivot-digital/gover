package de.aivot.gover.backend.teams.filters;

import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.teams.entities.TeamEntity;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import java.util.List;

public class TeamFilter implements Filter<TeamEntity> {
    private Integer id;
    private List<Integer> ids;
    private String name;

    public static TeamFilter create() {
        return new TeamFilter();
    }

    @Nonnull
    @Override
    public Specification<TeamEntity> build() {
        return SpecificationBuilder
                .create(TeamEntity.class)
                .withEquals("id", id)
                .withInList("id", ids)
                .withContains("name", name)
                .build();
    }

    public Integer getId() {
        return id;
    }

    public TeamFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public TeamFilter setIds(List<Integer> ids) {
        this.ids = ids;
        return this;
    }

    public String getName() {
        return name;
    }

    public TeamFilter setName(String name) {
        this.name = name;
        return this;
    }
}
