package de.aivot.GoverBackend.userRoles.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class UserRoleAssignmentFilter implements Filter<UserRoleAssignmentEntity> {
    private Integer organizationalUnitMembershipId;
    private Integer teamMembershipId;
    private String userRoleId;

    private Boolean isOrgUnitAssignment;
    private Boolean isTeamAssignment;

    public static UserRoleAssignmentFilter create() {
        return new UserRoleAssignmentFilter();
    }

    @Override
    public Specification<UserRoleAssignmentEntity> build() {
        var filter = SpecificationBuilder
                .create(UserRoleAssignmentEntity.class)
                .withEquals("organizationalUnitMembershipId", organizationalUnitMembershipId)
                .withEquals("teamMembershipId", teamMembershipId)
                .withEquals("userRoleId", userRoleId);

        if (Boolean.TRUE.equals(isOrgUnitAssignment)) {
            filter = filter
                    .withNotNull("organizationalUnitMembershipId");
        }

        if (Boolean.TRUE.equals(isTeamAssignment)) {
            filter = filter
                    .withNotNull("teamMembershipId");
        }

        return filter.build();
    }

    public Integer getOrganizationalUnitMembershipId() {
        return organizationalUnitMembershipId;
    }

    public UserRoleAssignmentFilter setOrganizationalUnitMembershipId(Integer organizationalUnitMembershipId) {
        this.organizationalUnitMembershipId = organizationalUnitMembershipId;
        return this;
    }

    public Integer getTeamMembershipId() {
        return teamMembershipId;
    }

    public UserRoleAssignmentFilter setTeamMembershipId(Integer teamMembershipId) {
        this.teamMembershipId = teamMembershipId;
        return this;
    }

    public String getUserRoleId() {
        return userRoleId;
    }

    public UserRoleAssignmentFilter setUserRoleId(String userRoleId) {
        this.userRoleId = userRoleId;
        return this;
    }

    public Boolean getOrgUnitAssignment() {
        return isOrgUnitAssignment;
    }

    public UserRoleAssignmentFilter setOrgUnitAssignment(Boolean orgUnitAssignment) {
        isOrgUnitAssignment = orgUnitAssignment;
        return this;
    }

    public Boolean getTeamAssignment() {
        return isTeamAssignment;
    }

    public UserRoleAssignmentFilter setTeamAssignment(Boolean teamAssignment) {
        isTeamAssignment = teamAssignment;
        return this;
    }
}
