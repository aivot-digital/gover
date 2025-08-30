package de.aivot.GoverBackend.form.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "form_version_with_details")
@IdClass(FormVersionWithDetailsEntityId.class)
public class FormVersionWithDetailsEntity {
    @Id
    private Integer id;
    private String slug;
    private String title;
    private String publicTitle;
    private Integer developingDepartmentId;
    private Integer managingDepartmentId;
    private Integer responsibleDepartmentId;
    @Column(columnDefinition = "int2")
    private Integer publishedVersion;
    @Column(columnDefinition = "int2")
    private Integer draftedVersion;
    private Integer formId;
    @Id
    @Column(columnDefinition = "int2")
    private Integer version;
    private FormType formType;
    private FormStatus formStatus;
    private Integer legalSupportDepartmentId;
    private Integer technicalSupportDepartmentId;
    private Integer imprintDepartmentId;
    private Integer privacyDepartmentId;
    private Integer accessibilityDepartmentId;
    private Integer destinationId;
    @Column(columnDefinition = "int2")
    private Integer customerAccessHours;
    @Column(columnDefinition = "int2")
    private Integer submissionRetentionWeeks;
    private Integer themeId;
    private UUID pdfTemplateKey;
    private UUID paymentProviderKey;
    private String paymentPurpose;
    private String paymentDescription;
    private List<PaymentProduct> paymentProducts;
    private List<IdentityProviderLink> identityProviders;
    private Boolean identityVerificationRequired;
    private RootElement rootElement;
    private LocalDateTime created;
    private LocalDateTime updated;
    private LocalDateTime published;
    private LocalDateTime revoked;
    private Boolean isCurrentlyPublishedVersion;
    private Boolean isCurrentlyDraftedVersion;

    @JsonIgnore
    public Integer getRelevantDepartmentId() {
        if (managingDepartmentId != null) {
            return managingDepartmentId;
        } else if (responsibleDepartmentId != null) {
            return responsibleDepartmentId;
        } else {
            return developingDepartmentId;
        }
    }

    // region getters and setters

    public Integer getId() {
        return id;
    }

    public FormVersionWithDetailsEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public FormVersionWithDetailsEntity setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public FormVersionWithDetailsEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getPublicTitle() {
        return publicTitle;
    }

    public FormVersionWithDetailsEntity setPublicTitle(String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public FormVersionWithDetailsEntity setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public FormVersionWithDetailsEntity setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public FormVersionWithDetailsEntity setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public FormVersionWithDetailsEntity setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public FormVersionWithDetailsEntity setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public Integer getFormId() {
        return formId;
    }

    public FormVersionWithDetailsEntity setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public FormVersionWithDetailsEntity setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public FormType getFormType() {
        return formType;
    }

    public FormVersionWithDetailsEntity setFormType(FormType formType) {
        this.formType = formType;
        return this;
    }

    public FormStatus getFormStatus() {
        return formStatus;
    }

    public FormVersionWithDetailsEntity setFormStatus(FormStatus formStatus) {
        this.formStatus = formStatus;
        return this;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public FormVersionWithDetailsEntity setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public FormVersionWithDetailsEntity setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public FormVersionWithDetailsEntity setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public FormVersionWithDetailsEntity setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public FormVersionWithDetailsEntity setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public FormVersionWithDetailsEntity setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Integer getCustomerAccessHours() {
        return customerAccessHours;
    }

    public FormVersionWithDetailsEntity setCustomerAccessHours(Integer customerAccessHours) {
        this.customerAccessHours = customerAccessHours;
        return this;
    }

    public Integer getSubmissionRetentionWeeks() {
        return submissionRetentionWeeks;
    }

    public FormVersionWithDetailsEntity setSubmissionRetentionWeeks(Integer submissionRetentionWeeks) {
        this.submissionRetentionWeeks = submissionRetentionWeeks;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public FormVersionWithDetailsEntity setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public UUID getPdfTemplateKey() {
        return pdfTemplateKey;
    }

    public FormVersionWithDetailsEntity setPdfTemplateKey(UUID pdfTemplateKey) {
        this.pdfTemplateKey = pdfTemplateKey;
        return this;
    }

    public UUID getPaymentProviderKey() {
        return paymentProviderKey;
    }

    public FormVersionWithDetailsEntity setPaymentProviderKey(UUID paymentProviderKey) {
        this.paymentProviderKey = paymentProviderKey;
        return this;
    }

    public String getPaymentPurpose() {
        return paymentPurpose;
    }

    public FormVersionWithDetailsEntity setPaymentPurpose(String paymentPurpose) {
        this.paymentPurpose = paymentPurpose;
        return this;
    }

    public String getPaymentDescription() {
        return paymentDescription;
    }

    public FormVersionWithDetailsEntity setPaymentDescription(String paymentDescription) {
        this.paymentDescription = paymentDescription;
        return this;
    }

    public List<PaymentProduct> getPaymentProducts() {
        return paymentProducts;
    }

    public FormVersionWithDetailsEntity setPaymentProducts(List<PaymentProduct> paymentProducts) {
        this.paymentProducts = paymentProducts;
        return this;
    }

    public List<IdentityProviderLink> getIdentityProviders() {
        return identityProviders;
    }

    public FormVersionWithDetailsEntity setIdentityProviders(List<IdentityProviderLink> identityProviders) {
        this.identityProviders = identityProviders;
        return this;
    }

    public Boolean getIdentityVerificationRequired() {
        return identityVerificationRequired;
    }

    public FormVersionWithDetailsEntity setIdentityVerificationRequired(Boolean identityVerificationRequired) {
        this.identityVerificationRequired = identityVerificationRequired;
        return this;
    }

    public RootElement getRootElement() {
        return rootElement;
    }

    public FormVersionWithDetailsEntity setRootElement(RootElement rootElement) {
        this.rootElement = rootElement;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public FormVersionWithDetailsEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public FormVersionWithDetailsEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public LocalDateTime getPublished() {
        return published;
    }

    public FormVersionWithDetailsEntity setPublished(LocalDateTime published) {
        this.published = published;
        return this;
    }

    public LocalDateTime getRevoked() {
        return revoked;
    }

    public FormVersionWithDetailsEntity setRevoked(LocalDateTime revoked) {
        this.revoked = revoked;
        return this;
    }

    public Boolean getCurrentlyPublishedVersion() {
        return isCurrentlyPublishedVersion;
    }

    public FormVersionWithDetailsEntity setCurrentlyPublishedVersion(Boolean currentlyPublishedVersion) {
        isCurrentlyPublishedVersion = currentlyPublishedVersion;
        return this;
    }

    public Boolean getCurrentlyDraftedVersion() {
        return isCurrentlyDraftedVersion;
    }

    public FormVersionWithDetailsEntity setCurrentlyDraftedVersion(Boolean currentlyDraftedVersion) {
        isCurrentlyDraftedVersion = currentlyDraftedVersion;
        return this;
    }

    // endregion
}
