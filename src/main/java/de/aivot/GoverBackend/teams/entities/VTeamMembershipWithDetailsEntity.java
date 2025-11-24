package de.aivot.GoverBackend.teams.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "v_team_memberships_with_details")
public class VTeamMembershipWithDetailsEntity {
    @Id
    private Integer membershipId;

    @Nonnull
    private Integer teamId;
    @Nonnull
    private String teamName;

    @Nonnull
    private String userId;
    @Nonnull
    private String userFirstName;
    @Nonnull
    private String userLastName;
    @Nonnull
    private String userFullName;
    @Nonnull
    private String userEmail;
    @Nonnull
    private Boolean userEnabled;
    @Nonnull
    private Boolean userVerified;
    @Nonnull
    private Boolean userGlobalAdmin;
    @Nonnull
    private Boolean userDeletedInIdp;

    public Integer getMembershipId() {
        return membershipId;
    }

    public VTeamMembershipWithDetailsEntity setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    @Nonnull
    public Integer getTeamId() {
        return teamId;
    }

    public VTeamMembershipWithDetailsEntity setTeamId(@Nonnull Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    @Nonnull
    public String getTeamName() {
        return teamName;
    }

    public VTeamMembershipWithDetailsEntity setTeamName(@Nonnull String teamName) {
        this.teamName = teamName;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VTeamMembershipWithDetailsEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public String getUserFirstName() {
        return userFirstName;
    }

    public VTeamMembershipWithDetailsEntity setUserFirstName(@Nonnull String userFirstName) {
        this.userFirstName = userFirstName;
        return this;
    }

    @Nonnull
    public String getUserLastName() {
        return userLastName;
    }

    public VTeamMembershipWithDetailsEntity setUserLastName(@Nonnull String userLastName) {
        this.userLastName = userLastName;
        return this;
    }

    @Nonnull
    public String getUserFullName() {
        return userFullName;
    }

    public VTeamMembershipWithDetailsEntity setUserFullName(@Nonnull String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    @Nonnull
    public String getUserEmail() {
        return userEmail;
    }

    public VTeamMembershipWithDetailsEntity setUserEmail(@Nonnull String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    @Nonnull
    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public VTeamMembershipWithDetailsEntity setUserEnabled(@Nonnull Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    @Nonnull
    public Boolean getUserVerified() {
        return userVerified;
    }

    public VTeamMembershipWithDetailsEntity setUserVerified(@Nonnull Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    @Nonnull
    public Boolean getUserGlobalAdmin() {
        return userGlobalAdmin;
    }

    public VTeamMembershipWithDetailsEntity setUserGlobalAdmin(@Nonnull Boolean userGlobalAdmin) {
        this.userGlobalAdmin = userGlobalAdmin;
        return this;
    }

    @Nonnull
    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public VTeamMembershipWithDetailsEntity setUserDeletedInIdp(@Nonnull Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }
}
