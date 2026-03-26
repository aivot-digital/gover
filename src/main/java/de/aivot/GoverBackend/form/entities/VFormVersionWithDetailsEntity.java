package de.aivot.GoverBackend.form.entities;

import de.aivot.GoverBackend.core.converters.RootElementConverter;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.converters.IdentityProviderLinksConverter;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
import de.aivot.GoverBackend.payment.converters.PaymentProductsConverter;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "v_form_versions_with_details")
@IdClass(VFormVersionWithDetailsEntityId.class)
public class VFormVersionWithDetailsEntity implements Cloneable {
    @Id
    private Integer id;
    private String slug;
    private String internalTitle;
    private Integer developingDepartmentId;
    @Column(columnDefinition = "int2")
    private Integer publishedVersion;
    @Column(columnDefinition = "int2")
    private Integer draftedVersion;
    private Integer versionCount;
    private Integer formId;
    @Id
    @Column(columnDefinition = "int2")
    private Integer version;
    @Column(columnDefinition = "int2")
    private FormStatus status;
    @Column(columnDefinition = "int2")
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
    private String paymentPurpose;
    private String paymentDescription;
    @Column(columnDefinition = "jsonb")
    @Convert(converter = PaymentProductsConverter.class)
    private List<PaymentProduct> paymentProducts;
    @Column(columnDefinition = "jsonb")
    @Convert(converter = IdentityProviderLinksConverter.class)
    private List<IdentityProviderLink> identityProviders;
    private Boolean identityVerificationRequired;
    @Column(columnDefinition = "int2")
    private Integer customerAccessHours;
    @Column(columnDefinition = "int2")
    private Integer submissionRetentionWeeks;
    @Column(columnDefinition = "jsonb")
    @Convert(converter = RootElementConverter.class)
    private FormLayoutElement rootElement;
    private LocalDateTime created;
    private LocalDateTime updated;
    private LocalDateTime published;
    private LocalDateTime revoked;
    private String publicTitle;
    private Integer managingDepartmentId;
    private Integer responsibleDepartmentId;

    public FormEntity toFormEntity() {
        return new FormEntity(
                this.id,
                this.slug,
                this.internalTitle,
                this.developingDepartmentId,
                this.created,
                this.updated,
                this.publishedVersion,
                this.draftedVersion,
                this.versionCount
        );
    }

    public FormVersionEntity toFormVersionEntity() {
        return new FormVersionEntity(
                this.formId,
                this.publicTitle,
                this.version,
                this.status,
                this.type,
                this.managingDepartmentId,
                this.responsibleDepartmentId,
                this.legalSupportDepartmentId,
                this.technicalSupportDepartmentId,
                this.imprintDepartmentId,
                this.privacyDepartmentId,
                this.accessibilityDepartmentId,
                this.destinationId,
                this.customerAccessHours,
                this.submissionRetentionWeeks,
                this.themeId,
                this.pdfTemplateKey,
                this.paymentProviderKey,
                this.paymentPurpose,
                this.paymentDescription,
                this.paymentProducts,
                this.identityProviders,
                this.identityVerificationRequired,
                this.rootElement,
                this.created,
                this.updated,
                this.published,
                this.revoked
        );
    }

    public VFormVersionWithDetailsEntity clone() {
        try {
            return ((VFormVersionWithDetailsEntity) super.clone())
                    .setId(0)
                    .setFormId(0)
                    .setVersion(0);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone VFormVersionWithDetailsEntity", e);
        }
    }

    public Integer getRelevantDepartmentId() {
        if (this.getManagingDepartmentId() == null) {
            return this.getDevelopingDepartmentId();
        }

        if (this.getDevelopingDepartmentId() == null) {
            return this.getManagingDepartmentId();
        }

        return this.getDevelopingDepartmentId();
    }

    public Integer getId() {
        return id;
    }

    public VFormVersionWithDetailsEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public VFormVersionWithDetailsEntity setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public VFormVersionWithDetailsEntity setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public VFormVersionWithDetailsEntity setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public VFormVersionWithDetailsEntity setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public VFormVersionWithDetailsEntity setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public Integer getVersionCount() {
        return versionCount;
    }

