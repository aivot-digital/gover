package de.aivot.gover.backend.teams.filters;

import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.teams.entities.TeamMembershipEntity;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import java.util.List;

public class TeamMembershipFilter implements Filter<TeamMembershipEntity> {
    private Integer teamId;
    private List<Integer> teamIds;
    private String userId;

    public static TeamMembershipFilter create() {
        return new TeamMembershipFilter();
    }

    @Nonnull
    @Override
    public Specification<TeamMembershipEntity> build() {
        return SpecificationBuilder
                .create(TeamMembershipEntity.class)
                .withEquals("teamId", teamId)
                .withInList("teamId", teamIds)
                .withEquals("userId", userId)
                .build();
    }

    public Integer getTeamId() {
        return teamId;
    }

    public TeamMembershipFilter setTeamId(Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    public List<Integer> getTeamIds() {
        return teamIds;
    }

    public TeamMembershipFilter setTeamIds(List<Integer> teamIds) {
        this.teamIds = teamIds;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public TeamMembershipFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
