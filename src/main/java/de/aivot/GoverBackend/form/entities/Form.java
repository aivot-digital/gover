package de.aivot.GoverBackend.form.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.aivot.GoverBackend.core.converters.JacksonRootElementDeserializer;
import de.aivot.GoverBackend.core.converters.JacksonRootElementSerializer;
import de.aivot.GoverBackend.core.converters.RootElementConverter;
import de.aivot.GoverBackend.enums.*;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.identity.converters.IdentityProviderLinksConverter;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.json.JSONPropertyIgnore;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name = "forms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"slug", "version"})
})
public class Form {
    @Id
    @Column(name = "id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "applications_id_seq")
    @SequenceGenerator(name = "applications_id_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @Column(length = 255)
    @NotBlank(message = "Slug cannot be blank")
    private String slug;

    @NotNull
    @Column(length = 11)
    @NotBlank(message = "Version cannot be blank")
    private String version;

    @NotNull
    @Column(length = 96)
    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotNull
    @ColumnDefault("0")
    private FormStatus status;

    @NotNull
    @ColumnDefault("0")
    private FormType type;

    @NotNull
    @Convert(converter = RootElementConverter.class)
    @JsonSerialize(converter = JacksonRootElementSerializer.class)
    @JsonDeserialize(converter = JacksonRootElementDeserializer.class)
    @Column(columnDefinition = "jsonb")
    private RootElement root;

    private Integer destinationId;

    private Integer legalSupportDepartmentId;

    private Integer technicalSupportDepartmentId;

    private Integer imprintDepartmentId;

    private Integer privacyDepartmentId;

    private Integer accessibilityDepartmentId;

    @NotNull
    private Integer developingDepartmentId;

    private Integer managingDepartmentId;

    private Integer responsibleDepartmentId;

    private Integer themeId;

    @NotNull
    private LocalDateTime created;

    @NotNull
    private LocalDateTime updated;

    private Integer customerAccessHours;

    private Integer submissionDeletionWeeks;

    @Column(length = 36)
    private String pdfBodyTemplateKey;

    @JdbcTypeCode(SqlTypes.JSON)
    private Collection<PaymentProduct> products;

    @Column(length = 27)
    private String paymentPurpose;

    @Column(length = 250)
    private String paymentDescription;

    @Column(length = 36)
    private String paymentProvider;

    @Column(columnDefinition = "boolean")
    private Boolean identityRequired = false;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = IdentityProviderLinksConverter.class)
    private List<IdentityProviderLink> identityProviders = new LinkedList<>();

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

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public FormStatus getStatus() {
        return status;
    }

    public void setStatus(FormStatus status) {
        this.status = status;
    }

    public FormType getType() {
        return type;
    }

    public Form setType(FormType type) {
        this.type = type != null ? type : FormType.Public;
        return this;
    }

    public RootElement getRoot() {
        return root;
    }

    public void setRoot(RootElement root) {
        this.root = root;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public void setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public void setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public void setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public void setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public void setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public void setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public void setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public void setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public void setThemeId(Integer themeId) {
        this.themeId = themeId;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public Integer getCustomerAccessHours() {
        return customerAccessHours;
    }

    public void setCustomerAccessHours(Integer customerAccessHours) {
        this.customerAccessHours = customerAccessHours;
    }

    public Integer getSubmissionDeletionWeeks() {
        return submissionDeletionWeeks;
    }

    public void setSubmissionDeletionWeeks(Integer submissionDeletionWeeks) {
        this.submissionDeletionWeeks = submissionDeletionWeeks;
    }

    public String getPdfBodyTemplateKey() {
        return pdfBodyTemplateKey;
    }

    public void setPdfBodyTemplateKey(String pdfBodyTemplateKey) {
        this.pdfBodyTemplateKey = pdfBodyTemplateKey;
    }

    public String getPaymentPurpose() {
        return paymentPurpose;
    }

    public void setPaymentPurpose(String paymentPurpose) {
        this.paymentPurpose = paymentPurpose;
    }

    public Collection<PaymentProduct> getProducts() {
        return products;
    }

    public void setProducts(Collection<PaymentProduct> products) {
        this.products = products;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getPaymentDescription() {
        return paymentDescription;
    }

    public void setPaymentDescription(String paymentDescription) {
        this.paymentDescription = paymentDescription;
    }

    public Boolean getIdentityRequired() {
        return identityRequired;
    }

    public Form setIdentityRequired(Boolean identityRequired) {
        this.identityRequired = identityRequired;
        return this;
    }

    public List<IdentityProviderLink> getIdentityProviders() {
        return identityProviders;
    }

    public Form setIdentityProviders(List<IdentityProviderLink> identityProviders) {
        this.identityProviders = identityProviders;
        return this;
    }

    // endregion
}