    public VFormVersionWithDetailsEntity setVersionCount(Integer versionCount) {
        this.versionCount = versionCount;
        return this;
    }

    public Integer getFormId() {
        return formId;
    }

    public VFormVersionWithDetailsEntity setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public VFormVersionWithDetailsEntity setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public FormStatus getStatus() {
        return status;
    }

    public VFormVersionWithDetailsEntity setStatus(FormStatus status) {
        this.status = status;
        return this;
    }

    public FormType getType() {
        return type;
    }

    public VFormVersionWithDetailsEntity setType(FormType type) {
        this.type = type;
        return this;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public VFormVersionWithDetailsEntity setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public VFormVersionWithDetailsEntity setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public VFormVersionWithDetailsEntity setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public VFormVersionWithDetailsEntity setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public VFormVersionWithDetailsEntity setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public VFormVersionWithDetailsEntity setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public VFormVersionWithDetailsEntity setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public UUID getPdfTemplateKey() {
        return pdfTemplateKey;
    }

    public VFormVersionWithDetailsEntity setPdfTemplateKey(UUID pdfTemplateKey) {
        this.pdfTemplateKey = pdfTemplateKey;
        return this;
    }

    public UUID getPaymentProviderKey() {
        return paymentProviderKey;
    }

    public VFormVersionWithDetailsEntity setPaymentProviderKey(UUID paymentProviderKey) {
        this.paymentProviderKey = paymentProviderKey;
        return this;
    }

    public String getPaymentPurpose() {
        return paymentPurpose;
    }

    public VFormVersionWithDetailsEntity setPaymentPurpose(String paymentPurpose) {
        this.paymentPurpose = paymentPurpose;
        return this;
    }

    public String getPaymentDescription() {
        return paymentDescription;
    }

    public VFormVersionWithDetailsEntity setPaymentDescription(String paymentDescription) {
        this.paymentDescription = paymentDescription;
        return this;
    }

    public List<PaymentProduct> getPaymentProducts() {
        return paymentProducts;
    }

    public VFormVersionWithDetailsEntity setPaymentProducts(List<PaymentProduct> paymentProducts) {
        this.paymentProducts = paymentProducts;
        return this;
    }

    public List<IdentityProviderLink> getIdentityProviders() {
        return identityProviders;
    }

    public VFormVersionWithDetailsEntity setIdentityProviders(List<IdentityProviderLink> identityProviders) {
        this.identityProviders = identityProviders;
        return this;
    }

    public Boolean getIdentityVerificationRequired() {
        return identityVerificationRequired;
    }

    public VFormVersionWithDetailsEntity setIdentityVerificationRequired(Boolean identityVerificationRequired) {
        this.identityVerificationRequired = identityVerificationRequired;
        return this;
    }

    public Integer getCustomerAccessHours() {
        return customerAccessHours;
    }

    public VFormVersionWithDetailsEntity setCustomerAccessHours(Integer customerAccessHours) {
        this.customerAccessHours = customerAccessHours;
        return this;
    }

    public Integer getSubmissionRetentionWeeks() {
        return submissionRetentionWeeks;
    }

    public VFormVersionWithDetailsEntity setSubmissionRetentionWeeks(Integer submissionRetentionWeeks) {
        this.submissionRetentionWeeks = submissionRetentionWeeks;
        return this;
    }

    public FormLayoutElement getRootElement() {
        return rootElement;
    }

    public VFormVersionWithDetailsEntity setRootElement(FormLayoutElement rootElement) {
        this.rootElement = rootElement;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public VFormVersionWithDetailsEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public VFormVersionWithDetailsEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public LocalDateTime getPublished() {
        return published;
    }

    public VFormVersionWithDetailsEntity setPublished(LocalDateTime published) {
        this.published = published;
        return this;
    }

    public LocalDateTime getRevoked() {
        return revoked;
    }

    public VFormVersionWithDetailsEntity setRevoked(LocalDateTime revoked) {
        this.revoked = revoked;
        return this;
    }

    public String getPublicTitle() {
        return publicTitle;
    }

    public VFormVersionWithDetailsEntity setPublicTitle(String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public VFormVersionWithDetailsEntity setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public VFormVersionWithDetailsEntity setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }
}
