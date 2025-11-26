package de.aivot.GoverBackend.teams.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.teams.entities.VTeamMembershipWithPermissionsEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;


public class VTeamMembershipWithPermissionsFilter implements Filter<VTeamMembershipWithPermissionsEntity> {
    private Integer id;
    private Integer teamId;
    private String userId;
    private Boolean teamPermissionEdit;

    public static VTeamMembershipWithPermissionsFilter create() {
        return new VTeamMembershipWithPermissionsFilter();
    }

    @Override
    public Specification<VTeamMembershipWithPermissionsEntity> build() {
        return SpecificationBuilder
                .create(VTeamMembershipWithPermissionsEntity.class)
                .withEquals("id", id)
                .withEquals("teamId", teamId)
                .withEquals("userId", userId)
                .withEquals("teamPermissionEdit", teamPermissionEdit)
                .build();
    }

    public Integer getId() {
        return id;
    }

    public VTeamMembershipWithPermissionsFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public VTeamMembershipWithPermissionsFilter setTeamId(Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VTeamMembershipWithPermissionsFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getTeamPermissionEdit() {
        return teamPermissionEdit;
    }

    public VTeamMembershipWithPermissionsFilter setTeamPermissionEdit(Boolean teamPermissionEdit) {
        this.teamPermissionEdit = teamPermissionEdit;
        return this;
    }
}
