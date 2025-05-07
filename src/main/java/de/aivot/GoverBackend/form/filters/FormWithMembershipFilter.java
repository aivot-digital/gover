package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.entities.FormWithMembership;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class FormWithMembershipFilter implements Filter<FormWithMembership> {
    private Integer id;
    private String title;
    private String slug;
    private String version;
    private FormStatus status;
    private FormType type;
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
    private String pdfBodyTemplateKey;
    private String paymentProvider;
    private String userId;
    private Boolean isDeveloper;
    private Boolean isManager;
    private Boolean isResponsible;
    private Boolean identityRequired;
    private String identityProviderKey;

    public static FormWithMembershipFilter create() {
        return new FormWithMembershipFilter();
    }

    @Nonnull
    @Override
    public Specification<FormWithMembership> build() {
        return SpecificationBuilder
                .create(FormWithMembership.class)
                .withEquals("id", id)
                .withContains("title", title)
                .withEquals("slug", slug)
                .withEquals("version", version)
                .withEquals("status", status)
                .withEquals("type", type)
                .withEquals("destinationId", destinationId)
                .withEquals("legalSupportDepartmentId", legalSupportDepartmentId)
                .withEquals("technicalSupportDepartmentId", technicalSupportDepartmentId)
                .withEquals("imprintDepartmentId", imprintDepartmentId)
                .withEquals("privacyDepartmentId", privacyDepartmentId)
                .withEquals("accessibilityDepartmentId", accessibilityDepartmentId)
                .withEquals("developingDepartmentId", developingDepartmentId)
                .withEquals("managingDepartmentId", managingDepartmentId)
                .withEquals("responsibleDepartmentId", responsibleDepartmentId)
                .withEquals("themeId", themeId)
                .withEquals("pdfBodyTemplateKey", pdfBodyTemplateKey)
                .withEquals("paymentProvider", paymentProvider)
                .withEquals("userId", userId)
                .withEquals("userIsDeveloper", isDeveloper)
                .withEquals("userIsManager", isManager)
                .withEquals("userIsResponsible", isResponsible)
                .withEquals("identityRequired", identityRequired)
                .withJsonArrayElementFieldEquals("identityProviders", "identityProviderKey", identityProviderKey)
                .build();
    }

    public FormFilter asFormFilter() {
        return FormFilter
                .create()
                .setId(id)
                .setTitle(title)
                .setSlug(slug)
                .setVersion(version)
                .setStatus(status)
                .setType(type)
                .setDestinationId(destinationId)
                .setLegalSupportDepartmentId(legalSupportDepartmentId)
                .setTechnicalSupportDepartmentId(technicalSupportDepartmentId)
                .setImprintDepartmentId(imprintDepartmentId)
                .setPrivacyDepartmentId(privacyDepartmentId)
                .setAccessibilityDepartmentId(accessibilityDepartmentId)
                .setDevelopingDepartmentId(developingDepartmentId)
                .setManagingDepartmentId(managingDepartmentId)
                .setResponsibleDepartmentId(responsibleDepartmentId)
                .setThemeId(themeId)
                .setPdfBodyTemplateKey(pdfBodyTemplateKey)
                .setPaymentProvider(paymentProvider)
                .setIdentityRequired(identityRequired)
                .setIdentityProviderKey(identityProviderKey);
    }

    public Integer getId() {
        return id;
    }

    public FormWithMembershipFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public FormWithMembershipFilter setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public FormWithMembershipFilter setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public FormWithMembershipFilter setVersion(String version) {
        this.version = version;
        return this;
    }

    public FormStatus getStatus() {
        return status;
    }

    public FormWithMembershipFilter setStatus(FormStatus status) {
        this.status = status;
        return this;
    }

    public FormType getType() {
        return type;
    }

    public FormWithMembershipFilter setType(FormType type) {
        this.type = type;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public FormWithMembershipFilter setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public FormWithMembershipFilter setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public FormWithMembershipFilter setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public FormWithMembershipFilter setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public FormWithMembershipFilter setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public FormWithMembershipFilter setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public FormWithMembershipFilter setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public FormWithMembershipFilter setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public FormWithMembershipFilter setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public FormWithMembershipFilter setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public String getPdfBodyTemplateKey() {
        return pdfBodyTemplateKey;
    }

    public FormWithMembershipFilter setPdfBodyTemplateKey(String pdfBodyTemplateKey) {
        this.pdfBodyTemplateKey = pdfBodyTemplateKey;
        return this;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public FormWithMembershipFilter setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public FormWithMembershipFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getDeveloper() {
        return isDeveloper;
    }

    public FormWithMembershipFilter setDeveloper(Boolean developer) {
        isDeveloper = developer;
        return this;
    }

    public Boolean getManager() {
        return isManager;
    }

    public FormWithMembershipFilter setManager(Boolean manager) {
        isManager = manager;
        return this;
    }

    public Boolean getResponsible() {
        return isResponsible;
    }

    public FormWithMembershipFilter setResponsible(Boolean responsible) {
        isResponsible = responsible;
        return this;
    }

    public Boolean getIdentityRequired() {
        return identityRequired;
    }

    public FormWithMembershipFilter setIdentityRequired(Boolean identityRequired) {
        this.identityRequired = identityRequired;
        return this;
    }

    public String getIdentityProviderKey() {
        return identityProviderKey;
    }

    public FormWithMembershipFilter setIdentityProviderKey(String identityProviderKey) {
        this.identityProviderKey = identityProviderKey;
        return this;
    }
}
