package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsAndPermissionsEntity;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.validation.Valid;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import java.util.UUID;

public class VFormVersionWithDetailsAndPermissionFilter implements Filter<VFormVersionWithDetailsAndPermissionsEntity> {
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
    private String userId;
    private Boolean formPermissionRead;

    public static VFormVersionWithDetailsAndPermissionFilter create() {
        return new VFormVersionWithDetailsAndPermissionFilter();
    }

    public static VFormVersionWithDetailsAndPermissionFilter from(@Valid VFormVersionWithDetailsFilter filter) {
        return new VFormVersionWithDetailsAndPermissionFilter()
                .setId(filter.getId())
                .setSlug(filter.getSlug())
                .setInternalTitle(filter.getInternalTitle())
                .setPublicTitle(filter.getPublicTitle())
                .setDevelopingDepartmentId(filter.getDevelopingDepartmentId())
                .setManagingDepartmentId(filter.getManagingDepartmentId())
                .setResponsibleDepartmentId(filter.getResponsibleDepartmentId())
                .setPublishedVersion(filter.getPublishedVersion())
                .setDraftedVersion(filter.getDraftedVersion())
                .setFormId(filter.getFormId())
                .setVersion(filter.getVersion())
                .setStatus(filter.getStatus())
                .setType(filter.getType())
                .setLegalSupportDepartmentId(filter.getLegalSupportDepartmentId())
                .setTechnicalSupportDepartmentId(filter.getTechnicalSupportDepartmentId())
                .setImprintDepartmentId(filter.getImprintDepartmentId())
                .setPrivacyDepartmentId(filter.getPrivacyDepartmentId())
                .setAccessibilityDepartmentId(filter.getAccessibilityDepartmentId())
                .setDestinationId(filter.getDestinationId())
                .setThemeId(filter.getThemeId())
                .setPdfTemplateKey(filter.getPdfTemplateKey())
                .setPaymentProviderKey(filter.getPaymentProviderKey())
                .setIdentityVerificationRequired(filter.getIdentityVerificationRequired());
    }

    public static VFormVersionWithDetailsAndPermissionFilter from(@Valid FormVersionFilter filter) {
        return new VFormVersionWithDetailsAndPermissionFilter()
                .setFormId(filter.getFormId())
                .setVersion(filter.getVersion())
                .setStatus(filter.getStatus())
                .setType(filter.getType())
                .setLegalSupportDepartmentId(filter.getLegalSupportDepartmentId())
                .setTechnicalSupportDepartmentId(filter.getTechnicalSupportDepartmentId())
                .setImprintDepartmentId(filter.getImprintDepartmentId())
                .setPrivacyDepartmentId(filter.getPrivacyDepartmentId())
                .setAccessibilityDepartmentId(filter.getAccessibilityDepartmentId())
                .setDestinationId(filter.getDestinationId())
                .setThemeId(filter.getThemeId())
                .setPdfTemplateKey(filter.getPdfTemplateKey())
                .setPaymentProviderKey(filter.getPaymentProviderKey())
                .setIdentityVerificationRequired(filter.getIdentityVerificationRequired())
                .setIdentityProviderKey(filter.getIdentityProviderKey());
    }

    @Nonnull
    @Override
    public Specification<VFormVersionWithDetailsAndPermissionsEntity> build() {
        return SpecificationBuilder
                .create(VFormVersionWithDetailsAndPermissionsEntity.class)
                .withEquals("id", id)
                .withEquals("slug", slug)
                .withContains("internalTitle", internalTitle)
                .withContains("publicTitle", publicTitle)
                .withEquals("developingDepartmentId", developingDepartmentId)
                .withEquals("managingDepartmentId", managingDepartmentId)
                .withEquals("responsibleDepartmentId", responsibleDepartmentId)
                .withEquals("publishedVersion", publishedVersion)
                .withEquals("draftedVersion", draftedVersion)
                .withEquals("formId", formId)
                .withEquals("version", version)
                .withEquals("status", status)
                .withEquals("type", type)
                .withEquals("legalSupportDepartmentId", legalSupportDepartmentId)
                .withEquals("technicalSupportDepartmentId", technicalSupportDepartmentId)
                .withEquals("imprintDepartmentId", imprintDepartmentId)
                .withEquals("privacyDepartmentId", privacyDepartmentId)
                .withEquals("accessibilityDepartmentId", accessibilityDepartmentId)
                .withEquals("destinationId", destinationId)
                .withEquals("themeId", themeId)
                .withEquals("pdfTemplateKey", pdfTemplateKey)
                .withEquals("paymentProviderKey", paymentProviderKey)
                .withEquals("identityVerificationRequired", identityVerificationRequired)
                .withJsonArrayElementFieldEquals("identityProviders", "identityProviderKey", identityProviderKey != null ? identityProviderKey.toString() : null)
                .withEquals("userId", userId)
                .withEquals("formPermissionRead", formPermissionRead)
                .build();
    }

    public String getUserId() {
        return userId;
    }

    public VFormVersionWithDetailsAndPermissionFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public VFormVersionWithDetailsAndPermissionFilter setFormPermissionRead(Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public VFormVersionWithDetailsAndPermissionFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public VFormVersionWithDetailsAndPermissionFilter setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public VFormVersionWithDetailsAndPermissionFilter setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public String getPublicTitle() {
        return publicTitle;
    }

    public VFormVersionWithDetailsAndPermissionFilter setPublicTitle(String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionFilter setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionFilter setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionFilter setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public VFormVersionWithDetailsAndPermissionFilter setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public VFormVersionWithDetailsAndPermissionFilter setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public Integer getFormId() {
        return formId;
    }

    public VFormVersionWithDetailsAndPermissionFilter setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public VFormVersionWithDetailsAndPermissionFilter setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public FormType getType() {
        return type;
    }

    public VFormVersionWithDetailsAndPermissionFilter setType(FormType type) {
        this.type = type;
        return this;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionFilter setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionFilter setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionFilter setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionFilter setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionFilter setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public VFormVersionWithDetailsAndPermissionFilter setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public VFormVersionWithDetailsAndPermissionFilter setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public UUID getPdfTemplateKey() {
        return pdfTemplateKey;
    }

    public VFormVersionWithDetailsAndPermissionFilter setPdfTemplateKey(UUID pdfTemplateKey) {
        this.pdfTemplateKey = pdfTemplateKey;
        return this;
    }

    public UUID getPaymentProviderKey() {
        return paymentProviderKey;
    }

    public VFormVersionWithDetailsAndPermissionFilter setPaymentProviderKey(UUID paymentProviderKey) {
        this.paymentProviderKey = paymentProviderKey;
        return this;
    }

    public Boolean getIdentityVerificationRequired() {
        return identityVerificationRequired;
    }

    public VFormVersionWithDetailsAndPermissionFilter setIdentityVerificationRequired(Boolean identityVerificationRequired) {
        this.identityVerificationRequired = identityVerificationRequired;
        return this;
    }

    public UUID getIdentityProviderKey() {
        return identityProviderKey;
    }

    public VFormVersionWithDetailsAndPermissionFilter setIdentityProviderKey(UUID identityProviderKey) {
        this.identityProviderKey = identityProviderKey;
        return this;
    }

    public FormStatus getStatus() {
        return status;
    }

    public VFormVersionWithDetailsAndPermissionFilter setStatus(FormStatus status) {
        this.status = status;
        return this;
    }
}
