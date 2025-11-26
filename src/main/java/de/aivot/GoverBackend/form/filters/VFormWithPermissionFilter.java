package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.entities.VFormWithPermissionEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class VFormWithPermissionFilter implements Filter<VFormWithPermissionEntity> {
    private Integer id;
    private String slug;
    private String internalTitle;
    private String publicTitle;
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

    public static VFormWithPermissionFilter create() {
        return new VFormWithPermissionFilter();
    }

    @Nonnull
    @Override
    public Specification<VFormWithPermissionEntity> build() {
        var builder = SpecificationBuilder
                .create(VFormWithPermissionEntity.class)
                .withEquals("formId", id)
                .withContains("formSlug", slug)
                .withContains("formInternalTitle", internalTitle)
                .withContains("publicTitle", publicTitle)
                .withEquals("formDevelopingOrganizationalUnitId", developingDepartmentId)
                .withNotEquals("formDevelopingOrganizationalUnitId", developingDepartmentIdNot)
                .withEquals("formPublishedVersion", publishedVersion)
                .withEquals("formDraftedVersion", draftedVersion)
                .withEquals("userId", userId)
                .withEquals("formPermissionCreate", formPermissionCreate)
                .withEquals("formPermissionRead", formPermissionRead)
                .withEquals("formPermissionEdit", formPermissionEdit)
                .withEquals("formPermissionDelete", formPermissionDelete)
                .withEquals("formPermissionAnnotate", formPermissionAnnotate)
                .withEquals("formPermissionPublish", formPermissionPublish);

        if (Boolean.TRUE.equals(isDrafted)) {
            builder.withNotNull("formDraftedVersion");
        }

        if (Boolean.TRUE.equals(isPublished)) {
            builder.withNotNull("formPublishedVersion");
        }

        if (Boolean.TRUE.equals(isRevoked)) {
            builder.withNull("formDraftedVersion");
            builder.withNull("formPublishedVersion");
        }

        return builder.build();
    }

    public Integer getId() {
        return id;
    }

    public VFormWithPermissionFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public VFormWithPermissionFilter setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public VFormWithPermissionFilter setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public String getPublicTitle() {
        return publicTitle;
    }

    public VFormWithPermissionFilter setPublicTitle(String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public VFormWithPermissionFilter setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public VFormWithPermissionFilter setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public VFormWithPermissionFilter setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VFormWithPermissionFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getDrafted() {
        return isDrafted;
    }

    public VFormWithPermissionFilter setDrafted(Boolean drafted) {
        isDrafted = drafted;
        return this;
    }

    public Boolean getPublished() {
        return isPublished;
    }

    public VFormWithPermissionFilter setPublished(Boolean published) {
        isPublished = published;
        return this;
    }

    public Boolean getRevoked() {
        return isRevoked;
    }

    public VFormWithPermissionFilter setRevoked(Boolean revoked) {
        isRevoked = revoked;
        return this;
    }

    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public VFormWithPermissionFilter setFormPermissionCreate(Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public VFormWithPermissionFilter setFormPermissionRead(Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public VFormWithPermissionFilter setFormPermissionEdit(Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public VFormWithPermissionFilter setFormPermissionDelete(Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public VFormWithPermissionFilter setFormPermissionAnnotate(Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public VFormWithPermissionFilter setFormPermissionPublish(Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }

    public Integer getDevelopingDepartmentIdNot() {
        return developingDepartmentIdNot;
    }

    public VFormWithPermissionFilter setDevelopingDepartmentIdNot(Integer developingDepartmentIdNot) {
        this.developingDepartmentIdNot = developingDepartmentIdNot;
        return this;
    }
}
