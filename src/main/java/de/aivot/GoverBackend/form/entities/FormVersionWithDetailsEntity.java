package de.aivot.GoverBackend.form.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.converters.RootElementConverter;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
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
@Table(name = "form_version_with_details")
@IdClass(FormVersionWithDetailsEntityId.class)
public class FormVersionWithDetailsEntity implements Cloneable {
    @Id
    private Integer id;
    private String slug;
    private String internalTitle;
    private String publicTitle;
    private Integer developingDepartmentId;
    private Integer managingDepartmentId;
    private Integer responsibleDepartmentId;
    @Column(columnDefinition = "int2")
    private Integer publishedVersion;
    @Column(columnDefinition = "int2")
    private Integer draftedVersion;
    private Integer versionCount;
    private Integer formId;
    @Id
    @Column(columnDefinition = "int2")
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
    private RootElement rootElement;
    private LocalDateTime created;
    private LocalDateTime updated;
    private LocalDateTime published;
    private LocalDateTime revoked;

    // region constructors

    // Default constructor is required by JPA
    public FormVersionWithDetailsEntity() {
    }

    // Full constructor
    public FormVersionWithDetailsEntity(Integer id,
                                        String slug,
                                        String internalTitle,
                                        String publicTitle,
                                        Integer developingDepartmentId,
                                        Integer managingDepartmentId,
                                        Integer responsibleDepartmentId,
                                        Integer publishedVersion,
                                        Integer draftedVersion,
                                        Integer versionCount,
                                        Integer formId,
                                        Integer version,
                                        FormStatus status,
                                        FormType type,
                                        Integer legalSupportDepartmentId,
                                        Integer technicalSupportDepartmentId,
                                        Integer imprintDepartmentId,
                                        Integer privacyDepartmentId,
                                        Integer accessibilityDepartmentId,
                                        Integer destinationId,
                                        Integer themeId,
                                        UUID pdfTemplateKey,
                                        UUID paymentProviderKey,
                                        String paymentPurpose,
                                        String paymentDescription,
                                        List<PaymentProduct> paymentProducts,
                                        List<IdentityProviderLink> identityProviders,
                                        Boolean identityVerificationRequired,
                                        Integer customerAccessHours,
                                        Integer submissionRetentionWeeks,
                                        RootElement rootElement,
                                        LocalDateTime created,
                                        LocalDateTime updated,
                                        LocalDateTime published,
                                        LocalDateTime revoked) {
        this.id = id;
        this.slug = slug;
        this.internalTitle = internalTitle;
        this.publicTitle = publicTitle;
        this.developingDepartmentId = developingDepartmentId;
        this.managingDepartmentId = managingDepartmentId;
        this.responsibleDepartmentId = responsibleDepartmentId;
        this.publishedVersion = publishedVersion;
        this.draftedVersion = draftedVersion;
        this.versionCount = versionCount;
        this.formId = formId;
        this.version = version;
        this.status = status;
        this.type = type;
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        this.imprintDepartmentId = imprintDepartmentId;
        this.privacyDepartmentId = privacyDepartmentId;
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        this.destinationId = destinationId;
        this.themeId = themeId;
        this.pdfTemplateKey = pdfTemplateKey;
        this.paymentProviderKey = paymentProviderKey;
        this.paymentPurpose = paymentPurpose;
        this.paymentDescription = paymentDescription;
        this.paymentProducts = paymentProducts;
        this.identityProviders = identityProviders;
        this.identityVerificationRequired = identityVerificationRequired;
        this.customerAccessHours = customerAccessHours;
        this.submissionRetentionWeeks = submissionRetentionWeeks;
        this.rootElement = rootElement;
        this.created = created;
        this.updated = updated;
        this.published = published;
        this.revoked = revoked;
    }

