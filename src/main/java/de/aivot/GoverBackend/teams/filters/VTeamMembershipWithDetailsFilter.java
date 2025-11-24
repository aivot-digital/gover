package de.aivot.GoverBackend.teams.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.teams.entities.VTeamMembershipWithDetailsEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;


public class VTeamMembershipWithDetailsFilter implements Filter<VTeamMembershipWithDetailsEntity> {
    private List<Integer> teamIds;
    private Integer teamId;
    private String teamName;
    private String userId;
    private List<String> userIds;
    private String userFullName;
    private String userEmail;

    private Boolean userEnabled;
    private Boolean userVerified;
    private Boolean userGlobalAdmin;
    private Boolean userDeletedInIdp;

    public static VTeamMembershipWithDetailsFilter create() {
        return new VTeamMembershipWithDetailsFilter();
    }

    @Override
    public Specification<VTeamMembershipWithDetailsEntity> build() {
        var builder = SpecificationBuilder
                .create(VTeamMembershipWithDetailsEntity.class)
                .withEquals("teamId", teamId)
                .withInList("teamId", teamIds)
                .withContains("teamName", teamName)
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

    public String getTeamName() {
        return teamName;
    }

    public VTeamMembershipWithDetailsFilter setTeamName(String teamName) {
        this.teamName = teamName;
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

    public String getUserFullName() {
        return userFullName;
    }

    public VTeamMembershipWithDetailsFilter setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public VTeamMembershipWithDetailsFilter setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public VTeamMembershipWithDetailsFilter setUserEnabled(Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    public Boolean getUserVerified() {
        return userVerified;
    }

    public VTeamMembershipWithDetailsFilter setUserVerified(Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    public Boolean getUserGlobalAdmin() {
        return userGlobalAdmin;
    }

    public VTeamMembershipWithDetailsFilter setUserGlobalAdmin(Boolean userGlobalAdmin) {
        this.userGlobalAdmin = userGlobalAdmin;
        return this;
    }

    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public VTeamMembershipWithDetailsFilter setUserDeletedInIdp(Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }
}
