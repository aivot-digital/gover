package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.VOrganizationalUnitMembershipWithDetailsEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;


public class VOrganizationalUnitMembershipWithDetailsFilter implements Filter<VOrganizationalUnitMembershipWithDetailsEntity> {
    private List<Integer> organizationalUnitIds;
    private Integer organizationalUnitId;
    private String organizationalUnitName;
    private String userId;
    private List<String> userIds;
    private String userFullName;
    private String userEmail;

    private Boolean userEnabled;
    private Boolean userVerified;
    private Boolean userGlobalAdmin;
    private Boolean userDeletedInIdp;

    public static VOrganizationalUnitMembershipWithDetailsFilter create() {
        return new VOrganizationalUnitMembershipWithDetailsFilter();
    }

    @Override
    public Specification<VOrganizationalUnitMembershipWithDetailsEntity> build() {
        var builder = SpecificationBuilder
                .create(VOrganizationalUnitMembershipWithDetailsEntity.class)
                .withEquals("organizationalUnitId", organizationalUnitId)
                .withInList("organizationalUnitId", organizationalUnitIds)
                .withContains("organizationalUnitName", organizationalUnitName)
                .withEquals("userId", userId)
                .withInList("userId", userIds)
                .withContains("userFullName", userFullName)
                .withContains("userEmail", userEmail);

        if (userEnabled != null) {
            builder = builder
                    .withEquals("userEnabled", userEnabled);
        }

        if (userVerified != null) {
            builder = builder
                    .withEquals("userVerified", userVerified);
        }

        if (userGlobalAdmin != null) {
            builder = builder
                    .withEquals("userGlobalAdmin", userGlobalAdmin);
        }

        if (userDeletedInIdp != null) {
            builder = builder
                    .withEquals("userDeletedInIdp", userDeletedInIdp);
        }

        return builder
                .build();
    }

    public List<Integer> getOrganizationalUnitIds() {
        return organizationalUnitIds;
    }

    public VOrganizationalUnitMembershipWithDetailsFilter setOrganizationalUnitIds(List<Integer> organizationalUnitIds) {
        this.organizationalUnitIds = organizationalUnitIds;
        return this;
    }

    public Integer getOrganizationalUnitId() {
        return organizationalUnitId;
    }

    public VOrganizationalUnitMembershipWithDetailsFilter setOrganizationalUnitId(Integer organizationalUnitId) {
        this.organizationalUnitId = organizationalUnitId;
        return this;
    }

    public String getOrganizationalUnitName() {
        return organizationalUnitName;
    }

    public VOrganizationalUnitMembershipWithDetailsFilter setOrganizationalUnitName(String organizationalUnitName) {
        this.organizationalUnitName = organizationalUnitName;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VOrganizationalUnitMembershipWithDetailsFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public VOrganizationalUnitMembershipWithDetailsFilter setUserIds(List<String> userIds) {
        this.userIds = userIds;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public VOrganizationalUnitMembershipWithDetailsFilter setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public VOrganizationalUnitMembershipWithDetailsFilter setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public VOrganizationalUnitMembershipWithDetailsFilter setUserEnabled(Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    public Boolean getUserVerified() {
        return userVerified;
    }

    public VOrganizationalUnitMembershipWithDetailsFilter setUserVerified(Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    public Boolean getUserGlobalAdmin() {
        return userGlobalAdmin;
    }

    public VOrganizationalUnitMembershipWithDetailsFilter setUserGlobalAdmin(Boolean userGlobalAdmin) {
        this.userGlobalAdmin = userGlobalAdmin;
        return this;
    }

    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public VOrganizationalUnitMembershipWithDetailsFilter setUserDeletedInIdp(Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }
}
