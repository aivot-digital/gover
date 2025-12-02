package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import java.util.UUID;

public class VFormVersionWithDetailsFilter implements Filter<VFormVersionWithDetailsEntity> {
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

    public static VFormVersionWithDetailsFilter create() {
        return new VFormVersionWithDetailsFilter();
    }

    @Nonnull
    @Override
    public Specification<VFormVersionWithDetailsEntity> build() {
        return SpecificationBuilder
                .create(VFormVersionWithDetailsEntity.class)
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
                .build();
    }

    public Integer getId() {
        return id;
    }

    public VFormVersionWithDetailsFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public VFormVersionWithDetailsFilter setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public VFormVersionWithDetailsFilter setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public String getPublicTitle() {
        return publicTitle;
    }

    public VFormVersionWithDetailsFilter setPublicTitle(String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public VFormVersionWithDetailsFilter setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public VFormVersionWithDetailsFilter setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public VFormVersionWithDetailsFilter setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public VFormVersionWithDetailsFilter setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public VFormVersionWithDetailsFilter setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public Integer getFormId() {
        return formId;
    }

    public VFormVersionWithDetailsFilter setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public VFormVersionWithDetailsFilter setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public FormType getType() {
        return type;
    }

    public VFormVersionWithDetailsFilter setType(FormType type) {
        this.type = type;
        return this;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public VFormVersionWithDetailsFilter setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public VFormVersionWithDetailsFilter setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public VFormVersionWithDetailsFilter setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public VFormVersionWithDetailsFilter setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public VFormVersionWithDetailsFilter setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public VFormVersionWithDetailsFilter setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public VFormVersionWithDetailsFilter setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public UUID getPdfTemplateKey() {
        return pdfTemplateKey;
    }

    public VFormVersionWithDetailsFilter setPdfTemplateKey(UUID pdfTemplateKey) {
        this.pdfTemplateKey = pdfTemplateKey;
        return this;
    }

    public UUID getPaymentProviderKey() {
        return paymentProviderKey;
    }

    public VFormVersionWithDetailsFilter setPaymentProviderKey(UUID paymentProviderKey) {
        this.paymentProviderKey = paymentProviderKey;
        return this;
    }

    public Boolean getIdentityVerificationRequired() {
        return identityVerificationRequired;
    }

    public VFormVersionWithDetailsFilter setIdentityVerificationRequired(Boolean identityVerificationRequired) {
        this.identityVerificationRequired = identityVerificationRequired;
        return this;
    }

    public UUID getIdentityProviderKey() {
        return identityProviderKey;
    }

    public VFormVersionWithDetailsFilter setIdentityProviderKey(UUID identityProviderKey) {
        this.identityProviderKey = identityProviderKey;
        return this;
    }

    public FormStatus getStatus() {
        return status;
    }

    public VFormVersionWithDetailsFilter setStatus(FormStatus status) {
        this.status = status;
        return this;
    }
}
