package de.aivot.GoverBackend.models.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.aivot.GoverBackend.converters.JacksonRootElementDeserializer;
import de.aivot.GoverBackend.converters.JacksonRootElementSerializer;
import de.aivot.GoverBackend.converters.RootElementConverter;
import de.aivot.GoverBackend.enums.*;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Collection;

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
    private ApplicationStatus status;

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

    @NotNull
    @ColumnDefault("false")
    private Boolean bundIdEnabled = false;

    private BundIdAccessLevel bundIdLevel;

    @NotNull
    @ColumnDefault("false")
    private Boolean bayernIdEnabled = false;

    private BayernIdAccessLevel bayernIdLevel;

    @NotNull
    @ColumnDefault("false")
    private Boolean shIdEnabled = false;

    private SchleswigHolsteinIdAccessLevel shIdLevel;

    @NotNull
    @ColumnDefault("false")
    private Boolean mukEnabled = false;

    private MukAccessLevel mukLevel;

    @Column(length = 36)
    private String pdfBodyTemplateKey;

    @JdbcTypeCode(SqlTypes.JSON)
    private Collection<PaymentProduct> products;

    @Column(length = 27)
    private String paymentPurpose;

    @Column(length = 250)
    private String paymentDescription;

    @Column(length = 36)
    private String paymentOriginatorId;

    @Column(length = 36)
    private String paymentEndpointId;

    @Column(length = 12)
    private PaymentProvider paymentProvider;

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
     * @return
     * @deprecated
     */
    @Deprecated
    public String getApplicationTitle() {
        return getFormTitle();
    }

    public String getFormTitle() {
        if (root.getHeadline() != null) {
            return root.getHeadline();
        }

        if (title != null) {
            return title;
        }

        return slug;
    }

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

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
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

    public Boolean getBundIdEnabled() {
        return bundIdEnabled;
    }

    public void setBundIdEnabled(Boolean bundIdEnabled) {
        this.bundIdEnabled = bundIdEnabled;
    }

    public Boolean getBayernIdEnabled() {
        return bayernIdEnabled;
    }

    public void setBayernIdEnabled(Boolean bayernIdEnabled) {
        this.bayernIdEnabled = bayernIdEnabled;
    }

    public Boolean getMukEnabled() {
        return mukEnabled;
    }

    public void setMukEnabled(Boolean mukEnabled) {
        this.mukEnabled = mukEnabled;
    }

    public BayernIdAccessLevel getBayernIdLevel() {
        return bayernIdLevel;
    }

    public void setBayernIdLevel(BayernIdAccessLevel bayernIdLevel) {
        this.bayernIdLevel = bayernIdLevel;
    }

    public BundIdAccessLevel getBundIdLevel() {
        return bundIdLevel;
    }

    public void setBundIdLevel(BundIdAccessLevel bundIdLevel) {
        this.bundIdLevel = bundIdLevel;
    }

    public Boolean getShIdEnabled() {
        return shIdEnabled;
    }

    public void setShIdEnabled(Boolean shIdEnabled) {
        this.shIdEnabled = shIdEnabled;
    }

    public SchleswigHolsteinIdAccessLevel getShIdLevel() {
        return shIdLevel;
    }

    public void setShIdLevel(SchleswigHolsteinIdAccessLevel shIdLevel) {
        this.shIdLevel = shIdLevel;
    }

    public MukAccessLevel getMukLevel() {
        return mukLevel;
    }

    public void setMukLevel(MukAccessLevel mukLevel) {
        this.mukLevel = mukLevel;
    }

    public String getPdfBodyTemplateKey() {
        return pdfBodyTemplateKey;
    }

    public void setPdfBodyTemplateKey(String pdfBodyTemplateKey) {
        this.pdfBodyTemplateKey = pdfBodyTemplateKey;
    }

    public String getPaymentEndpointId() {
        return paymentEndpointId;
    }

    public void setPaymentEndpointId(String paymentEndpointId) {
        this.paymentEndpointId = paymentEndpointId;
    }

    public String getPaymentOriginatorId() {
        return paymentOriginatorId;
    }

    public void setPaymentOriginatorId(String paymentOriginatorId) {
        this.paymentOriginatorId = paymentOriginatorId;
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

    public PaymentProvider getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(PaymentProvider paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getPaymentDescription() {
        return paymentDescription;
    }

    public void setPaymentDescription(String paymentDescription) {
        this.paymentDescription = paymentDescription;
    }


    // endregion
}