    public static FormVersionWithDetailsEntity of(FormEntity form, FormVersionEntity version) {
        return new FormVersionWithDetailsEntity(
                version.getFormId(),
                form.getSlug(),
                form.getInternalTitle(),
                version.getPublicTitle(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getPublishedVersion(),
                form.getDraftedVersion(),
                form.getVersionCount(),
                version.getFormId(),
                version.getVersion(),
                version.getStatus(),
                version.getType(),
                version.getLegalSupportDepartmentId(),
                version.getTechnicalSupportDepartmentId(),
                version.getImprintDepartmentId(),
                version.getPrivacyDepartmentId(),
                version.getAccessibilityDepartmentId(),
                version.getDestinationId(),
                version.getThemeId(),
                version.getPdfTemplateKey(),
                version.getPaymentProviderKey(),
                version.getPaymentPurpose(),
                version.getPaymentDescription(),
                version.getPaymentProducts(),
                version.getIdentityProviders(),
                version.getIdentityVerificationRequired(),
                version.getCustomerAccessHours(),
                version.getSubmissionRetentionWeeks(),
                version.getRootElement(),
                version.getCreated(),
                version.getUpdated(),
                version.getPublished(),
                version.getRevoked()
        );
    }

    public FormEntity toFormEntity() {
        return new FormEntity(
                id,
                slug,
                internalTitle,
                developingDepartmentId,
                managingDepartmentId,
                responsibleDepartmentId,
                created,
                updated,
                publishedVersion,
                draftedVersion,
                versionCount
        );
    }

    public FormVersionEntity toVersionEntity() {
        return new FormVersionEntity(
                formId,
                version,
                status,
                publicTitle,
                type,
                legalSupportDepartmentId,
                technicalSupportDepartmentId,
                imprintDepartmentId,
                privacyDepartmentId,
                accessibilityDepartmentId,
                destinationId,
                customerAccessHours,
                submissionRetentionWeeks,
                themeId,
                pdfTemplateKey,
                paymentProviderKey,
                paymentPurpose,
                paymentDescription,
                paymentProducts,
                identityProviders,
                identityVerificationRequired,
                rootElement,
                created,
                updated,
                published,
                revoked
        );
    }

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

    public String getInternalTitle() {
        return internalTitle;
    }

    public FormVersionWithDetailsEntity setInternalTitle(String title) {
        this.internalTitle = title;
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

    public FormStatus getStatus() {
        return status;
    }

    public FormVersionWithDetailsEntity setStatus(FormStatus status) {
        this.status = status;
        return this;
    }

    public FormType getType() {
        return type;
    }

    public FormVersionWithDetailsEntity setType(FormType formType) {
        this.type = formType;
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

    @Override
    public FormVersionWithDetailsEntity clone() {
        try {
            var clone = (FormVersionWithDetailsEntity) super.clone();

            clone.id = this.id;
            clone.slug = this.slug;
            clone.internalTitle = this.internalTitle;
            clone.publicTitle = this.publicTitle;
            clone.developingDepartmentId = this.developingDepartmentId;
            clone.managingDepartmentId = this.managingDepartmentId;
            clone.responsibleDepartmentId = this.responsibleDepartmentId;
            clone.publishedVersion = this.publishedVersion;
            clone.draftedVersion = this.draftedVersion;
            clone.versionCount = this.versionCount;
            clone.formId = this.formId;
            clone.version = this.version;
            clone.status = this.status;
            clone.type = this.type;
            clone.legalSupportDepartmentId = this.legalSupportDepartmentId;
            clone.technicalSupportDepartmentId = this.technicalSupportDepartmentId;
            clone.imprintDepartmentId = this.imprintDepartmentId;
            clone.privacyDepartmentId = this.privacyDepartmentId;
            clone.accessibilityDepartmentId = this.accessibilityDepartmentId;
            clone.destinationId = this.destinationId;
            clone.themeId = this.themeId;
            clone.pdfTemplateKey = this.pdfTemplateKey;
            clone.paymentProviderKey = this.paymentProviderKey;
            clone.paymentPurpose = this.paymentPurpose;
            clone.paymentDescription = this.paymentDescription;
            clone.paymentProducts = this.paymentProducts;
            clone.identityProviders = this.identityProviders;
            clone.identityVerificationRequired = this.identityVerificationRequired;
            clone.customerAccessHours = this.customerAccessHours;
            clone.submissionRetentionWeeks = this.submissionRetentionWeeks;
            clone.rootElement = this.rootElement;
            clone.created = this.created;
            clone.updated = this.updated;
            clone.published = this.published;
            clone.revoked = this.revoked;

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public Integer getVersionCount() {
        return versionCount;
    }

    public FormVersionWithDetailsEntity setVersionCount(Integer versionCount) {
        this.versionCount = versionCount;
        return this;
    }

    // endregion
}
