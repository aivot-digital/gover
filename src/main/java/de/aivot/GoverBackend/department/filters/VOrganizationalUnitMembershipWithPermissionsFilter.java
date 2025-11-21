package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.VOrganizationalUnitMembershipWithPermissionsEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;


public class VOrganizationalUnitMembershipWithPermissionsFilter implements Filter<VOrganizationalUnitMembershipWithPermissionsEntity> {
    private Integer membershipId;
    private Integer organizationalUnitId;
    private String userId;

    public static VOrganizationalUnitMembershipWithPermissionsFilter create() {
        return new VOrganizationalUnitMembershipWithPermissionsFilter();
    }

    @Override
    public Specification<VOrganizationalUnitMembershipWithPermissionsEntity> build() {
        return SpecificationBuilder
                .create(VOrganizationalUnitMembershipWithPermissionsEntity.class)
                .withEquals("membershipId", membershipId)
                .withEquals("organizationalUnitId", organizationalUnitId)
                .withEquals("userId", userId)
                .build();
    }

    public Integer getMembershipId() {
        return membershipId;
    }

    public VOrganizationalUnitMembershipWithPermissionsFilter setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    public Integer getOrganizationalUnitId() {
        return organizationalUnitId;
    }

    public VOrganizationalUnitMembershipWithPermissionsFilter setOrganizationalUnitId(Integer organizationalUnitId) {
        this.organizationalUnitId = organizationalUnitId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VOrganizationalUnitMembershipWithPermissionsFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
