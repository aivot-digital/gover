package de.aivot.GoverBackend.teams.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.teams.entities.VTeamUserRoleAssignmentWithDetailsEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;


public class VTeamUserRoleAssignmentWithDetailsFilter implements Filter<VTeamUserRoleAssignmentWithDetailsEntity> {
    private Integer id;
    private Integer teamId;
    private String userId;

    public static VTeamUserRoleAssignmentWithDetailsFilter create() {
        return new VTeamUserRoleAssignmentWithDetailsFilter();
    }

    @Override
    public Specification<VTeamUserRoleAssignmentWithDetailsEntity> build() {
        return SpecificationBuilder
                .create(VTeamUserRoleAssignmentWithDetailsEntity.class)
                .withEquals("id", id)
                .withEquals("teamId", teamId)
                .withEquals("userId", userId)
                .build();
    }

    public Integer getId() {
        return id;
    }

    public VTeamUserRoleAssignmentWithDetailsFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public VTeamUserRoleAssignmentWithDetailsFilter setTeamId(Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VTeamUserRoleAssignmentWithDetailsFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
