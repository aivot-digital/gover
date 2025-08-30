package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import java.util.UUID;

public class FormVersionFilter implements Filter<FormEntity> {
    private Integer formId;
    private Integer version;
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

    public static FormVersionFilter create() {
        return new FormVersionFilter();
    }

    @Nonnull
    @Override
    public Specification<FormEntity> build() {
        var builder = SpecificationBuilder
                .create(FormEntity.class)
                .withEquals("formId", formId)
                .withEquals("version", version)
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
                .withJsonArrayElementFieldEquals("identityProviders", "identityProviderKey", identityProviderKey.toString());

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

    public Integer getFormId() {
        return formId;
    }

    public FormVersionFilter setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public FormVersionFilter setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public FormType getType() {
        return type;
    }

    public FormVersionFilter setType(FormType type) {
        this.type = type;
        return this;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public FormVersionFilter setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public FormVersionFilter setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public FormVersionFilter setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public FormVersionFilter setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public FormVersionFilter setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public FormVersionFilter setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public FormVersionFilter setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public UUID getPdfTemplateKey() {
        return pdfTemplateKey;
    }

    public FormVersionFilter setPdfTemplateKey(UUID pdfTemplateKey) {
        this.pdfTemplateKey = pdfTemplateKey;
        return this;
    }

    public UUID getPaymentProviderKey() {
        return paymentProviderKey;
    }

    public FormVersionFilter setPaymentProviderKey(UUID paymentProviderKey) {
        this.paymentProviderKey = paymentProviderKey;
        return this;
    }

    public Boolean getIdentityVerificationRequired() {
        return identityVerificationRequired;
    }

    public FormVersionFilter setIdentityVerificationRequired(Boolean identityVerificationRequired) {
        this.identityVerificationRequired = identityVerificationRequired;
        return this;
    }

    public UUID getIdentityProviderKey() {
        return identityProviderKey;
    }

    public FormVersionFilter setIdentityProviderKey(UUID identityProviderKey) {
        this.identityProviderKey = identityProviderKey;
        return this;
    }

    public Boolean getPublished() {
        return isPublished;
    }

    public FormVersionFilter setPublished(Boolean published) {
        isPublished = published;
        return this;
    }

    public Boolean getRevoked() {
        return isRevoked;
    }

    public FormVersionFilter setRevoked(Boolean revoked) {
        isRevoked = revoked;
        return this;
    }
}
