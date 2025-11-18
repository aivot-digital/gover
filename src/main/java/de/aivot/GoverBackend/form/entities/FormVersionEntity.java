package de.aivot.GoverBackend.form.entities;

import de.aivot.GoverBackend.core.converters.RootElementConverter;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.form.enums.FormStatus;
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

    @Nonnull
    @Column(columnDefinition = "text")
    private String publicTitle;

    @Id
    @Nonnull
    @Column(columnDefinition = "int2")
    private Integer version;

    @Nonnull
    @ColumnDefault("0")
    @Column(columnDefinition = "int2")
    private FormStatus status = FormStatus.Drafted;

    @Nonnull
    @ColumnDefault("0")
    @Column(columnDefinition = "int2")
    private FormType type = FormType.Public;

    @Nullable
    private Integer managingOrganizationalUnitId;

    @Nullable
    private Integer responsibleOrganizationalUnitId;

    @Nullable
    private Integer legalSupportOrganizationalUnitId;

    @Nullable
    private Integer technicalSupportOrganizationalUnitId;

    @Nullable
    private Integer imprintOrganizationalUnitId;

    @Nullable
    private Integer privacyOrganizationalUnitId;

    @Nullable
    private Integer accessibilityOrganizationalUnitId;

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
                             @Nonnull FormStatus status,
                             @Nonnull String publicTitle,
                             @Nonnull FormType type,
                             @Nullable Integer managingOrganizationalUnitId,
                             @Nullable Integer responsibleOrganizationalUnitId,
                             @Nullable Integer legalSupportOrganizationalUnitId,
                             @Nullable Integer technicalSupportOrganizationalUnitId,
                             @Nullable Integer imprintOrganizationalUnitId,
                             @Nullable Integer privacyOrganizationalUnitId,
                             @Nullable Integer accessibilityOrganizationalUnitId,
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
        this.status = status;
        this.publicTitle = publicTitle;
        this.type = type;
        this.managingOrganizationalUnitId = managingOrganizationalUnitId;
        this.responsibleOrganizationalUnitId = responsibleOrganizationalUnitId;
        this.legalSupportOrganizationalUnitId = legalSupportOrganizationalUnitId;
        this.technicalSupportOrganizationalUnitId = technicalSupportOrganizationalUnitId;
        this.imprintOrganizationalUnitId = imprintOrganizationalUnitId;
        this.privacyOrganizationalUnitId = privacyOrganizationalUnitId;
        this.accessibilityOrganizationalUnitId = accessibilityOrganizationalUnitId;
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
                formVersionWithDetailsEntity.getStatus(),
                formVersionWithDetailsEntity.getPublicTitle(),
                formVersionWithDetailsEntity.getType(),
                formVersionWithDetailsEntity.getManagingDepartmentId(),
                formVersionWithDetailsEntity.getResponsibleDepartmentId(),
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
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        FormVersionEntity that = (FormVersionEntity) o;
        return formId.equals(that.formId) && publicTitle.equals(that.publicTitle) && version.equals(that.version) && status == that.status && type == that.type && Objects.equals(managingOrganizationalUnitId, that.managingOrganizationalUnitId) && Objects.equals(responsibleOrganizationalUnitId, that.responsibleOrganizationalUnitId) && Objects.equals(legalSupportOrganizationalUnitId, that.legalSupportOrganizationalUnitId) && Objects.equals(technicalSupportOrganizationalUnitId, that.technicalSupportOrganizationalUnitId) && Objects.equals(imprintOrganizationalUnitId, that.imprintOrganizationalUnitId) && Objects.equals(privacyOrganizationalUnitId, that.privacyOrganizationalUnitId) && Objects.equals(accessibilityOrganizationalUnitId, that.accessibilityOrganizationalUnitId) && Objects.equals(destinationId, that.destinationId) && Objects.equals(customerAccessHours, that.customerAccessHours) && Objects.equals(submissionRetentionWeeks, that.submissionRetentionWeeks) && Objects.equals(themeId, that.themeId) && Objects.equals(pdfTemplateKey, that.pdfTemplateKey) && Objects.equals(paymentProviderKey, that.paymentProviderKey) && Objects.equals(paymentPurpose, that.paymentPurpose) && Objects.equals(paymentDescription, that.paymentDescription) && Objects.equals(paymentProducts, that.paymentProducts) && Objects.equals(identityProviders, that.identityProviders) && Objects.equals(identityVerificationRequired, that.identityVerificationRequired) && rootElement.equals(that.rootElement) && created.equals(that.created) && updated.equals(that.updated) && Objects.equals(published, that.published) && Objects.equals(revoked, that.revoked);
    }

    @Override
    public int hashCode() {
        int result = formId.hashCode();
        result = 31 * result + publicTitle.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + Objects.hashCode(managingOrganizationalUnitId);
        result = 31 * result + Objects.hashCode(responsibleOrganizationalUnitId);
        result = 31 * result + Objects.hashCode(legalSupportOrganizationalUnitId);
        result = 31 * result + Objects.hashCode(technicalSupportOrganizationalUnitId);
        result = 31 * result + Objects.hashCode(imprintOrganizationalUnitId);
        result = 31 * result + Objects.hashCode(privacyOrganizationalUnitId);
        result = 31 * result + Objects.hashCode(accessibilityOrganizationalUnitId);
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
    public String getPublicTitle() {
        return publicTitle;
    }

    public FormVersionEntity setPublicTitle(@Nonnull String publicTitle) {
        this.publicTitle = publicTitle;
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
    public FormStatus getStatus() {
        return status;
    }

    public FormVersionEntity setStatus(@Nonnull FormStatus status) {
        this.status = status;
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
    public Integer getManagingOrganizationalUnitId() {
        return managingOrganizationalUnitId;
    }

    public FormVersionEntity setManagingOrganizationalUnitId(@Nullable Integer managingDepartmentId) {
        this.managingOrganizationalUnitId = managingDepartmentId;
        return this;
    }

    @Nullable
    public Integer getResponsibleOrganizationalUnitId() {
        return responsibleOrganizationalUnitId;
    }

    public FormVersionEntity setResponsibleOrganizationalUnitId(@Nullable Integer responsibleDepartmentId) {
        this.responsibleOrganizationalUnitId = responsibleDepartmentId;
        return this;
    }

    @Nullable
    public Integer getLegalSupportOrganizationalUnitId() {
        return legalSupportOrganizationalUnitId;
    }

    public FormVersionEntity setLegalSupportOrganizationalUnitId(@Nullable Integer legalSupportDepartmentId) {
        this.legalSupportOrganizationalUnitId = legalSupportDepartmentId;
        return this;
    }

    @Nullable
    public Integer getTechnicalSupportOrganizationalUnitId() {
        return technicalSupportOrganizationalUnitId;
    }

    public FormVersionEntity setTechnicalSupportOrganizationalUnitId(@Nullable Integer technicalSupportDepartmentId) {
        this.technicalSupportOrganizationalUnitId = technicalSupportDepartmentId;
        return this;
    }

    @Nullable
    public Integer getImprintOrganizationalUnitId() {
        return imprintOrganizationalUnitId;
    }

    public FormVersionEntity setImprintOrganizationalUnitId(@Nullable Integer imprintDepartmentId) {
        this.imprintOrganizationalUnitId = imprintDepartmentId;
        return this;
    }

    @Nullable
    public Integer getPrivacyOrganizationalUnitId() {
        return privacyOrganizationalUnitId;
    }

    public FormVersionEntity setPrivacyOrganizationalUnitId(@Nullable Integer privacyDepartmentId) {
        this.privacyOrganizationalUnitId = privacyDepartmentId;
        return this;
    }

    @Nullable
    public Integer getAccessibilityOrganizationalUnitId() {
        return accessibilityOrganizationalUnitId;
    }

    public FormVersionEntity setAccessibilityOrganizationalUnitId(@Nullable Integer accessibilityDepartmentId) {
        this.accessibilityOrganizationalUnitId = accessibilityDepartmentId;
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
