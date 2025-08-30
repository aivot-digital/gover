package de.aivot.GoverBackend.form.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
import org.json.JSONPropertyIgnore;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;


public class XForm implements Cloneable {
    private Integer id;
    private String slug;
    private String version;
    private String title;
    private FormStatus status;
    private FormType type;
    private RootElement root;
    private Integer destinationId;
    private Integer legalSupportDepartmentId;
    private Integer technicalSupportDepartmentId;
    private Integer imprintDepartmentId;
    private Integer privacyDepartmentId;
    private Integer accessibilityDepartmentId;
    private Integer developingDepartmentId;
    private Integer managingDepartmentId;
    private Integer responsibleDepartmentId;
    private Integer themeId;
    private LocalDateTime created;
    private LocalDateTime updated;
    private Integer customerAccessHours;
    private Integer submissionDeletionWeeks;
    private String pdfBodyTemplateKey;
    private Collection<PaymentProduct> products;
    private String paymentPurpose;
    private String paymentDescription;
    private UUID paymentProvider;
    private Boolean identityRequired = false;
    private List<IdentityProviderLink> identityProviders = new LinkedList<>();

    // region Utils

    /**
     * @deprecated Use getFormTitle() instead
     */
    @Deprecated
    @JsonIgnore
    @JSONPropertyIgnore
    public String getApplicationTitle() {
        return getFormTitle();
    }

    @JsonIgnore
    @JSONPropertyIgnore
    public String getFormTitle() {
        if (root.getHeadline() != null) {
            return root.getHeadline();
        }

        if (title != null) {
            return title;
        }

        return slug;
    }

    @JsonIgnore
    @JSONPropertyIgnore
    public Integer getRelevantDepartmentId() {
        if (managingDepartmentId != null) {
            return managingDepartmentId;
        } else if (responsibleDepartmentId != null) {
            return responsibleDepartmentId;
        } else {
            return developingDepartmentId;
        }
    }

    // endregion


    // region Getters & Setters

    public Integer getId() {
        return id;
    }

    public XForm setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public XForm setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public XForm setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public XForm setTitle(String title) {
        this.title = title;
        return this;
    }

    public XFormStatus getStatus() {
        return status;
    }

    public XForm setStatus(FormStatus status) {
        this.status = status;
        return this;
    }

    public XFormType getType() {
        return type;
    }

    public XForm setType(FormType type) {
        this.type = type;
        return this;
    }

    public RootElement getRoot() {
        return root;
    }

    public XForm setRoot(RootElement root) {
        this.root = root;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public XForm setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public XForm setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public XForm setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public XForm setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public XForm setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public XForm setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public XForm setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public XForm setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public XForm setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public XForm setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public XForm setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public XForm setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public Integer getCustomerAccessHours() {
        return customerAccessHours;
    }

    public XForm setCustomerAccessHours(Integer customerAccessHours) {
        this.customerAccessHours = customerAccessHours;
        return this;
    }

    public Integer getSubmissionDeletionWeeks() {
        return submissionDeletionWeeks;
    }

    public XForm setSubmissionDeletionWeeks(Integer submissionDeletionWeeks) {
        this.submissionDeletionWeeks = submissionDeletionWeeks;
        return this;
    }

    public String getPdfBodyTemplateKey() {
        return pdfBodyTemplateKey;
    }

    public XForm setPdfBodyTemplateKey(String pdfBodyTemplateKey) {
        this.pdfBodyTemplateKey = pdfBodyTemplateKey;
        return this;
    }

    public Collection<PaymentProduct> getProducts() {
        return products;
    }

    public XForm setProducts(Collection<PaymentProduct> products) {
        this.products = products;
        return this;
    }

    public String getPaymentPurpose() {
        return paymentPurpose;
    }

    public XForm setPaymentPurpose(String paymentPurpose) {
        this.paymentPurpose = paymentPurpose;
        return this;
    }

    public String getPaymentDescription() {
        return paymentDescription;
    }

    public XForm setPaymentDescription(String paymentDescription) {
        this.paymentDescription = paymentDescription;
        return this;
    }

    public UUID getPaymentProvider() {
        return paymentProvider;
    }

    public XForm setPaymentProvider(UUID paymentProvider) {
        this.paymentProvider = paymentProvider;
        return this;
    }

    public Boolean getIdentityRequired() {
        return identityRequired;
    }

    public XForm setIdentityRequired(Boolean identityRequired) {
        this.identityRequired = identityRequired;
        return this;
    }

    public List<IdentityProviderLink> getIdentityProviders() {
        return identityProviders;
    }

    public XForm setIdentityProviders(List<IdentityProviderLink> identityProviders) {
        this.identityProviders = identityProviders;
        return this;
    }

    // endregion
}
