package de.aivot.GoverBackend.form.entities;

import de.aivot.GoverBackend.core.converters.RootElementConverter;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.converters.IdentityProviderLinksConverter;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
import de.aivot.GoverBackend.payment.converters.PaymentProductsConverter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "form_versions")
@IdClass(FormVersionEntityId.class)
public class FormVersionEntity {
    @Id
    @Nonnull
    private Integer formId;

    @Id
    @Nonnull
    @Column(columnDefinition = "int2")
    private Integer version;

    @Nonnull
    @ColumnDefault("0")
    @Column(columnDefinition = "int2")
    private FormType type = FormType.Public;

    @Nullable
    private Integer legalSupportDepartmentId;

    @Nullable
    private Integer technicalSupportDepartmentId;

    @Nullable
    private Integer imprintDepartmentId;

    @Nullable
    private Integer privacyDepartmentId;

    @Nullable
    private Integer accessibilityDepartmentId;

    @Nullable
    private Integer destinationId;

    @Nullable
    @ColumnDefault("4")
    @Column(columnDefinition = "int2")
    private Integer customerAccessHours = 4;

    @Nullable
    @ColumnDefault("4")
    @Column(columnDefinition = "int2")
    private Integer submissionRetentionWeeks = 4;

    @Nullable
    private Integer themeId;

    @Nullable
    private UUID pdfTemplateKey;

    @Nullable
    private UUID paymentProviderKey;

    @Nullable
    @Column(length = 27)
    private String paymentPurpose;

    @Nullable
    @Column(length = 250)
    private String paymentDescription;

    @Nullable
    @Column(columnDefinition = "jsonb")
    @Convert(converter = PaymentProductsConverter.class)
    private List<PaymentProduct> paymentProducts;

    @Nullable
    @Column(columnDefinition = "jsonb")
    @Convert(converter = IdentityProviderLinksConverter.class)
    private List<IdentityProviderLink> identityProviders = new LinkedList<>();

    @Nullable
    private Boolean identityVerificationRequired;

    @Nonnull
    @Convert(converter = RootElementConverter.class)
    @Column(columnDefinition = "jsonb")
    private RootElement rootElement;

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private LocalDateTime updated;

    @Nullable
    private LocalDateTime published;

    @Nullable
    private LocalDateTime revoked;

    // region Constructors

    // Empty constructor for JPA
    public FormVersionEntity() {
    }

    // Full constructor

    public FormVersionEntity(@Nonnull Integer formId,
                             @Nonnull Integer version,
                             @Nonnull FormType type,
                             @Nullable Integer legalSupportDepartmentId,
                             @Nullable Integer technicalSupportDepartmentId,
                             @Nullable Integer imprintDepartmentId,
                             @Nullable Integer privacyDepartmentId,
                             @Nullable Integer accessibilityDepartmentId,
                             @Nullable Integer destinationId,
                             @Nullable Integer customerAccessHours,
                             @Nullable Integer submissionRetentionWeeks,
                             @Nullable Integer themeId,
                             @Nullable UUID pdfTemplateKey,
                             @Nullable UUID paymentProviderKey,
                             @Nullable String paymentPurpose,
                             @Nullable String paymentDescription,
                             @Nullable List<PaymentProduct> paymentProducts,
                             @Nullable List<IdentityProviderLink> identityProviders,
                             @Nullable Boolean identityVerificationRequired,
                             @Nonnull RootElement rootElement,
                             @Nonnull LocalDateTime created,
                             @Nonnull LocalDateTime updated,
                             @Nullable LocalDateTime published,
                             @Nullable LocalDateTime revoked) {
        this.formId = formId;
        this.version = version;
        this.type = type;
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        this.imprintDepartmentId = imprintDepartmentId;
        this.privacyDepartmentId = privacyDepartmentId;
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        this.destinationId = destinationId;
        this.customerAccessHours = customerAccessHours;
        this.submissionRetentionWeeks = submissionRetentionWeeks;
        this.themeId = themeId;
        this.pdfTemplateKey = pdfTemplateKey;
        this.paymentProviderKey = paymentProviderKey;
        this.paymentPurpose = paymentPurpose;
        this.paymentDescription = paymentDescription;
        this.paymentProducts = paymentProducts;
        this.identityProviders = identityProviders;
        this.identityVerificationRequired = identityVerificationRequired;
        this.rootElement = rootElement;
        this.created = created;
        this.updated = updated;
        this.published = published;
        this.revoked = revoked;
    }

