package de.aivot.GoverBackend.teams.entities;

import de.aivot.GoverBackend.core.converters.JsonArrayConverter;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "v_team_memberships_with_details")
public class VTeamMembershipWithDetailsEntity {
    @Id
    @Nonnull
    private Integer membershipId;

    private Boolean membershipHasDeputies;
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonArrayConverter.class)
    private List<Map<String, Object>> membershipDeputies;

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

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonArrayConverter.class)
    private List<Map<String, Object>> domainRoles;
    @Nonnull
    private List<String> domainRolePermissions;

    // region Getters and Setters

    @Nonnull
    public Integer getMembershipId() {
        return membershipId;
    }

    public VTeamMembershipWithDetailsEntity setMembershipId(@Nonnull Integer membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    public Boolean getMembershipHasDeputies() {
        return membershipHasDeputies;
    }

    public VTeamMembershipWithDetailsEntity setMembershipHasDeputies(Boolean membershipHasDeputies) {
        this.membershipHasDeputies = membershipHasDeputies;
        return this;
    }

    public List<Map<String, Object>> getMembershipDeputies() {
        return membershipDeputies;
    }

    public VTeamMembershipWithDetailsEntity setMembershipDeputies(List<Map<String, Object>> membershipDeputies) {
        this.membershipDeputies = membershipDeputies;
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
    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public VTeamMembershipWithDetailsEntity setUserDeletedInIdp(@Nonnull Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }

    public List<Map<String, Object>> getDomainRoles() {
        return domainRoles;
    }

    public VTeamMembershipWithDetailsEntity setDomainRoles(List<Map<String, Object>> domainRoles) {
        this.domainRoles = domainRoles;
        return this;
    }

    @Nonnull
    public List<String> getDomainRolePermissions() {
        return domainRolePermissions;
    }

    public VTeamMembershipWithDetailsEntity setDomainRolePermissions(@Nonnull List<String> domainRolePermissions) {
        this.domainRolePermissions = domainRolePermissions;
        return this;
    }


    // endregion
}
