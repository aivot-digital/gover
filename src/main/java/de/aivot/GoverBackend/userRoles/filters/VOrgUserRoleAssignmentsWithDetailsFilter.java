package de.aivot.GoverBackend.userRoles.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.GoverBackend.userRoles.entities.VOrgUserRoleAssignmentsWithDetailsEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class VOrgUserRoleAssignmentsWithDetailsFilter implements Filter<VOrgUserRoleAssignmentsWithDetailsEntity> {
    private Integer userRoleId;
    private Integer orgUnitMembershipId;
    private Integer orgUnitMembershipOrganizationalUnitId;
    private String orgUnitMembershipUserId;
    private String orgUnitMembershipOrganizationalUnitName;
    private String orgUnitMembershipUserFullName;

    public static VOrgUserRoleAssignmentsWithDetailsFilter create() {
        return new VOrgUserRoleAssignmentsWithDetailsFilter();
    }

    @Override
    public Specification<VOrgUserRoleAssignmentsWithDetailsEntity> build() {
        return SpecificationBuilder
                .create(VOrgUserRoleAssignmentsWithDetailsEntity.class)
                .withEquals("userRoleId", userRoleId)
                .withEquals("orgUnitMembershipId", orgUnitMembershipId)
                .withEquals("orgUnitMembershipOrganizationalUnitId", orgUnitMembershipOrganizationalUnitId)
                .withEquals("orgUnitMembershipUserId", orgUnitMembershipUserId)
                .withEquals("orgUnitMembershipOrganizationalUnitName", orgUnitMembershipOrganizationalUnitName)
                .withEquals("orgUnitMembershipUserFullName", orgUnitMembershipUserFullName)
                .build();
    }

    public Integer getUserRoleId() {
        return userRoleId;
    }

    public VOrgUserRoleAssignmentsWithDetailsFilter setUserRoleId(Integer userRoleId) {
        this.userRoleId = userRoleId;
        return this;
    }

    public Integer getOrgUnitMembershipId() {
        return orgUnitMembershipId;
    }

    public VOrgUserRoleAssignmentsWithDetailsFilter setOrgUnitMembershipId(Integer orgUnitMembershipId) {
        this.orgUnitMembershipId = orgUnitMembershipId;
        return this;
    }

    public Integer getOrgUnitMembershipOrganizationalUnitId() {
        return orgUnitMembershipOrganizationalUnitId;
    }

    public VOrgUserRoleAssignmentsWithDetailsFilter setOrgUnitMembershipOrganizationalUnitId(Integer orgUnitMembershipOrganizationalUnitId) {
        this.orgUnitMembershipOrganizationalUnitId = orgUnitMembershipOrganizationalUnitId;
        return this;
    }

    public String getOrgUnitMembershipUserId() {
        return orgUnitMembershipUserId;
    }

    public VOrgUserRoleAssignmentsWithDetailsFilter setOrgUnitMembershipUserId(String orgUnitMembershipUserId) {
        this.orgUnitMembershipUserId = orgUnitMembershipUserId;
        return this;
    }

    public String getOrgUnitMembershipOrganizationalUnitName() {
        return orgUnitMembershipOrganizationalUnitName;
    }

    public VOrgUserRoleAssignmentsWithDetailsFilter setOrgUnitMembershipOrganizationalUnitName(String orgUnitMembershipOrganizationalUnitName) {
        this.orgUnitMembershipOrganizationalUnitName = orgUnitMembershipOrganizationalUnitName;
        return this;
    }

    public String getOrgUnitMembershipUserFullName() {
        return orgUnitMembershipUserFullName;
    }

    public VOrgUserRoleAssignmentsWithDetailsFilter setOrgUnitMembershipUserFullName(String orgUnitMembershipUserFullName) {
        this.orgUnitMembershipUserFullName = orgUnitMembershipUserFullName;
        return this;
    }
}
