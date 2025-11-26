package de.aivot.GoverBackend.form.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "v_forms_with_permissions")
public class VFormWithPermissionEntity {
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
    private String userId;
    private Boolean formPermissionCreate;
    private Boolean formPermissionRead;
    private Boolean formPermissionEdit;
    private Boolean formPermissionDelete;
    private Boolean formPermissionAnnotate;
    private Boolean formPermissionPublish;

    public Integer getId() {
        return id;
    }

    public VFormWithPermissionEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public VFormWithPermissionEntity setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public VFormWithPermissionEntity setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public VFormWithPermissionEntity setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public VFormWithPermissionEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public VFormWithPermissionEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public VFormWithPermissionEntity setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public VFormWithPermissionEntity setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public Integer getVersionCount() {
        return versionCount;
    }

    public VFormWithPermissionEntity setVersionCount(Integer versionCount) {
        this.versionCount = versionCount;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VFormWithPermissionEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public VFormWithPermissionEntity setFormPermissionCreate(Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public VFormWithPermissionEntity setFormPermissionRead(Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public VFormWithPermissionEntity setFormPermissionEdit(Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public VFormWithPermissionEntity setFormPermissionDelete(Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public VFormWithPermissionEntity setFormPermissionAnnotate(Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public VFormWithPermissionEntity setFormPermissionPublish(Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }
}
