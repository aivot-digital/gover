package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.OrganizationalUnitMembershipEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;


public class OrganizationalUnitMembershipFilter implements Filter<OrganizationalUnitMembershipEntity> {
    private Integer organizationalUnitId;
    private List<Integer> organizationalUnitIds;
    private String userId;
    private List<String> userIds;

    public static OrganizationalUnitMembershipFilter create() {
        return new OrganizationalUnitMembershipFilter();
    }

    @Override
    public Specification<OrganizationalUnitMembershipEntity> build() {
        return SpecificationBuilder
                .create(OrganizationalUnitMembershipEntity.class)
                .withEquals("organizationalUnitId", organizationalUnitId)
                .withInList("organizationalUnitId", organizationalUnitIds)
                .withEquals("userId", userId)
                .withInList("userId", userIds)
                .build();
    }

    public Integer getOrganizationalUnitId() {
        return organizationalUnitId;
    }

    public OrganizationalUnitMembershipFilter setOrganizationalUnitId(Integer organizationalUnitId) {
        this.organizationalUnitId = organizationalUnitId;
        return this;
    }

    public List<Integer> getOrganizationalUnitIds() {
        return organizationalUnitIds;
    }

    public OrganizationalUnitMembershipFilter setOrganizationalUnitIds(List<Integer> organizationalUnitIds) {
        this.organizationalUnitIds = organizationalUnitIds;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public OrganizationalUnitMembershipFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public OrganizationalUnitMembershipFilter setUserIds(List<String> userIds) {
        this.userIds = userIds;
        return this;
    }
}
