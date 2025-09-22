package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.entities.FormWithMembershipEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class FormWithMembershipFilter implements Filter<FormWithMembershipEntity> {
    private Integer id;
    private String slug;
    private String internalTitle;
    private String publicTitle;
    private Integer developingDepartmentId;
    private Integer managingDepartmentId;
    private Integer responsibleDepartmentId;
    private Integer publishedVersion;
    private Integer draftedVersion;
    private String userId;
    private Boolean isDeveloper;
    private Boolean isManager;
    private Boolean isResponsible;
    private Boolean isDrafted;
    private Boolean isPublished;
    private Boolean isRevoked;

    public static FormWithMembershipFilter create() {
        return new FormWithMembershipFilter();
    }

    @Nonnull
    @Override
    public Specification<FormWithMembershipEntity> build() {
        var builder = SpecificationBuilder
                .create(FormWithMembershipEntity.class)
                .withEquals("id", id)
                .withContains("slug", slug)
                .withContains("internalTitle", internalTitle)
                .withContains("publicTitle", publicTitle)
                .withEquals("developingDepartmentId", developingDepartmentId)
                .withEquals("managingDepartmentId", managingDepartmentId)
                .withEquals("responsibleDepartmentId", responsibleDepartmentId)
                .withEquals("publishedVersion", publishedVersion)
                .withEquals("draftedVersion", draftedVersion)
                .withEquals("userId", userId)
                .withEquals("userIsDeveloper", isDeveloper)
                .withEquals("userIsManager", isManager)
                .withEquals("userIsResponsible", isResponsible);

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

    public FormWithMembershipFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public FormWithMembershipFilter setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public FormWithMembershipFilter setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public String getPublicTitle() {
        return publicTitle;
    }

    public FormWithMembershipFilter setPublicTitle(String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public FormWithMembershipFilter setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public FormWithMembershipFilter setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public FormWithMembershipFilter setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public FormWithMembershipFilter setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public FormWithMembershipFilter setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public FormWithMembershipFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getIsDeveloper() {
        return isDeveloper;
    }

    public FormWithMembershipFilter setIsDeveloper(Boolean developer) {
        isDeveloper = developer;
        return this;
    }

    public Boolean getIsManager() {
        return isManager;
    }

    public FormWithMembershipFilter setIsManager(Boolean manager) {
        isManager = manager;
        return this;
    }

    public Boolean getIsResponsible() {
        return isResponsible;
    }

    public FormWithMembershipFilter setIsResponsible(Boolean responsible) {
        isResponsible = responsible;
        return this;
    }

    public Boolean getIsDrafted() {
        return isDrafted;
    }

    public FormWithMembershipFilter setIsDrafted(Boolean drafted) {
        isDrafted = drafted;
        return this;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public FormWithMembershipFilter setIsPublished(Boolean published) {
        isPublished = published;
        return this;
    }

    public Boolean getIsRevoked() {
        return isRevoked;
    }

    public FormWithMembershipFilter setIsRevoked(Boolean revoked) {
        isRevoked = revoked;
        return this;
    }



}
