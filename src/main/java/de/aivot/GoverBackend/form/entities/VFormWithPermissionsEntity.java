package de.aivot.GoverBackend.form.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "v_forms_with_permissions")
@IdClass(VFormWithPermissionsEntityId.class)
public class VFormWithPermissionsEntity {
    @Id
    private Integer id;
    private String slug;
    private String internalTitle;
    private Integer developingDepartmentId;
    private LocalDateTime created;
    private LocalDateTime updated;
    @Column(columnDefinition = "int2")
    private Integer publishedVersion;
    @Column(columnDefinition = "int2")
    private Integer draftedVersion;
    private Integer versionCount;
    @Id
    private String userId;
    private Boolean formPermissionCreate;
    private Boolean formPermissionRead;
    private Boolean formPermissionEdit;
    private Boolean formPermissionDelete;
    private Boolean formPermissionAnnotate;
    private Boolean formPermissionPublish;

    // Default constructor for JPA
    public VFormWithPermissionsEntity() {

    }

    // Full constructor
    public VFormWithPermissionsEntity(Integer id,
                                      String slug,
                                      String internalTitle,
                                      Integer developingDepartmentId,
                                      LocalDateTime created,
                                      LocalDateTime updated,
                                      Integer publishedVersion,
                                      Integer draftedVersion,
                                      Integer versionCount,
                                      String userId,
                                      Boolean formPermissionCreate,
                                      Boolean formPermissionRead,
                                      Boolean formPermissionEdit,
                                      Boolean formPermissionDelete,
                                      Boolean formPermissionAnnotate,
                                      Boolean formPermissionPublish) {
        this.id = id;
        this.slug = slug;
        this.internalTitle = internalTitle;
        this.developingDepartmentId = developingDepartmentId;
        this.created = created;
        this.updated = updated;
        this.publishedVersion = publishedVersion;
        this.draftedVersion = draftedVersion;
        this.versionCount = versionCount;
        this.userId = userId;
        this.formPermissionCreate = formPermissionCreate;
        this.formPermissionRead = formPermissionRead;
        this.formPermissionEdit = formPermissionEdit;
        this.formPermissionDelete = formPermissionDelete;
        this.formPermissionAnnotate = formPermissionAnnotate;
        this.formPermissionPublish = formPermissionPublish;
    }

    public FormEntity toFormEntity() {
        return new FormEntity(
                this.id,
                this.slug,
                this.internalTitle,
                this.developingDepartmentId,
                this.created,
                this.updated,
                this.publishedVersion,
                this.draftedVersion,
                this.versionCount
        );
    }

    public Integer getId() {
        return id;
    }

    public VFormWithPermissionsEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public VFormWithPermissionsEntity setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public VFormWithPermissionsEntity setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public VFormWithPermissionsEntity setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public VFormWithPermissionsEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public VFormWithPermissionsEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public VFormWithPermissionsEntity setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public VFormWithPermissionsEntity setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public Integer getVersionCount() {
        return versionCount;
    }

    public VFormWithPermissionsEntity setVersionCount(Integer versionCount) {
        this.versionCount = versionCount;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VFormWithPermissionsEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public VFormWithPermissionsEntity setFormPermissionCreate(Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public VFormWithPermissionsEntity setFormPermissionRead(Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public VFormWithPermissionsEntity setFormPermissionEdit(Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public VFormWithPermissionsEntity setFormPermissionDelete(Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public VFormWithPermissionsEntity setFormPermissionAnnotate(Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public VFormWithPermissionsEntity setFormPermissionPublish(Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }
}
