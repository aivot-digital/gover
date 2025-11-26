package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import java.util.UUID;

public class FormVersionWithMembershipFilter implements Filter<FormVersionWithMembershipEntity> {
    private Integer id;
    private String slug;
    private String internalTitle;
    private String publicTitle;
    private Integer developingDepartmentId;
    private Integer managingDepartmentId;
    private Integer responsibleDepartmentId;
    private Integer publishedVersion;
    private Integer draftedVersion;
    private Integer formId;
    private Integer version;
    private FormStatus status;
    private FormType type;
    private Integer legalSupportDepartmentId;
    private Integer technicalSupportDepartmentId;
    private Integer imprintDepartmentId;
    private Integer privacyDepartmentId;
    private Integer accessibilityDepartmentId;
    private Integer destinationId;
    private Integer themeId;
    private UUID pdfTemplateKey;
    private UUID paymentProviderKey;
    private Boolean identityVerificationRequired;
    private UUID identityProviderKey;
    private Boolean isPublished;
    private Boolean isRevoked;
    private Boolean isDrafted;
    private Boolean isCurrentlyPublishedVersion;
    private Boolean isCurrentlyDraftedVersion;
    private String userId;
    private Boolean isDeveloper;
    private Boolean isManager;
    private Boolean isResponsible;

    public static FormVersionWithMembershipFilter create() {
        return new FormVersionWithMembershipFilter();
    }

    @Nonnull
    @Override
    public Specification<FormVersionWithMembershipEntity> build() {
        var builder = SpecificationBuilder
                .create(FormVersionWithMembershipEntity.class)
                .withEquals("id", id)
                .withContains("slug", slug)
                .withContains("internalTitle", internalTitle)
                .withContains("publicTitle", publicTitle)
                .withEquals("developingOrganizationalUnitId", developingDepartmentId)
                .withEquals("managingOrganizationalUnitId", managingDepartmentId)
                .withEquals("responsibleOrganizationalUnitId", responsibleDepartmentId)
                .withEquals("publishedVersion", publishedVersion)
                .withEquals("draftedVersion", draftedVersion)
                .withEquals("formId", formId)
                .withEquals("version", version)
                .withEquals("status", status)
                .withEquals("type", type)
                .withEquals("legalSupportOrganizationalUnitId", legalSupportDepartmentId)
                .withEquals("technicalSupportOrganizationalUnitId", technicalSupportDepartmentId)
                .withEquals("imprintOrganizationalUnitId", imprintDepartmentId)
                .withEquals("privacyOrganizationalUnitId", privacyDepartmentId)
                .withEquals("accessibilityOrganizationalUnitId", accessibilityDepartmentId)
                .withEquals("destinationId", destinationId)
                .withEquals("themeId", themeId)
                .withEquals("pdfTemplateKey", pdfTemplateKey)
                .withEquals("paymentProviderKey", paymentProviderKey)
                .withEquals("identityVerificationRequired", identityVerificationRequired)
                .withJsonArrayElementFieldEquals("identityProviders", "identityProviderKey", identityProviderKey != null ? identityProviderKey.toString() : null)
                .withEquals("isCurrentlyPublishedVersion", isCurrentlyPublishedVersion)
                .withEquals("isCurrentlyDraftedVersion", isCurrentlyDraftedVersion)
                .withEquals("userId", userId)
                .withEquals("userIsDeveloper", isDeveloper)
                .withEquals("userIsManager", isManager)
                .withEquals("userIsResponsible", isResponsible);

        if (isPublished != null && isPublished) {
            builder = builder
                    .withNotNull("published");
        }

        if (isRevoked != null && isRevoked) {
            builder = builder
                    .withNotNull("revoked");
        }

        return builder.build();
    }

    public FormVersionWithDetailsFilter asFormVersionWithDetailsFilter() {
        return FormVersionWithDetailsFilter
                .create()
                .setId(id)
                .setSlug(slug)
                .setInternalTitle(internalTitle)
                .setPublicTitle(publicTitle)
                .setDevelopingDepartmentId(developingDepartmentId)
                .setManagingDepartmentId(managingDepartmentId)
                .setResponsibleDepartmentId(responsibleDepartmentId)
                .setPublishedVersion(publishedVersion)
                .setDraftedVersion(draftedVersion)
                .setFormId(formId)
                .setVersion(version)
                .setType(type)
                .setLegalSupportDepartmentId(legalSupportDepartmentId)
                .setTechnicalSupportDepartmentId(technicalSupportDepartmentId)
                .setImprintDepartmentId(imprintDepartmentId)
                .setPrivacyDepartmentId(privacyDepartmentId)
                .setAccessibilityDepartmentId(accessibilityDepartmentId)
                .setDestinationId(destinationId)
                .setThemeId(themeId)
                .setPdfTemplateKey(pdfTemplateKey);
    }