    public static FormVersionEntity from(FormVersionWithDetailsEntity formVersionWithDetailsEntity) {
        return new FormVersionEntity(
                formVersionWithDetailsEntity.getId(),
                formVersionWithDetailsEntity.getVersion(),
                formVersionWithDetailsEntity.getType(),
                formVersionWithDetailsEntity.getLegalSupportDepartmentId(),
                formVersionWithDetailsEntity.getTechnicalSupportDepartmentId(),
                formVersionWithDetailsEntity.getImprintDepartmentId(),
                formVersionWithDetailsEntity.getPrivacyDepartmentId(),
                formVersionWithDetailsEntity.getAccessibilityDepartmentId(),
                formVersionWithDetailsEntity.getDestinationId(),
                formVersionWithDetailsEntity.getCustomerAccessHours(),
                formVersionWithDetailsEntity.getSubmissionRetentionWeeks(),
                formVersionWithDetailsEntity.getThemeId(),
                formVersionWithDetailsEntity.getPdfTemplateKey(),
                formVersionWithDetailsEntity.getPaymentProviderKey(),
                formVersionWithDetailsEntity.getPaymentPurpose(),
                formVersionWithDetailsEntity.getPaymentDescription(),
                formVersionWithDetailsEntity.getPaymentProducts(),
                formVersionWithDetailsEntity.getIdentityProviders(),
                formVersionWithDetailsEntity.getIdentityVerificationRequired(),
                formVersionWithDetailsEntity.getRootElement(),
                formVersionWithDetailsEntity.getCreated(),
                formVersionWithDetailsEntity.getUpdated(),
                formVersionWithDetailsEntity.getPublished(),
                formVersionWithDetailsEntity.getRevoked()
        );
    }

    // endregion

