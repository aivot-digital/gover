package de.aivot.GoverBackend.form.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "form_with_memberships")
@IdClass(FormWithMembershipEntityId.class)
public class FormWithMembershipEntity {
    @Id
    private Integer id;
    private String slug;
    private String internalTitle;
    private Integer developingDepartmentId;
    private Integer managingDepartmentId;
    private Integer responsibleDepartmentId;
    private LocalDateTime created;
    private LocalDateTime updated;
    @Column(columnDefinition = "int2")
    private Integer publishedVersion;
    @Column(columnDefinition = "int2")
    private Integer draftedVersion;
    private Integer versionCount;
    @Id
    private String userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private String userFullName;
    private Boolean userEnabled;
    private Boolean userVerified;
    private Boolean userGlobalAdmin;
    private Boolean userDeletedInIdp;
    private Boolean userIsDeveloper;
    private Boolean userIsManager;
    private Boolean userIsResponsible;

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public FormWithMembershipEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public FormWithMembershipEntity setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public FormWithMembershipEntity setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public FormWithMembershipEntity setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public FormWithMembershipEntity setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public FormWithMembershipEntity setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public FormWithMembershipEntity setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public FormWithMembershipEntity setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public FormWithMembershipEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public FormWithMembershipEntity setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public FormWithMembershipEntity setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
        return this;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public FormWithMembershipEntity setUserLastName(String userLastName) {
        this.userLastName = userLastName;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public FormWithMembershipEntity setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public FormWithMembershipEntity setUserEnabled(Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    public Boolean getUserVerified() {
        return userVerified;
    }

    public FormWithMembershipEntity setUserVerified(Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    public Boolean getUserGlobalAdmin() {
        return userGlobalAdmin;
    }

    public FormWithMembershipEntity setUserGlobalAdmin(Boolean userGlobalAdmin) {
        this.userGlobalAdmin = userGlobalAdmin;
        return this;
    }

    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public FormWithMembershipEntity setUserDeletedInIdp(Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }

    public Boolean getUserIsDeveloper() {
        return userIsDeveloper;
    }

    public FormWithMembershipEntity setUserIsDeveloper(Boolean userIsDeveloper) {
        this.userIsDeveloper = userIsDeveloper;
        return this;
    }

    public Boolean getUserIsManager() {
        return userIsManager;
    }

    public FormWithMembershipEntity setUserIsManager(Boolean userIsManager) {
        this.userIsManager = userIsManager;
        return this;
    }

    public Boolean getUserIsResponsible() {
        return userIsResponsible;
    }

    public FormWithMembershipEntity setUserIsResponsible(Boolean userIsResponsible) {
        this.userIsResponsible = userIsResponsible;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public FormWithMembershipEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public FormWithMembershipEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public Integer getVersionCount() {
        return versionCount;
    }

    public FormWithMembershipEntity setVersionCount(Integer versionCount) {
        this.versionCount = versionCount;
        return this;
    }


    // endregion
}
