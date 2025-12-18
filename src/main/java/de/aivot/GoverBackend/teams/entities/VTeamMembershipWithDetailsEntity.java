package de.aivot.GoverBackend.teams.entities;

import jakarta.annotation.Nonnull;
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
    private Boolean userDeletedInIdp;

    public Integer getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
    }

    @Nonnull
    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(@Nonnull Integer teamId) {
        this.teamId = teamId;
    }

    @Nonnull
    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(@Nonnull String teamname) {
        this.teamName = teamname;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@Nonnull String userId) {
        this.userId = userId;
    }

    @Nonnull
    public String getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(@Nonnull String userFirstName) {
        this.userFirstName = userFirstName;
    }

    @Nonnull
    public String getUserLastName() {
        return userLastName;
    }

    public void setUserLastName(@Nonnull String userLastName) {
        this.userLastName = userLastName;
    }

    @Nonnull
    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(@Nonnull String userFullName) {
        this.userFullName = userFullName;
    }

    @Nonnull
    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(@Nonnull String userEmail) {
        this.userEmail = userEmail;
    }

    @Nonnull
    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public void setUserEnabled(@Nonnull Boolean userEnabled) {
        this.userEnabled = userEnabled;
    }

    @Nonnull
    public Boolean getUserVerified() {
        return userVerified;
    }

    public void setUserVerified(@Nonnull Boolean userVerified) {
        this.userVerified = userVerified;
    }

    @Nonnull
    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public void setUserDeletedInIdp(@Nonnull Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
    }
}