    // region Signales

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
    }

    // endregion

    // region Equals & HashCode

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        FormVersionEntity that = (FormVersionEntity) object;
        return formId.equals(that.formId) && version.equals(that.version) && type == that.type && Objects.equals(legalSupportDepartmentId, that.legalSupportDepartmentId) && Objects.equals(technicalSupportDepartmentId, that.technicalSupportDepartmentId) && Objects.equals(imprintDepartmentId, that.imprintDepartmentId) && Objects.equals(privacyDepartmentId, that.privacyDepartmentId) && Objects.equals(accessibilityDepartmentId, that.accessibilityDepartmentId) && Objects.equals(destinationId, that.destinationId) && Objects.equals(customerAccessHours, that.customerAccessHours) && Objects.equals(submissionRetentionWeeks, that.submissionRetentionWeeks) && Objects.equals(themeId, that.themeId) && Objects.equals(pdfTemplateKey, that.pdfTemplateKey) && Objects.equals(paymentProviderKey, that.paymentProviderKey) && Objects.equals(paymentPurpose, that.paymentPurpose) && Objects.equals(paymentDescription, that.paymentDescription) && Objects.equals(paymentProducts, that.paymentProducts) && Objects.equals(identityProviders, that.identityProviders) && Objects.equals(identityVerificationRequired, that.identityVerificationRequired) && rootElement.equals(that.rootElement) && created.equals(that.created) && updated.equals(that.updated) && Objects.equals(published, that.published) && Objects.equals(revoked, that.revoked);
    }

    @Override
    public int hashCode() {
        int result = formId.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + Objects.hashCode(legalSupportDepartmentId);
        result = 31 * result + Objects.hashCode(technicalSupportDepartmentId);
        result = 31 * result + Objects.hashCode(imprintDepartmentId);
        result = 31 * result + Objects.hashCode(privacyDepartmentId);
        result = 31 * result + Objects.hashCode(accessibilityDepartmentId);
        result = 31 * result + Objects.hashCode(destinationId);
        result = 31 * result + Objects.hashCode(customerAccessHours);
        result = 31 * result + Objects.hashCode(submissionRetentionWeeks);
        result = 31 * result + Objects.hashCode(themeId);
        result = 31 * result + Objects.hashCode(pdfTemplateKey);
        result = 31 * result + Objects.hashCode(paymentProviderKey);
        result = 31 * result + Objects.hashCode(paymentPurpose);
        result = 31 * result + Objects.hashCode(paymentDescription);
        result = 31 * result + Objects.hashCode(paymentProducts);
        result = 31 * result + Objects.hashCode(identityProviders);
        result = 31 * result + Objects.hashCode(identityVerificationRequired);
        result = 31 * result + rootElement.hashCode();
        result = 31 * result + created.hashCode();
        result = 31 * result + updated.hashCode();
        result = 31 * result + Objects.hashCode(published);
        result = 31 * result + Objects.hashCode(revoked);
        return result;
    }


    // endregion

    // region Getters & Setters

    @Nonnull
    public Integer getFormId() {
        return formId;
    }

    public FormVersionEntity setFormId(@Nonnull Integer formId) {
        this.formId = formId;
        return this;
    }

    @Nonnull
    public Integer getVersion() {
        return version;
    }

    public FormVersionEntity setVersion(@Nonnull Integer version) {
        this.version = version;
        return this;
    }

    @Nonnull
    public FormType getType() {
        return type;
    }

    public FormVersionEntity setType(@Nonnull FormType type) {
        this.type = type;
        return this;
    }

    @Nullable
    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public FormVersionEntity setLegalSupportDepartmentId(@Nullable Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    @Nullable
    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public FormVersionEntity setTechnicalSupportDepartmentId(@Nullable Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    @Nullable
    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public FormVersionEntity setImprintDepartmentId(@Nullable Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    @Nullable
    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public FormVersionEntity setPrivacyDepartmentId(@Nullable Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    @Nullable
    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public FormVersionEntity setAccessibilityDepartmentId(@Nullable Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    @Nullable
    public Integer getDestinationId() {
        return destinationId;
    }

    public FormVersionEntity setDestinationId(@Nullable Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    @Nullable
    public Integer getCustomerAccessHours() {
        return customerAccessHours;
    }

    public FormVersionEntity setCustomerAccessHours(@Nullable Integer customerAccessHours) {
        this.customerAccessHours = customerAccessHours;
        return this;
    }

    @Nullable
    public Integer getSubmissionRetentionWeeks() {
        return submissionRetentionWeeks;
    }

    public FormVersionEntity setSubmissionRetentionWeeks(@Nullable Integer submissionRetentionWeeks) {
        this.submissionRetentionWeeks = submissionRetentionWeeks;
        return this;
    }

    @Nullable
    public Integer getThemeId() {
        return themeId;
    }

    public FormVersionEntity setThemeId(@Nullable Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    @Nullable
    public UUID getPdfTemplateKey() {
        return pdfTemplateKey;
    }

    public FormVersionEntity setPdfTemplateKey(@Nullable UUID pdfTemplateKey) {
        this.pdfTemplateKey = pdfTemplateKey;
        return this;
    }

    @Nullable
    public UUID getPaymentProviderKey() {
        return paymentProviderKey;
    }

    public FormVersionEntity setPaymentProviderKey(@Nullable UUID paymentProviderKey) {
        this.paymentProviderKey = paymentProviderKey;
        return this;
    }

    @Nullable
    public String getPaymentPurpose() {
        return paymentPurpose;
    }

    public FormVersionEntity setPaymentPurpose(@Nullable String paymentPurpose) {
        this.paymentPurpose = paymentPurpose;
        return this;
    }

    @Nullable
    public String getPaymentDescription() {
        return paymentDescription;
    }

    public FormVersionEntity setPaymentDescription(@Nullable String paymentDescription) {
        this.paymentDescription = paymentDescription;
        return this;
    }

    @Nullable
    public List<PaymentProduct> getPaymentProducts() {
        return paymentProducts;
    }

    public FormVersionEntity setPaymentProducts(@Nullable List<PaymentProduct> paymentProducts) {
        this.paymentProducts = paymentProducts;
        return this;
    }

    @Nullable
    public List<IdentityProviderLink> getIdentityProviders() {
        return identityProviders;
    }

    public FormVersionEntity setIdentityProviders(@Nullable List<IdentityProviderLink> identityProviders) {
        this.identityProviders = identityProviders;
        return this;
    }

    @Nullable
    public Boolean getIdentityVerificationRequired() {
        return identityVerificationRequired;
    }

    public FormVersionEntity setIdentityVerificationRequired(@Nullable Boolean identityVerificationRequired) {
        this.identityVerificationRequired = identityVerificationRequired;
        return this;
    }

    @Nonnull
    public RootElement getRootElement() {
        return rootElement;
    }

    public FormVersionEntity setRootElement(@Nonnull RootElement rootElement) {
        this.rootElement = rootElement;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public FormVersionEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public FormVersionEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public LocalDateTime getPublished() {
        return published;
    }

    public FormVersionEntity setPublished(@Nullable LocalDateTime published) {
        this.published = published;
        return this;
    }

    @Nullable
    public LocalDateTime getRevoked() {
        return revoked;
    }

    public FormVersionEntity setRevoked(@Nullable LocalDateTime revoked) {
        this.revoked = revoked;
        return this;
    }

    // endregion
}
