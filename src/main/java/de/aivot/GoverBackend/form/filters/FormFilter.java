package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class FormFilter implements Filter<FormEntity> {
    private Integer id;
    private String slug;
    private String internalTitle;
    private Integer developingDepartmentId;
    private Integer developingDepartmentIdNot;
    private Integer publishedVersion;
    private Integer draftedVersion;

    private Boolean isDrafted;
    private Boolean isPublished;
    private Boolean isRevoked;

    public static FormFilter create() {
        return new FormFilter();
    }

    @Nonnull
    @Override
    public Specification<FormEntity> build() {
        var builder = SpecificationBuilder
                .create(FormEntity.class)
                .withEquals("id", id)
                .withContains("slug", slug)
                .withContains("internalTitle", internalTitle)
                .withEquals("developingDepartmentId", developingDepartmentId)
                .withNotEquals("developingDepartmentId", developingDepartmentIdNot)
                .withEquals("publishedVersion", publishedVersion)
                .withEquals("draftedVersion", draftedVersion);

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

    public Integer getId() {
        return id;
    }

    public FormFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public FormFilter setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public FormFilter setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public FormFilter setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public FormFilter setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public FormFilter setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public Boolean getIsDrafted() {
        return isDrafted;
    }

    public FormFilter setIsDrafted(Boolean drafted) {
        isDrafted = drafted;
        return this;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public FormFilter setIsPublished(Boolean published) {
        isPublished = published;
        return this;
    }

    public Boolean getIsRevoked() {
        return isRevoked;
    }

    public FormFilter setIsRevoked(Boolean revoked) {
        isRevoked = revoked;
        return this;
    }

    public Integer getDevelopingDepartmentIdNot() {
        return developingDepartmentIdNot;
    }

    public FormFilter setDevelopingDepartmentIdNot(Integer developingDepartmentIdNot) {
        this.developingDepartmentIdNot = developingDepartmentIdNot;
        return this;
    }

    public Boolean getDrafted() {
        return isDrafted;
    }

    public FormFilter setDrafted(Boolean drafted) {
        isDrafted = drafted;
        return this;
    }

    public Boolean getPublished() {
        return isPublished;
    }

    public FormFilter setPublished(Boolean published) {
        isPublished = published;
        return this;
    }

    public Boolean getRevoked() {
        return isRevoked;
    }

    public FormFilter setRevoked(Boolean revoked) {
        isRevoked = revoked;
        return this;
    }
}
