package de.aivot.GoverBackend.form.entities;

import de.aivot.GoverBackend.core.converters.RootElementConverter;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "v_form_versions_with_details_and_permissions")
@IdClass(VFormVersionWithDetailsAndPermissionsEntityId.class)
public class VFormVersionWithDetailsAndPermissionsEntity {
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
    private List<PaymentProduct> paymentProducts;
    @Column(columnDefinition = "jsonb")
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
    @Id
    private String userId;
    private Boolean formPermissionCreate;
    private Boolean formPermissionRead;
    private Boolean formPermissionEdit;
    private Boolean formPermissionDelete;
    private Boolean formPermissionAnnotate;
    private Boolean formPermissionPublish;

    public FormVersionEntity toFormVersionEntity() {
        return new FormVersionEntity()
                .setFormId(this.formId)
                .setVersion(this.version)
                .setStatus(this.status)
                .setType(this.type)
                .setRootElement(this.rootElement)
                .setCreated(this.created)
                .setUpdated(this.updated)
                .setPublished(this.published)
                .setRevoked(this.revoked);
    }

    public VFormVersionWithDetailsEntity toVFormVersionWithDetailsEntity() {
        return new VFormVersionWithDetailsEntity()
                .setId(this.id)
                .setSlug(this.slug)
                .setInternalTitle(this.internalTitle)
                .setDevelopingDepartmentId(this.developingDepartmentId)
                .setPublishedVersion(this.publishedVersion)
                .setDraftedVersion(this.draftedVersion)
                .setVersionCount(this.versionCount)
                .setFormId(this.formId)
                .setVersion(this.version)
                .setStatus(this.status)
                .setType(this.type)
                .setLegalSupportDepartmentId(this.legalSupportDepartmentId)
                .setTechnicalSupportDepartmentId(this.technicalSupportDepartmentId)
                .setImprintDepartmentId(this.imprintDepartmentId)
                .setPrivacyDepartmentId(this.privacyDepartmentId)
                .setAccessibilityDepartmentId(this.accessibilityDepartmentId)
                .setDestinationId(this.destinationId)
                .setThemeId(this.themeId)
                .setPdfTemplateKey(this.pdfTemplateKey)
                .setPaymentProviderKey(this.paymentProviderKey)
                .setPaymentPurpose(this.paymentPurpose)
                .setPaymentDescription(this.paymentDescription)
                .setPaymentProducts(this.paymentProducts)
                .setIdentityProviders(this.identityProviders)
                .setIdentityVerificationRequired(this.identityVerificationRequired)
                .setCustomerAccessHours(this.customerAccessHours)
                .setSubmissionRetentionWeeks(this.submissionRetentionWeeks)
                .setRootElement(this.rootElement)
                .setCreated(this.created)
                .setUpdated(this.updated)
                .setPublished(this.published)
                .setRevoked(this.revoked)
                .setPublicTitle(this.publicTitle)
                .setManagingDepartmentId(this.managingDepartmentId)
                .setResponsibleDepartmentId(this.responsibleDepartmentId);
    }

    public Integer getId() {
        return id;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setDraftedVersion(Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    public Integer getVersionCount() {
        return versionCount;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setVersionCount(Integer versionCount) {
        this.versionCount = versionCount;
        return this;
    }

    public Integer getFormId() {
        return formId;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public FormStatus getStatus() {
        return status;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setStatus(FormStatus status) {
        this.status = status;
        return this;
    }

    public FormType getType() {
        return type;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setType(FormType type) {
        this.type = type;
        return this;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public UUID getPdfTemplateKey() {
        return pdfTemplateKey;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setPdfTemplateKey(UUID pdfTemplateKey) {
        this.pdfTemplateKey = pdfTemplateKey;
        return this;
    }

    public UUID getPaymentProviderKey() {
        return paymentProviderKey;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setPaymentProviderKey(UUID paymentProviderKey) {
        this.paymentProviderKey = paymentProviderKey;
        return this;
    }

    public String getPaymentPurpose() {
        return paymentPurpose;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setPaymentPurpose(String paymentPurpose) {
        this.paymentPurpose = paymentPurpose;
        return this;
    }

    public String getPaymentDescription() {
        return paymentDescription;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setPaymentDescription(String paymentDescription) {
        this.paymentDescription = paymentDescription;
        return this;
    }

    public List<PaymentProduct> getPaymentProducts() {
        return paymentProducts;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setPaymentProducts(List<PaymentProduct> paymentProducts) {
        this.paymentProducts = paymentProducts;
        return this;
    }

    public List<IdentityProviderLink> getIdentityProviders() {
        return identityProviders;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setIdentityProviders(List<IdentityProviderLink> identityProviders) {
        this.identityProviders = identityProviders;
        return this;
    }

    public Boolean getIdentityVerificationRequired() {
        return identityVerificationRequired;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setIdentityVerificationRequired(Boolean identityVerificationRequired) {
        this.identityVerificationRequired = identityVerificationRequired;
        return this;
    }

    public Integer getCustomerAccessHours() {
        return customerAccessHours;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setCustomerAccessHours(Integer customerAccessHours) {
        this.customerAccessHours = customerAccessHours;
        return this;
    }

    public Integer getSubmissionRetentionWeeks() {
        return submissionRetentionWeeks;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setSubmissionRetentionWeeks(Integer submissionRetentionWeeks) {
        this.submissionRetentionWeeks = submissionRetentionWeeks;
        return this;
    }

    public FormLayoutElement getRootElement() {
        return rootElement;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setRootElement(FormLayoutElement rootElement) {
        this.rootElement = rootElement;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public LocalDateTime getPublished() {
        return published;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setPublished(LocalDateTime published) {
        this.published = published;
        return this;
    }

    public LocalDateTime getRevoked() {
        return revoked;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setRevoked(LocalDateTime revoked) {
        this.revoked = revoked;
        return this;
    }

    public String getPublicTitle() {
        return publicTitle;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setPublicTitle(String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setFormPermissionCreate(Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setFormPermissionRead(Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setFormPermissionEdit(Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setFormPermissionDelete(Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setFormPermissionAnnotate(Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public VFormVersionWithDetailsAndPermissionsEntity setFormPermissionPublish(Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }
}
