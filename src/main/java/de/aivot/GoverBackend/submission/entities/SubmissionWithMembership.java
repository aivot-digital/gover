package de.aivot.GoverBackend.submission.entities;

import de.aivot.GoverBackend.core.converters.ElementDataConverter;
import de.aivot.GoverBackend.core.converters.RootElementConverter;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.converters.IdentityProviderLinksConverter;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
import de.aivot.GoverBackend.payment.converters.PaymentProductsConverter;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "submissions_with_memberships")
@IdClass(SubmissionWithMembershipId.class)
public class SubmissionWithMembership {
    @Id
    private String id;
    @Id
    private Integer formId;
    private LocalDateTime created;
    private String assigneeId;
    private LocalDateTime archived;
    private String fileNumber;
    private Integer destinationId;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = ElementDataConverter.class)
    private ElementData customerInput;

    private Boolean destinationSuccess;
    private Boolean isTestSubmission;
    private Boolean copySent;
    private Integer copyTries;
    private Integer reviewScore;
    private String destinationResult;
    private LocalDateTime destinationTimestamp;

    @Column(columnDefinition = "int4")
    private SubmissionStatus status;

    private LocalDateTime updated;
    private String paymentTransactionKey;

    @Id
    @Column(columnDefinition = "int2")
    private Integer formVersion;

    private String formSlug;

    private String formInternalTitle;
    private String formPublicTitle;

    private Integer formDevelopingDepartmentId;
    private Integer formManagingDepartmentId;
    private Integer formResponsibleDepartmentId;

    private LocalDateTime formCreated;
    private LocalDateTime formUpdated;

    @Column(columnDefinition = "int2")
    private Integer formPublishedVersion;

    @Column(columnDefinition = "int2")
    private Integer formDraftedVersion;

    private FormStatus formStatus;
    private FormType formType;

    private Integer formLegalSupportDepartmentId;
    private Integer formTechnicalSupportDepartmentId;
    private Integer formImprintDepartmentId;
    private Integer formPrivacyDepartmentId;
    private Integer formAccessibilityDepartmentId;

    @Column(columnDefinition = "int2")
    private Integer formCustomerAccessHours;

    @Column(columnDefinition = "int2")
    private Integer formSubmissionRetentionWeeks;

    private Integer formThemeId;

    private UUID formPdfTemplateKey;

    private UUID formPaymentProviderKey;
    private String formPaymentPurpose;
    private String formPaymentDescription;

    @Convert(converter = PaymentProductsConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<PaymentProduct> formPaymentProducts;

    @Convert(converter = IdentityProviderLinksConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<IdentityProviderLink> formIdentityProviders;

    private Boolean formIdentityVerificationRequired;

    private Integer formDestinationId;

    @Convert(converter = RootElementConverter.class)
    @Column(columnDefinition = "jsonb")
    private RootElement formRootElement;

    private LocalDateTime formVersionPublished;
    private LocalDateTime formVersionRevoked;

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

    public Submission asSubmission() {
        return new Submission()
                .setId(id)
                .setFormId(formId)
                .setFormVersion(formVersion)
                .setCreated(created)
                .setUpdated(updated)
                .setStatus(status)
                .setAssigneeId(assigneeId)
                .setFileNumber(fileNumber)
                .setArchived(archived)
                .setCustomerInput(customerInput)
                .setDestinationId(destinationId)
                .setDestinationSuccess(destinationSuccess)
                .setDestinationResult(destinationResult)
                .setDestinationTimestamp(destinationTimestamp)
                .setIsTestSubmission(isTestSubmission)
                .setCopySent(copySent)
                .setCopyTries(copyTries)
                .setReviewScore(reviewScore)
                .setPaymentTransactionKey(paymentTransactionKey);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        SubmissionWithMembership that = (SubmissionWithMembership) object;
        return Objects.equals(id, that.id) && Objects.equals(formId, that.formId) && Objects.equals(created, that.created) && Objects.equals(assigneeId, that.assigneeId) && Objects.equals(archived, that.archived) && Objects.equals(fileNumber, that.fileNumber) && Objects.equals(destinationId, that.destinationId) && Objects.equals(customerInput, that.customerInput) && Objects.equals(destinationSuccess, that.destinationSuccess) && Objects.equals(isTestSubmission, that.isTestSubmission) && Objects.equals(copySent, that.copySent) && Objects.equals(copyTries, that.copyTries) && Objects.equals(reviewScore, that.reviewScore) && Objects.equals(destinationResult, that.destinationResult) && Objects.equals(destinationTimestamp, that.destinationTimestamp) && status == that.status && Objects.equals(updated, that.updated) && Objects.equals(paymentTransactionKey, that.paymentTransactionKey) && Objects.equals(formVersion, that.formVersion) && Objects.equals(formSlug, that.formSlug) && Objects.equals(formInternalTitle, that.formInternalTitle) && Objects.equals(formPublicTitle, that.formPublicTitle) && Objects.equals(formDevelopingDepartmentId, that.formDevelopingDepartmentId) && Objects.equals(formManagingDepartmentId, that.formManagingDepartmentId) && Objects.equals(formResponsibleDepartmentId, that.formResponsibleDepartmentId) && Objects.equals(formCreated, that.formCreated) && Objects.equals(formUpdated, that.formUpdated) && Objects.equals(formPublishedVersion, that.formPublishedVersion) && Objects.equals(formDraftedVersion, that.formDraftedVersion) && formStatus == that.formStatus && formType == that.formType && Objects.equals(formLegalSupportDepartmentId, that.formLegalSupportDepartmentId) && Objects.equals(formTechnicalSupportDepartmentId, that.formTechnicalSupportDepartmentId) && Objects.equals(formImprintDepartmentId, that.formImprintDepartmentId) && Objects.equals(formPrivacyDepartmentId, that.formPrivacyDepartmentId) && Objects.equals(formAccessibilityDepartmentId, that.formAccessibilityDepartmentId) && Objects.equals(formCustomerAccessHours, that.formCustomerAccessHours) && Objects.equals(formSubmissionRetentionWeeks, that.formSubmissionRetentionWeeks) && Objects.equals(formThemeId, that.formThemeId) && Objects.equals(formPdfTemplateKey, that.formPdfTemplateKey) && Objects.equals(formPaymentProviderKey, that.formPaymentProviderKey) && Objects.equals(formPaymentPurpose, that.formPaymentPurpose) && Objects.equals(formPaymentDescription, that.formPaymentDescription) && Objects.equals(formPaymentProducts, that.formPaymentProducts) && Objects.equals(formIdentityProviders, that.formIdentityProviders) && Objects.equals(formIdentityVerificationRequired, that.formIdentityVerificationRequired) && Objects.equals(formDestinationId, that.formDestinationId) && Objects.equals(formRootElement, that.formRootElement) && Objects.equals(formVersionPublished, that.formVersionPublished) && Objects.equals(formVersionRevoked, that.formVersionRevoked) && Objects.equals(userId, that.userId) && Objects.equals(userEmail, that.userEmail) && Objects.equals(userFirstName, that.userFirstName) && Objects.equals(userLastName, that.userLastName) && Objects.equals(userFullName, that.userFullName) && Objects.equals(userEnabled, that.userEnabled) && Objects.equals(userVerified, that.userVerified) && Objects.equals(userGlobalAdmin, that.userGlobalAdmin) && Objects.equals(userDeletedInIdp, that.userDeletedInIdp) && Objects.equals(userIsDeveloper, that.userIsDeveloper) && Objects.equals(userIsManager, that.userIsManager) && Objects.equals(userIsResponsible, that.userIsResponsible);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(formId);
        result = 31 * result + Objects.hashCode(created);
        result = 31 * result + Objects.hashCode(assigneeId);
        result = 31 * result + Objects.hashCode(archived);
        result = 31 * result + Objects.hashCode(fileNumber);
        result = 31 * result + Objects.hashCode(destinationId);
        result = 31 * result + Objects.hashCode(customerInput);
        result = 31 * result + Objects.hashCode(destinationSuccess);
        result = 31 * result + Objects.hashCode(isTestSubmission);
        result = 31 * result + Objects.hashCode(copySent);
        result = 31 * result + Objects.hashCode(copyTries);
        result = 31 * result + Objects.hashCode(reviewScore);
        result = 31 * result + Objects.hashCode(destinationResult);
        result = 31 * result + Objects.hashCode(destinationTimestamp);
        result = 31 * result + Objects.hashCode(status);
        result = 31 * result + Objects.hashCode(updated);
        result = 31 * result + Objects.hashCode(paymentTransactionKey);
        result = 31 * result + Objects.hashCode(formVersion);
        result = 31 * result + Objects.hashCode(formSlug);
        result = 31 * result + Objects.hashCode(formInternalTitle);
        result = 31 * result + Objects.hashCode(formPublicTitle);
        result = 31 * result + Objects.hashCode(formDevelopingDepartmentId);
        result = 31 * result + Objects.hashCode(formManagingDepartmentId);
        result = 31 * result + Objects.hashCode(formResponsibleDepartmentId);
        result = 31 * result + Objects.hashCode(formCreated);
        result = 31 * result + Objects.hashCode(formUpdated);
        result = 31 * result + Objects.hashCode(formPublishedVersion);
        result = 31 * result + Objects.hashCode(formDraftedVersion);
        result = 31 * result + Objects.hashCode(formStatus);
        result = 31 * result + Objects.hashCode(formType);
        result = 31 * result + Objects.hashCode(formLegalSupportDepartmentId);
        result = 31 * result + Objects.hashCode(formTechnicalSupportDepartmentId);
        result = 31 * result + Objects.hashCode(formImprintDepartmentId);
        result = 31 * result + Objects.hashCode(formPrivacyDepartmentId);
        result = 31 * result + Objects.hashCode(formAccessibilityDepartmentId);
        result = 31 * result + Objects.hashCode(formCustomerAccessHours);
        result = 31 * result + Objects.hashCode(formSubmissionRetentionWeeks);
        result = 31 * result + Objects.hashCode(formThemeId);
        result = 31 * result + Objects.hashCode(formPdfTemplateKey);
        result = 31 * result + Objects.hashCode(formPaymentProviderKey);
        result = 31 * result + Objects.hashCode(formPaymentPurpose);
        result = 31 * result + Objects.hashCode(formPaymentDescription);
        result = 31 * result + Objects.hashCode(formPaymentProducts);
        result = 31 * result + Objects.hashCode(formIdentityProviders);
        result = 31 * result + Objects.hashCode(formIdentityVerificationRequired);
        result = 31 * result + Objects.hashCode(formDestinationId);
        result = 31 * result + Objects.hashCode(formRootElement);
        result = 31 * result + Objects.hashCode(formVersionPublished);
        result = 31 * result + Objects.hashCode(formVersionRevoked);
        result = 31 * result + Objects.hashCode(userId);
        result = 31 * result + Objects.hashCode(userEmail);
        result = 31 * result + Objects.hashCode(userFirstName);
        result = 31 * result + Objects.hashCode(userLastName);
        result = 31 * result + Objects.hashCode(userFullName);
        result = 31 * result + Objects.hashCode(userEnabled);
        result = 31 * result + Objects.hashCode(userVerified);
        result = 31 * result + Objects.hashCode(userGlobalAdmin);
        result = 31 * result + Objects.hashCode(userDeletedInIdp);
        result = 31 * result + Objects.hashCode(userIsDeveloper);
        result = 31 * result + Objects.hashCode(userIsManager);
        result = 31 * result + Objects.hashCode(userIsResponsible);
        return result;
    }

    public String getId() {
        return id;
    }

    public SubmissionWithMembership setId(String id) {
        this.id = id;
        return this;
    }

    public Integer getFormId() {
        return formId;
    }

    public SubmissionWithMembership setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public SubmissionWithMembership setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public SubmissionWithMembership setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    public LocalDateTime getArchived() {
        return archived;
    }

    public SubmissionWithMembership setArchived(LocalDateTime archived) {
        this.archived = archived;
        return this;
    }

    public String getFileNumber() {
        return fileNumber;
    }

    public SubmissionWithMembership setFileNumber(String fileNumber) {
        this.fileNumber = fileNumber;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public SubmissionWithMembership setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public ElementData getCustomerInput() {
        return customerInput;
    }

    public SubmissionWithMembership setCustomerInput(ElementData customerInput) {
        this.customerInput = customerInput;
        return this;
    }

    public Boolean getDestinationSuccess() {
        return destinationSuccess;
    }

    public SubmissionWithMembership setDestinationSuccess(Boolean destinationSuccess) {
        this.destinationSuccess = destinationSuccess;
        return this;
    }

    public Boolean getTestSubmission() {
        return isTestSubmission;
    }

    public SubmissionWithMembership setTestSubmission(Boolean testSubmission) {
        isTestSubmission = testSubmission;
        return this;
    }

    public Boolean getCopySent() {
        return copySent;
    }

    public SubmissionWithMembership setCopySent(Boolean copySent) {
        this.copySent = copySent;
        return this;
    }

    public Integer getCopyTries() {
        return copyTries;
    }

    public SubmissionWithMembership setCopyTries(Integer copyTries) {
        this.copyTries = copyTries;
        return this;
    }

    public Integer getReviewScore() {
        return reviewScore;
    }

    public SubmissionWithMembership setReviewScore(Integer reviewScore) {
        this.reviewScore = reviewScore;
        return this;
    }

    public String getDestinationResult() {
        return destinationResult;
    }

    public SubmissionWithMembership setDestinationResult(String destinationResult) {
        this.destinationResult = destinationResult;
        return this;
    }

    public LocalDateTime getDestinationTimestamp() {
        return destinationTimestamp;
    }

    public SubmissionWithMembership setDestinationTimestamp(LocalDateTime destinationTimestamp) {
        this.destinationTimestamp = destinationTimestamp;
        return this;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public SubmissionWithMembership setStatus(SubmissionStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public SubmissionWithMembership setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public String getPaymentTransactionKey() {
        return paymentTransactionKey;
    }

    public SubmissionWithMembership setPaymentTransactionKey(String paymentTransactionKey) {
        this.paymentTransactionKey = paymentTransactionKey;
        return this;
    }

    public Integer getFormVersion() {
        return formVersion;
    }

    public SubmissionWithMembership setFormVersion(Integer formVersion) {
        this.formVersion = formVersion;
        return this;
    }

    public String getFormSlug() {
        return formSlug;
    }

    public SubmissionWithMembership setFormSlug(String formSlug) {
        this.formSlug = formSlug;
        return this;
    }

    public String getFormInternalTitle() {
        return formInternalTitle;
    }

    public SubmissionWithMembership setFormInternalTitle(String formInternalTitle) {
        this.formInternalTitle = formInternalTitle;
        return this;
    }

    public String getFormPublicTitle() {
        return formPublicTitle;
    }

    public SubmissionWithMembership setFormPublicTitle(String formPublicTitle) {
        this.formPublicTitle = formPublicTitle;
        return this;
    }

    public Integer getFormDevelopingDepartmentId() {
        return formDevelopingDepartmentId;
    }

    public SubmissionWithMembership setFormDevelopingDepartmentId(Integer formDevelopingDepartmentId) {
        this.formDevelopingDepartmentId = formDevelopingDepartmentId;
        return this;
    }

    public Integer getFormManagingDepartmentId() {
        return formManagingDepartmentId;
    }

    public SubmissionWithMembership setFormManagingDepartmentId(Integer formManagingDepartmentId) {
        this.formManagingDepartmentId = formManagingDepartmentId;
        return this;
    }

    public Integer getFormResponsibleDepartmentId() {
        return formResponsibleDepartmentId;
    }

    public SubmissionWithMembership setFormResponsibleDepartmentId(Integer formResponsibleDepartmentId) {
        this.formResponsibleDepartmentId = formResponsibleDepartmentId;
        return this;
    }

    public LocalDateTime getFormCreated() {
        return formCreated;
    }

    public SubmissionWithMembership setFormCreated(LocalDateTime formCreated) {
        this.formCreated = formCreated;
        return this;
    }

    public LocalDateTime getFormUpdated() {
        return formUpdated;
    }

    public SubmissionWithMembership setFormUpdated(LocalDateTime formUpdated) {
        this.formUpdated = formUpdated;
        return this;
    }

    public Integer getFormPublishedVersion() {
        return formPublishedVersion;
    }

    public SubmissionWithMembership setFormPublishedVersion(Integer formPublishedVersion) {
        this.formPublishedVersion = formPublishedVersion;
        return this;
    }

    public Integer getFormDraftedVersion() {
        return formDraftedVersion;
    }

    public SubmissionWithMembership setFormDraftedVersion(Integer formDraftedVersion) {
        this.formDraftedVersion = formDraftedVersion;
        return this;
    }

    public FormStatus getFormStatus() {
        return formStatus;
    }

    public SubmissionWithMembership setFormStatus(FormStatus formStatus) {
        this.formStatus = formStatus;
        return this;
    }

    public FormType getFormType() {
        return formType;
    }

    public SubmissionWithMembership setFormType(FormType formType) {
        this.formType = formType;
        return this;
    }

    public Integer getFormLegalSupportDepartmentId() {
        return formLegalSupportDepartmentId;
    }

    public SubmissionWithMembership setFormLegalSupportDepartmentId(Integer formLegalSupportDepartmentId) {
        this.formLegalSupportDepartmentId = formLegalSupportDepartmentId;
        return this;
    }

    public Integer getFormTechnicalSupportDepartmentId() {
        return formTechnicalSupportDepartmentId;
    }

    public SubmissionWithMembership setFormTechnicalSupportDepartmentId(Integer formTechnicalSupportDepartmentId) {
        this.formTechnicalSupportDepartmentId = formTechnicalSupportDepartmentId;
        return this;
    }

    public Integer getFormImprintDepartmentId() {
        return formImprintDepartmentId;
    }

    public SubmissionWithMembership setFormImprintDepartmentId(Integer formImprintDepartmentId) {
        this.formImprintDepartmentId = formImprintDepartmentId;
        return this;
    }

    public Integer getFormPrivacyDepartmentId() {
        return formPrivacyDepartmentId;
    }

    public SubmissionWithMembership setFormPrivacyDepartmentId(Integer formPrivacyDepartmentId) {
        this.formPrivacyDepartmentId = formPrivacyDepartmentId;
        return this;
    }

    public Integer getFormAccessibilityDepartmentId() {
        return formAccessibilityDepartmentId;
    }

    public SubmissionWithMembership setFormAccessibilityDepartmentId(Integer formAccessibilityDepartmentId) {
        this.formAccessibilityDepartmentId = formAccessibilityDepartmentId;
        return this;
    }

    public Integer getFormCustomerAccessHours() {
        return formCustomerAccessHours;
    }

    public SubmissionWithMembership setFormCustomerAccessHours(Integer formCustomerAccessHours) {
        this.formCustomerAccessHours = formCustomerAccessHours;
        return this;
    }

    public Integer getFormSubmissionRetentionWeeks() {
        return formSubmissionRetentionWeeks;
    }

    public SubmissionWithMembership setFormSubmissionRetentionWeeks(Integer formSubmissionRetentionWeeks) {
        this.formSubmissionRetentionWeeks = formSubmissionRetentionWeeks;
        return this;
    }

    public Integer getFormThemeId() {
        return formThemeId;
    }

    public SubmissionWithMembership setFormThemeId(Integer formThemeId) {
        this.formThemeId = formThemeId;
        return this;
    }

    public UUID getFormPdfTemplateKey() {
        return formPdfTemplateKey;
    }

    public SubmissionWithMembership setFormPdfTemplateKey(UUID formPdfTemplateKey) {
        this.formPdfTemplateKey = formPdfTemplateKey;
        return this;
    }

    public UUID getFormPaymentProviderKey() {
        return formPaymentProviderKey;
    }

    public SubmissionWithMembership setFormPaymentProviderKey(UUID formPaymentProviderKey) {
        this.formPaymentProviderKey = formPaymentProviderKey;
        return this;
    }

    public String getFormPaymentPurpose() {
        return formPaymentPurpose;
    }

    public SubmissionWithMembership setFormPaymentPurpose(String formPaymentPurpose) {
        this.formPaymentPurpose = formPaymentPurpose;
        return this;
    }

    public String getFormPaymentDescription() {
        return formPaymentDescription;
    }

    public SubmissionWithMembership setFormPaymentDescription(String formPaymentDescription) {
        this.formPaymentDescription = formPaymentDescription;
        return this;
    }

    public List<PaymentProduct> getFormPaymentProducts() {
        return formPaymentProducts;
    }

    public SubmissionWithMembership setFormPaymentProducts(List<PaymentProduct> formPaymentProducts) {
        this.formPaymentProducts = formPaymentProducts;
        return this;
    }

    public List<IdentityProviderLink> getFormIdentityProviders() {
        return formIdentityProviders;
    }

    public SubmissionWithMembership setFormIdentityProviders(List<IdentityProviderLink> formIdentityProviders) {
        this.formIdentityProviders = formIdentityProviders;
        return this;
    }

    public Boolean getFormIdentityVerificationRequired() {
        return formIdentityVerificationRequired;
    }

    public SubmissionWithMembership setFormIdentityVerificationRequired(Boolean formIdentityVerificationRequired) {
        this.formIdentityVerificationRequired = formIdentityVerificationRequired;
        return this;
    }

    public Integer getFormDestinationId() {
        return formDestinationId;
    }

    public SubmissionWithMembership setFormDestinationId(Integer formDestinationId) {
        this.formDestinationId = formDestinationId;
        return this;
    }

    public RootElement getFormRootElement() {
        return formRootElement;
    }

    public SubmissionWithMembership setFormRootElement(RootElement formRootElement) {
        this.formRootElement = formRootElement;
        return this;
    }

    public LocalDateTime getFormVersionPublished() {
        return formVersionPublished;
    }

    public SubmissionWithMembership setFormVersionPublished(LocalDateTime formVersionPublished) {
        this.formVersionPublished = formVersionPublished;
        return this;
    }

    public LocalDateTime getFormVersionRevoked() {
        return formVersionRevoked;
    }

    public SubmissionWithMembership setFormVersionRevoked(LocalDateTime formVersionRevoked) {
        this.formVersionRevoked = formVersionRevoked;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public SubmissionWithMembership setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public SubmissionWithMembership setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public SubmissionWithMembership setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
        return this;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public SubmissionWithMembership setUserLastName(String userLastName) {
        this.userLastName = userLastName;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public SubmissionWithMembership setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public SubmissionWithMembership setUserEnabled(Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    public Boolean getUserVerified() {
        return userVerified;
    }

    public SubmissionWithMembership setUserVerified(Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    public Boolean getUserGlobalAdmin() {
        return userGlobalAdmin;
    }

    public SubmissionWithMembership setUserGlobalAdmin(Boolean userGlobalAdmin) {
        this.userGlobalAdmin = userGlobalAdmin;
        return this;
    }

    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public SubmissionWithMembership setUserDeletedInIdp(Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }

    public Boolean getUserIsDeveloper() {
        return userIsDeveloper;
    }

    public SubmissionWithMembership setUserIsDeveloper(Boolean userIsDeveloper) {
        this.userIsDeveloper = userIsDeveloper;
        return this;
    }

    public Boolean getUserIsManager() {
        return userIsManager;
    }

    public SubmissionWithMembership setUserIsManager(Boolean userIsManager) {
        this.userIsManager = userIsManager;
        return this;
    }

    public Boolean getUserIsResponsible() {
        return userIsResponsible;
    }

    public SubmissionWithMembership setUserIsResponsible(Boolean userIsResponsible) {
        this.userIsResponsible = userIsResponsible;
        return this;
    }
}
