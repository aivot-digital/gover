package de.aivot.GoverBackend.userRoles.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.userRoles.entities.VTeamUserRoleAssignmentsWithDetailsEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class VTeamUserRoleAssignmentsWithDetailsFilter implements Filter<VTeamUserRoleAssignmentsWithDetailsEntity> {
    private Integer userRoleId;
    private Integer teamMembershipId;
    private Integer teamMembershipTeamId;
    private String teamMembershipUserId;
    private String teamMembershipTeamName;
    private String teamMembershipUserFullName;

    public static VTeamUserRoleAssignmentsWithDetailsFilter create() {
        return new VTeamUserRoleAssignmentsWithDetailsFilter();
    }

    @Override
    public Specification<VTeamUserRoleAssignmentsWithDetailsEntity> build() {
        return SpecificationBuilder
                .create(VTeamUserRoleAssignmentsWithDetailsEntity.class)
                .withEquals("userRoleId", userRoleId)
                .withEquals("teamMembershipId", teamMembershipId)
                .withEquals("teamMembershipTeamId", teamMembershipTeamId)
                .withEquals("teamMembershipUserId", teamMembershipUserId)
                .withEquals("teamMembershipTeamName", teamMembershipTeamName)
                .withEquals("teamMembershipUserFullName", teamMembershipUserFullName)
                .build();
    }

    public Integer getUserRoleId() {
        return userRoleId;
    }

    public VTeamUserRoleAssignmentsWithDetailsFilter setUserRoleId(Integer userRoleId) {
        this.userRoleId = userRoleId;
        return this;
    }

    public Integer getTeamMembershipId() {
        return teamMembershipId;
    }

    public VTeamUserRoleAssignmentsWithDetailsFilter setTeamMembershipId(Integer teamMembershipId) {
        this.teamMembershipId = teamMembershipId;
        return this;
    }

    public Integer getTeamMembershipTeamId() {
        return teamMembershipTeamId;
    }

    public VTeamUserRoleAssignmentsWithDetailsFilter setTeamMembershipTeamId(Integer teamMembershipTeamId) {
        this.teamMembershipTeamId = teamMembershipTeamId;
        return this;
    }

    public String getTeamMembershipUserId() {
        return teamMembershipUserId;
    }

    public VTeamUserRoleAssignmentsWithDetailsFilter setTeamMembershipUserId(String teamMembershipUserId) {
        this.teamMembershipUserId = teamMembershipUserId;
        return this;
    }

    public String getTeamMembershipTeamName() {
        return teamMembershipTeamName;
    }

    public VTeamUserRoleAssignmentsWithDetailsFilter setTeamMembershipTeamName(String teamMembershipTeamName) {
        this.teamMembershipTeamName = teamMembershipTeamName;
        return this;
    }

    public String getTeamMembershipUserFullName() {
        return teamMembershipUserFullName;
    }

    public VTeamUserRoleAssignmentsWithDetailsFilter setTeamMembershipUserFullName(String teamMembershipUserFullName) {
        this.teamMembershipUserFullName = teamMembershipUserFullName;
        return this;
    }
}
