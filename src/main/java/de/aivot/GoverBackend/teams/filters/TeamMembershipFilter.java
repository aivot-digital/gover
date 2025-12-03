package de.aivot.GoverBackend.teams.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.teams.entities.TeamEntity;
import de.aivot.GoverBackend.teams.entities.TeamMembershipEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class TeamMembershipFilter implements Filter<TeamMembershipEntity> {
    private Integer teamId;
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

    public String getUserId() {
        return userId;
    }

    public TeamMembershipFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
