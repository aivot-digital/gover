package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class FormFilter implements Filter<FormEntity> {
    private Integer id;
    private String slug;
    private String internalTitle;
    private String publicTitle;
    private Integer developingDepartmentId;
    private Integer managingDepartmentId;
    private Integer responsibleDepartmentId;
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
                .withContains("publicTitle", publicTitle)
                .withEquals("developingDepartmentId", developingDepartmentId)
                .withEquals("managingDepartmentId", managingDepartmentId)
                .withEquals("responsibleDepartmentId", responsibleDepartmentId)
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

    public String getPublicTitle() {
        return publicTitle;
    }

    public FormFilter setPublicTitle(String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public FormFilter setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public FormFilter setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public FormFilter setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
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
}
