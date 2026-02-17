package de.aivot.GoverBackend.teams.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.teams.entities.VTeamMembershipWithDetailsEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;


public class VTeamMembershipWithDetailsFilter implements Filter<VTeamMembershipWithDetailsEntity> {
    private List<Integer> teamIds;
    private Integer teamId;
    private String name;
    private String userId;
    private List<String> userIds;
    private String fullName;
    private String email;
    private Boolean enabled;
    private Boolean verified;
    private Boolean deletedInIdp;

    public static VTeamMembershipWithDetailsFilter create() {
        return new VTeamMembershipWithDetailsFilter();
    }

    @Override
    public Specification<VTeamMembershipWithDetailsEntity> build() {
        var builder = SpecificationBuilder
                .create(VTeamMembershipWithDetailsEntity.class)
                .withEquals("teamId", teamId)
                .withInList("teamId", teamIds)
                .withContains("teamName", name)
                .withEquals("userId", userId)
                .withInList("userId", userIds)
                .withContains("userFullName", fullName)
                .withContains("userEmail", email);

        if (enabled != null) {
            builder = builder
                    .withEquals("userEnabled", enabled);
        }

        if (verified != null) {
            builder = builder
                    .withEquals("userVerified", verified);
        }

        if (deletedInIdp != null) {
            builder = builder
                    .withEquals("userDeletedInIdp", deletedInIdp);
        }

        return builder
                .build();
    }

    public List<Integer> getTeamIds() {
        return teamIds;
    }

    public VTeamMembershipWithDetailsFilter setTeamIds(List<Integer> teamIds) {
        this.teamIds = teamIds;
        return this;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public VTeamMembershipWithDetailsFilter setTeamId(Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    public String getName() {
        return name;
    }

    public VTeamMembershipWithDetailsFilter setName(String name) {
        this.name = name;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VTeamMembershipWithDetailsFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public VTeamMembershipWithDetailsFilter setUserIds(List<String> userIds) {
        this.userIds = userIds;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public VTeamMembershipWithDetailsFilter setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public VTeamMembershipWithDetailsFilter setEmail(String email) {
        this.email = email;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public VTeamMembershipWithDetailsFilter setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getVerified() {
        return verified;
    }

    public VTeamMembershipWithDetailsFilter setVerified(Boolean verified) {
        this.verified = verified;
        return this;
    }

    public Boolean getDeletedInIdp() {
        return deletedInIdp;
    }

    public VTeamMembershipWithDetailsFilter setDeletedInIdp(Boolean deletedInIdp) {
        this.deletedInIdp = deletedInIdp;
        return this;
    }
}