    public FormFilter asFormFilter() {
        return FormFilter
                .create()
                .setId(id)
                .setSlug(slug)
                .setInternalTitle(internalTitle)
                .setPublicTitle(publicTitle)
                .setDevelopingDepartmentId(developingDepartmentId)
                .setPublishedVersion(publishedVersion)
                .setDraftedVersion(draftedVersion)
                .setIsPublished(isPublished)
                .setIsDrafted(isDrafted)
                .setIsRevoked(isRevoked);
    }

    public FormWithMembershipFilter asFormWithMembershipFilter() {
        return FormWithMembershipFilter
                .create()
                .setId(id)
                .setSlug(slug)
                .setInternalTitle(internalTitle)
                .setPublicTitle(publicTitle)
                .setDevelopingDepartmentId(developingDepartmentId)
                .setManagingDepartmentId(managingDepartmentId)
                .setResponsibleDepartmentId(responsibleDepartmentId)
                .setPublishedVersion(publishedVersion)
                .setDraftedVersion(draftedVersion)
                .setUserId(userId)
                .setIsDeveloper(isDeveloper)
                .setIsManager(isManager)
                .setIsResponsible(isResponsible)
                .setIsDrafted(isDrafted)
                .setIsPublished(isPublished)
                .setIsRevoked(isRevoked);
    }

    public Integer getId() {
        return id;
    }

    public FormVersionWithMembershipFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public FormVersionWithMembershipFilter setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public FormVersionWithMembershipFilter setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public String getPublicTitle() {
        return publicTitle;
    }

    public FormVersionWithMembershipFilter setPublicTitle(String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public FormVersionWithMembershipFilter setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public FormVersionWithMembershipFilter setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public FormVersionWithMembershipFilter setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public FormVersionWithMembershipFilter setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public FormVersionWithMembershipFilter setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public Integer getFormId() {
        return formId;
    }

    public FormVersionWithMembershipFilter setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public FormVersionWithMembershipFilter setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public FormType getType() {
        return type;
    }

    public FormVersionWithMembershipFilter setType(FormType type) {
        this.type = type;
        return this;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public FormVersionWithMembershipFilter setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public FormVersionWithMembershipFilter setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public FormVersionWithMembershipFilter setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public FormVersionWithMembershipFilter setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public FormVersionWithMembershipFilter setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public FormVersionWithMembershipFilter setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public FormVersionWithMembershipFilter setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public UUID getPdfTemplateKey() {
        return pdfTemplateKey;
    }

    public FormVersionWithMembershipFilter setPdfTemplateKey(UUID pdfTemplateKey) {
        this.pdfTemplateKey = pdfTemplateKey;
        return this;
    }

    public UUID getPaymentProviderKey() {
        return paymentProviderKey;
    }

    public FormVersionWithMembershipFilter setPaymentProviderKey(UUID paymentProviderKey) {
        this.paymentProviderKey = paymentProviderKey;
        return this;
    }

    public Boolean getIdentityVerificationRequired() {
        return identityVerificationRequired;
    }

    public FormVersionWithMembershipFilter setIdentityVerificationRequired(Boolean identityVerificationRequired) {
        this.identityVerificationRequired = identityVerificationRequired;
        return this;
    }

    public UUID getIdentityProviderKey() {
        return identityProviderKey;
    }

    public FormVersionWithMembershipFilter setIdentityProviderKey(UUID identityProviderKey) {
        this.identityProviderKey = identityProviderKey;
        return this;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public FormVersionWithMembershipFilter setIsPublished(Boolean published) {
        isPublished = published;
        return this;
    }

    public Boolean getIsRevoked() {
        return isRevoked;
    }

    public FormVersionWithMembershipFilter setIsRevoked(Boolean revoked) {
        isRevoked = revoked;
        return this;
    }

    public Boolean getIsCurrentlyPublishedVersion() {
        return isCurrentlyPublishedVersion;
    }

    public FormVersionWithMembershipFilter setIsCurrentlyPublishedVersion(Boolean currentlyPublishedVersion) {
        isCurrentlyPublishedVersion = currentlyPublishedVersion;
        return this;
    }

    public Boolean getIsCurrentlyDraftedVersion() {
        return isCurrentlyDraftedVersion;
    }

    public FormVersionWithMembershipFilter setIsCurrentlyDraftedVersion(Boolean currentlyDraftedVersion) {
        isCurrentlyDraftedVersion = currentlyDraftedVersion;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public FormVersionWithMembershipFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getIsDeveloper() {
        return isDeveloper;
    }

    public FormVersionWithMembershipFilter setIsDeveloper(Boolean developer) {
        isDeveloper = developer;
        return this;
    }

    public Boolean getIsManager() {
        return isManager;
    }

    public FormVersionWithMembershipFilter setIsManager(Boolean manager) {
        isManager = manager;
        return this;
    }

    public Boolean getIsResponsible() {
        return isResponsible;
    }

    public FormVersionWithMembershipFilter setIsResponsible(Boolean responsible) {
        isResponsible = responsible;
        return this;
    }

    public FormStatus getStatus() {
        return status;
    }

    public FormVersionWithMembershipFilter setStatus(FormStatus status) {
        this.status = status;
        return this;
    }
}
