package de.aivot.GoverBackend.form.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.aivot.GoverBackend.core.converters.JacksonRootElementDeserializer;
import de.aivot.GoverBackend.core.converters.JacksonRootElementSerializer;
import de.aivot.GoverBackend.core.converters.RootElementConverter;
import de.aivot.GoverBackend.enums.*;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Collection;

@Entity
@Table(name = "forms_with_memberships")
@IdClass(FormWithMembershipId.class)
public class FormWithMembership {
    @Id
    private Integer id;
    private String slug;
    private String version;
    private String title;
    private FormStatus status;
    private FormType type;
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
    private Integer developingDepartmentId;
    private Integer managingDepartmentId;
    private Integer responsibleDepartmentId;
    private Integer themeId;
    private LocalDateTime created;
    private LocalDateTime updated;
    private Integer customerAccessHours;
    private Integer submissionDeletionWeeks;
    private Boolean bundIdEnabled = false;
    private BundIdAccessLevel bundIdLevel;
    private Boolean bayernIdEnabled = false;
    private BayernIdAccessLevel bayernIdLevel;
    private Boolean shIdEnabled = false;
    private SchleswigHolsteinIdAccessLevel shIdLevel;
    private Boolean mukEnabled = false;
    private MukAccessLevel mukLevel;
    private String pdfBodyTemplateKey;
    @JdbcTypeCode(SqlTypes.JSON)
    private Collection<PaymentProduct> products;
    private String paymentPurpose;
    private String paymentDescription;
    private String paymentProvider;
    @Id
    private String userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private String userFullName;
    private Boolean userEnabled;
    private Boolean userVerified;
    private Boolean userGlobalAdmin;
    private Boolean userDeletedInIdp;
    private Boolean userIsDeveloper;
    private Boolean userIsManager;
    private Boolean userIsResponsible;

    public Integer getId() {
        return id;
    }

    public FormWithMembership setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public FormWithMembership setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public FormWithMembership setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public FormWithMembership setTitle(String title) {
        this.title = title;
        return this;
    }

    public FormStatus getStatus() {
        return status;
    }

    public FormWithMembership setStatus(FormStatus status) {
        this.status = status;
        return this;
    }

    public FormType getType() {
        return type;
    }

    public FormWithMembership setType(FormType type) {
        this.type = type;
        return this;
    }

    public RootElement getRoot() {
        return root;
    }

    public FormWithMembership setRoot(RootElement root) {
        this.root = root;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public FormWithMembership setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public FormWithMembership setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public FormWithMembership setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public FormWithMembership setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public FormWithMembership setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public FormWithMembership setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public FormWithMembership setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public FormWithMembership setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public FormWithMembership setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public FormWithMembership setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public FormWithMembership setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public FormWithMembership setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public Integer getCustomerAccessHours() {
        return customerAccessHours;
    }

    public FormWithMembership setCustomerAccessHours(Integer customerAccessHours) {
        this.customerAccessHours = customerAccessHours;
        return this;
    }

    public Integer getSubmissionDeletionWeeks() {
        return submissionDeletionWeeks;
    }

    public FormWithMembership setSubmissionDeletionWeeks(Integer submissionDeletionWeeks) {
        this.submissionDeletionWeeks = submissionDeletionWeeks;
        return this;
    }

    public Boolean getBundIdEnabled() {
        return bundIdEnabled;
    }

    public FormWithMembership setBundIdEnabled(Boolean bundIdEnabled) {
        this.bundIdEnabled = bundIdEnabled;
        return this;
    }

    public BundIdAccessLevel getBundIdLevel() {
        return bundIdLevel;
    }

    public FormWithMembership setBundIdLevel(BundIdAccessLevel bundIdLevel) {
        this.bundIdLevel = bundIdLevel;
        return this;
    }

    public Boolean getBayernIdEnabled() {
        return bayernIdEnabled;
    }

    public FormWithMembership setBayernIdEnabled(Boolean bayernIdEnabled) {
        this.bayernIdEnabled = bayernIdEnabled;
        return this;
    }

    public BayernIdAccessLevel getBayernIdLevel() {
        return bayernIdLevel;
    }

    public FormWithMembership setBayernIdLevel(BayernIdAccessLevel bayernIdLevel) {
        this.bayernIdLevel = bayernIdLevel;
        return this;
    }

    public Boolean getShIdEnabled() {
        return shIdEnabled;
    }

    public FormWithMembership setShIdEnabled(Boolean shIdEnabled) {
        this.shIdEnabled = shIdEnabled;
        return this;
    }

    public SchleswigHolsteinIdAccessLevel getShIdLevel() {
        return shIdLevel;
    }

    public FormWithMembership setShIdLevel(SchleswigHolsteinIdAccessLevel shIdLevel) {
        this.shIdLevel = shIdLevel;
        return this;
    }

    public Boolean getMukEnabled() {
        return mukEnabled;
    }

    public FormWithMembership setMukEnabled(Boolean mukEnabled) {
        this.mukEnabled = mukEnabled;
        return this;
    }

    public MukAccessLevel getMukLevel() {
        return mukLevel;
    }

    public FormWithMembership setMukLevel(MukAccessLevel mukLevel) {
        this.mukLevel = mukLevel;
        return this;
    }

    public String getPdfBodyTemplateKey() {
        return pdfBodyTemplateKey;
    }

    public FormWithMembership setPdfBodyTemplateKey(String pdfBodyTemplateKey) {
        this.pdfBodyTemplateKey = pdfBodyTemplateKey;
        return this;
    }

    public Collection<PaymentProduct> getProducts() {
        return products;
    }

    public FormWithMembership setProducts(Collection<PaymentProduct> products) {
        this.products = products;
        return this;
    }

    public String getPaymentPurpose() {
        return paymentPurpose;
    }

    public FormWithMembership setPaymentPurpose(String paymentPurpose) {
        this.paymentPurpose = paymentPurpose;
        return this;
    }

    public String getPaymentDescription() {
        return paymentDescription;
    }

    public FormWithMembership setPaymentDescription(String paymentDescription) {
        this.paymentDescription = paymentDescription;
        return this;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public FormWithMembership setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public FormWithMembership setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public FormWithMembership setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public FormWithMembership setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
        return this;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public FormWithMembership setUserLastName(String userLastName) {
        this.userLastName = userLastName;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public FormWithMembership setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public FormWithMembership setUserEnabled(Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    public Boolean getUserVerified() {
        return userVerified;
    }

    public FormWithMembership setUserVerified(Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    public Boolean getUserGlobalAdmin() {
        return userGlobalAdmin;
    }

    public FormWithMembership setUserGlobalAdmin(Boolean userGlobalAdmin) {
        this.userGlobalAdmin = userGlobalAdmin;
        return this;
    }

    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public FormWithMembership setUserDeletedInIdp(Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }

    public Boolean getUserIsDeveloper() {
        return userIsDeveloper;
    }

    public FormWithMembership setUserIsDeveloper(Boolean userIsDeveloper) {
        this.userIsDeveloper = userIsDeveloper;
        return this;
    }

    public Boolean getUserIsManager() {
        return userIsManager;
    }

    public FormWithMembership setUserIsManager(Boolean userIsManager) {
        this.userIsManager = userIsManager;
        return this;
    }

    public Boolean getUserIsResponsible() {
        return userIsResponsible;
    }

    public FormWithMembership setUserIsResponsible(Boolean userIsResponsible) {
        this.userIsResponsible = userIsResponsible;
        return this;
    }
}
