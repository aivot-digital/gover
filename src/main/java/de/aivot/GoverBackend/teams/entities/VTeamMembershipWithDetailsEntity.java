package de.aivot.GoverBackend.teams.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "v_team_memberships_with_details")
public class VTeamMembershipWithDetailsEntity {
    @Id
    private Integer id;

    @Nonnull
    private Integer teamId;
    @Nonnull
    private String name;

    @Nonnull
    private LocalDateTime created;
    @Nonnull
    private LocalDateTime updated;

    @Nonnull
    private String userId;
    @Nonnull
    private String firstName;
    @Nonnull
    private String lastName;
    @Nonnull
    private String fullName;
    @Nonnull
    private String email;
    @Nonnull
    private Boolean enabled;
    @Nonnull
    private Boolean verified;
    @Nonnull
    private Integer globalRole;
    @Nonnull
    private Boolean deletedInIdp;

    public Integer getId() {
        return id;
    }

    public VTeamMembershipWithDetailsEntity setId(Integer membershipId) {
        this.id = membershipId;
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
    public String getName() {
        return name;
    }

    public VTeamMembershipWithDetailsEntity setName(@Nonnull String teamName) {
        this.name = teamName;
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
    public String getFirstName() {
        return firstName;
    }

    public VTeamMembershipWithDetailsEntity setFirstName(@Nonnull String userFirstName) {
        this.firstName = userFirstName;
        return this;
    }

    @Nonnull
    public String getLastName() {
        return lastName;
    }

    public VTeamMembershipWithDetailsEntity setLastName(@Nonnull String userLastName) {
        this.lastName = userLastName;
        return this;
    }

    @Nonnull
    public String getFullName() {
        return fullName;
    }

    public VTeamMembershipWithDetailsEntity setFullName(@Nonnull String userFullName) {
        this.fullName = userFullName;
        return this;
    }

    @Nonnull
    public String getEmail() {
        return email;
    }

    public VTeamMembershipWithDetailsEntity setEmail(@Nonnull String userEmail) {
        this.email = userEmail;
        return this;
    }

    @Nonnull
    public Boolean getEnabled() {
        return enabled;
    }

    public VTeamMembershipWithDetailsEntity setEnabled(@Nonnull Boolean userEnabled) {
        this.enabled = userEnabled;
        return this;
    }

    @Nonnull
    public Boolean getVerified() {
        return verified;
    }

    public VTeamMembershipWithDetailsEntity setVerified(@Nonnull Boolean userVerified) {
        this.verified = userVerified;
        return this;
    }

    @Nonnull
    public Integer getGlobalRole() {
        return globalRole;
    }

    public VTeamMembershipWithDetailsEntity setGlobalRole(@Nonnull Integer userGlobalAdmin) {
        this.globalRole = userGlobalAdmin;
        return this;
    }

    @Nonnull
    public Boolean getDeletedInIdp() {
        return deletedInIdp;
    }

    public VTeamMembershipWithDetailsEntity setDeletedInIdp(@Nonnull Boolean userDeletedInIdp) {
        this.deletedInIdp = userDeletedInIdp;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public VTeamMembershipWithDetailsEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public VTeamMembershipWithDetailsEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }
}
