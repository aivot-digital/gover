package de.aivot.GoverBackend.teams.entities;

import de.aivot.GoverBackend.core.converters.JsonArrayConverter;
import jakarta.persistence.*;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "v_team_memberships_with_details")
public class VTeamMembershipWithDetailsEntity {
    @Id
    private Integer membershipId;
    private Boolean membershipHasDeputies;
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonArrayConverter.class)
    private List<Map<String, Object>> membershipDeputies;
    private String userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private String userFullName;
    private Boolean userEnabled;
    private Boolean userVerified;
    private Boolean userDeletedInIdp;
    private Integer userSystemRoleId;
    private Integer teamId;
    private String teamName;
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonArrayConverter.class)
    private List<Map<String, Object>> domainRoles;
    private List<String> domainRolePermissions;

    public Integer getMembershipId() {
        return membershipId;
    }

    public VTeamMembershipWithDetailsEntity setMembershipId(Integer membershipId) {
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

    public String getUserId() {
        return userId;
    }

    public VTeamMembershipWithDetailsEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public VTeamMembershipWithDetailsEntity setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public VTeamMembershipWithDetailsEntity setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
        return this;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public VTeamMembershipWithDetailsEntity setUserLastName(String userLastName) {
        this.userLastName = userLastName;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public VTeamMembershipWithDetailsEntity setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public VTeamMembershipWithDetailsEntity setUserEnabled(Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    public Boolean getUserVerified() {
        return userVerified;
    }

    public VTeamMembershipWithDetailsEntity setUserVerified(Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public VTeamMembershipWithDetailsEntity setUserDeletedInIdp(Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }

    public Integer getUserSystemRoleId() {
        return userSystemRoleId;
    }

    public VTeamMembershipWithDetailsEntity setUserSystemRoleId(Integer userSystemRoleId) {
        this.userSystemRoleId = userSystemRoleId;
        return this;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public VTeamMembershipWithDetailsEntity setTeamId(Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    public String getTeamName() {
        return teamName;
    }

    public VTeamMembershipWithDetailsEntity setTeamName(String teamName) {
        this.teamName = teamName;
        return this;
    }

    public List<Map<String, Object>> getDomainRoles() {
        return domainRoles;
    }

    public VTeamMembershipWithDetailsEntity setDomainRoles(List<Map<String, Object>> domainRoles) {
        this.domainRoles = domainRoles;
        return this;
    }

    public List<String> getDomainRolePermissions() {
        return domainRolePermissions;
    }

    public VTeamMembershipWithDetailsEntity setDomainRolePermissions(List<String> domainRolePermissions) {
        this.domainRolePermissions = domainRolePermissions;
        return this;
    }
}
