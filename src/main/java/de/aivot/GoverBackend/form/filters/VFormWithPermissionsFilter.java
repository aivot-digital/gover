package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.validation.Valid;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class VFormWithPermissionsFilter implements Filter<VFormWithPermissionsEntity> {
    private Integer id;
    private String slug;
    private String internalTitle;
    private Integer developingDepartmentId;
    private Integer developingDepartmentIdNot;
    private Integer publishedVersion;
    private Integer draftedVersion;

    private String userId;

    private Boolean isDrafted;
    private Boolean isPublished;
    private Boolean isRevoked;

    private Boolean formPermissionCreate;
    private Boolean formPermissionRead;
    private Boolean formPermissionEdit;
    private Boolean formPermissionDelete;
    private Boolean formPermissionAnnotate;
    private Boolean formPermissionPublish;

    public static VFormWithPermissionsFilter create() {
        return new VFormWithPermissionsFilter();
    }

    public static VFormWithPermissionsFilter from(@Valid FormFilter filter) {
        return new VFormWithPermissionsFilter()
                .setId(filter.getId())
                .setSlug(filter.getSlug())
                .setInternalTitle(filter.getInternalTitle())
                .setDevelopingDepartmentId(filter.getDevelopingDepartmentId())
                .setPublishedVersion(filter.getPublishedVersion())
                .setDraftedVersion(filter.getDraftedVersion())
                .setIsDrafted(filter.getIsDrafted())
                .setIsPublished(filter.getIsPublished())
                .setIsRevoked(filter.getIsRevoked());
    }

    @Nonnull
    @Override
    public Specification<VFormWithPermissionsEntity> build() {
        var builder = SpecificationBuilder
                .create(VFormWithPermissionsEntity.class)
                .withEquals("id", id)
                .withContains("slug", slug)
                .withContains("internalTitle", internalTitle)
                .withEquals("developingDepartmentId", developingDepartmentId)
                .withNotEquals("developingDepartmentId", developingDepartmentIdNot)
                .withEquals("publishedVersion", publishedVersion)
                .withEquals("draftedVersion", draftedVersion)
                .withEquals("userId", userId)
                .withEquals("formPermissionCreate", formPermissionCreate)
                .withEquals("formPermissionRead", formPermissionRead)
                .withEquals("formPermissionEdit", formPermissionEdit)
                .withEquals("formPermissionDelete", formPermissionDelete)
                .withEquals("formPermissionAnnotate", formPermissionAnnotate)
                .withEquals("formPermissionPublish", formPermissionPublish);

        if (Boolean.TRUE.equals(isDrafted)) {
            builder.withNotNull("draftedVersion");
        }

        if (Boolean.TRUE.equals(isPublished)) {
            builder.withNotNull("publishedVersion");
        }

        if (Boolean.TRUE.equals(isRevoked)) {
            builder.withNull("draftedVersion");
            builder.withNull("publishedVersion");
        }

        return builder.build();
    }

    public Filter<FormEntity> asFormFilter() {
        return new FormFilter()
                .setId(this.id)
                .setSlug(this.slug)
                .setInternalTitle(this.internalTitle)
                .setDevelopingDepartmentId(this.developingDepartmentId)
                .setPublishedVersion(this.publishedVersion)
                .setDraftedVersion(this.draftedVersion)
                .setIsDrafted(this.isDrafted)
                .setIsPublished(this.isPublished)
                .setIsRevoked(this.isRevoked);
    }

    public Integer getId() {
        return id;
    }

    public VFormWithPermissionsFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public VFormWithPermissionsFilter setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public VFormWithPermissionsFilter setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public VFormWithPermissionsFilter setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public VFormWithPermissionsFilter setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public VFormWithPermissionsFilter setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VFormWithPermissionsFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getIsDrafted() {
        return isDrafted;
    }

    public VFormWithPermissionsFilter setIsDrafted(Boolean drafted) {
        isDrafted = drafted;
        return this;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public VFormWithPermissionsFilter setIsPublished(Boolean published) {
        isPublished = published;
        return this;
    }

    public Boolean getIsRevoked() {
        return isRevoked;
    }

    public VFormWithPermissionsFilter setIsRevoked(Boolean revoked) {
        isRevoked = revoked;
        return this;
    }

    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public VFormWithPermissionsFilter setFormPermissionCreate(Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public VFormWithPermissionsFilter setFormPermissionRead(Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public VFormWithPermissionsFilter setFormPermissionEdit(Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public VFormWithPermissionsFilter setFormPermissionDelete(Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public VFormWithPermissionsFilter setFormPermissionAnnotate(Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public VFormWithPermissionsFilter setFormPermissionPublish(Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }

    public Integer getDevelopingDepartmentIdNot() {
        return developingDepartmentIdNot;
    }

    public VFormWithPermissionsFilter setDevelopingDepartmentIdNot(Integer developingDepartmentIdNot) {
        this.developingDepartmentIdNot = developingDepartmentIdNot;
        return this;
    }
}
