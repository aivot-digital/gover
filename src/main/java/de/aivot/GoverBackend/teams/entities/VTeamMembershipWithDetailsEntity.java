package de.aivot.GoverBackend.teams.entities;

import de.aivot.GoverBackend.core.converters.JsonArrayConverter;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "v_team_memberships_with_details")
public class VTeamMembershipWithDetailsEntity {
    private Integer membershipId;

    private Boolean membershipIsDeputy;
    private String membershipAsDeputyForUserId;
    private String membershipAsDeputyForUserEmail;
    private String membershipAsDeputyForUserFirstName;
    private String membershipAsDeputyForUserLastName;
    private String membershipAsDeputyForUserFullName;
    private Boolean membershipAsDeputyForUserEnabled;
    private Boolean membershipAsDeputyForUserVerified;
    private Boolean membershipAsDeputyForUserDeletedInIdp;
    private Integer membershipAsDeputyForUserSystemRoleId;

    @Nonnull
    private Integer teamId;
    @Nonnull
    private String teamName;

    @Id
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

    public Integer getMembershipId() {
        return membershipId;
    }

    public VTeamMembershipWithDetailsEntity setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    public Boolean getMembershipIsDeputy() {
        return membershipIsDeputy;
    }

    public VTeamMembershipWithDetailsEntity setMembershipIsDeputy(Boolean membershipIsDeputy) {
        this.membershipIsDeputy = membershipIsDeputy;
        return this;
    }

    public String getMembershipAsDeputyForUserId() {
        return membershipAsDeputyForUserId;
    }

    public VTeamMembershipWithDetailsEntity setMembershipAsDeputyForUserId(String membershipAsDeputyForUserId) {
        this.membershipAsDeputyForUserId = membershipAsDeputyForUserId;
        return this;
    }

    public String getMembershipAsDeputyForUserEmail() {
        return membershipAsDeputyForUserEmail;
    }

    public VTeamMembershipWithDetailsEntity setMembershipAsDeputyForUserEmail(String membershipAsDeputyForUserEmail) {
        this.membershipAsDeputyForUserEmail = membershipAsDeputyForUserEmail;
        return this;
    }

    public String getMembershipAsDeputyForUserFirstName() {
        return membershipAsDeputyForUserFirstName;
    }

    public VTeamMembershipWithDetailsEntity setMembershipAsDeputyForUserFirstName(String membershipAsDeputyForUserFirstName) {
        this.membershipAsDeputyForUserFirstName = membershipAsDeputyForUserFirstName;
        return this;
    }

    public String getMembershipAsDeputyForUserLastName() {
        return membershipAsDeputyForUserLastName;
    }

    public VTeamMembershipWithDetailsEntity setMembershipAsDeputyForUserLastName(String membershipAsDeputyForUserLastName) {
        this.membershipAsDeputyForUserLastName = membershipAsDeputyForUserLastName;
        return this;
    }

    public String getMembershipAsDeputyForUserFullName() {
        return membershipAsDeputyForUserFullName;
    }

    public VTeamMembershipWithDetailsEntity setMembershipAsDeputyForUserFullName(String membershipAsDeputyForUserFullName) {
        this.membershipAsDeputyForUserFullName = membershipAsDeputyForUserFullName;
        return this;
    }

    public Boolean getMembershipAsDeputyForUserEnabled() {
        return membershipAsDeputyForUserEnabled;
    }

    public VTeamMembershipWithDetailsEntity setMembershipAsDeputyForUserEnabled(Boolean membershipAsDeputyForUserEnabled) {
        this.membershipAsDeputyForUserEnabled = membershipAsDeputyForUserEnabled;
        return this;
    }

    public Boolean getMembershipAsDeputyForUserVerified() {
        return membershipAsDeputyForUserVerified;
    }

    public VTeamMembershipWithDetailsEntity setMembershipAsDeputyForUserVerified(Boolean membershipAsDeputyForUserVerified) {
        this.membershipAsDeputyForUserVerified = membershipAsDeputyForUserVerified;
        return this;
    }

    public Boolean getMembershipAsDeputyForUserDeletedInIdp() {
        return membershipAsDeputyForUserDeletedInIdp;
    }

    public VTeamMembershipWithDetailsEntity setMembershipAsDeputyForUserDeletedInIdp(Boolean membershipAsDeputyForUserDeletedInIdp) {
        this.membershipAsDeputyForUserDeletedInIdp = membershipAsDeputyForUserDeletedInIdp;
        return this;
    }

    public Integer getMembershipAsDeputyForUserSystemRoleId() {
        return membershipAsDeputyForUserSystemRoleId;
    }

    public VTeamMembershipWithDetailsEntity setMembershipAsDeputyForUserSystemRoleId(Integer membershipAsDeputyForUserSystemRoleId) {
        this.membershipAsDeputyForUserSystemRoleId = membershipAsDeputyForUserSystemRoleId;
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
}
