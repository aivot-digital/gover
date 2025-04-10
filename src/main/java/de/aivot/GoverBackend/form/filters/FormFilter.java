package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class FormFilter implements Filter<Form> {
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
    private Integer bundIdEnabled;
    private Integer bayernIdEnabled;
    private Integer mukEnabled;
    private Integer shIdEnabled;
    private String pdfBodyTemplateKey;
    private String paymentProvider;

    public static FormFilter create() {
        return new FormFilter();
    }

    @Nonnull
    @Override
    public Specification<Form> build() {
        return SpecificationBuilder
                .create(Form.class)
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
                .withEquals("bundIdEnabled", bundIdEnabled)
                .withEquals("bayernIdEnabled", bayernIdEnabled)
                .withEquals("mukEnabled", mukEnabled)
                .withEquals("shIdEnabled", shIdEnabled)
                .withEquals("pdfBodyTemplateKey", pdfBodyTemplateKey)
                .withEquals("paymentProvider", paymentProvider)
                .build();
    }

    public Integer getId() {
        return id;
    }

    public FormFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public FormFilter setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getSlug() {
        return slug;
    }

    public FormFilter setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public FormFilter setVersion(String version) {
        this.version = version;
        return this;
    }

    public FormStatus getStatus() {
        return status;
    }

    public FormFilter setStatus(FormStatus status) {
        this.status = status;
        return this;
    }

    public FormType getType() {
        return type;
    }

    public FormFilter setType(FormType type) {
        this.type = type;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public FormFilter setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Integer getLegalSupportDepartmentId() {
        return legalSupportDepartmentId;
    }

    public FormFilter setLegalSupportDepartmentId(Integer legalSupportDepartmentId) {
        this.legalSupportDepartmentId = legalSupportDepartmentId;
        return this;
    }

    public Integer getTechnicalSupportDepartmentId() {
        return technicalSupportDepartmentId;
    }

    public FormFilter setTechnicalSupportDepartmentId(Integer technicalSupportDepartmentId) {
        this.technicalSupportDepartmentId = technicalSupportDepartmentId;
        return this;
    }

    public Integer getImprintDepartmentId() {
        return imprintDepartmentId;
    }

    public FormFilter setImprintDepartmentId(Integer imprintDepartmentId) {
        this.imprintDepartmentId = imprintDepartmentId;
        return this;
    }

    public Integer getPrivacyDepartmentId() {
        return privacyDepartmentId;
    }

    public FormFilter setPrivacyDepartmentId(Integer privacyDepartmentId) {
        this.privacyDepartmentId = privacyDepartmentId;
        return this;
    }

    public Integer getAccessibilityDepartmentId() {
        return accessibilityDepartmentId;
    }

    public FormFilter setAccessibilityDepartmentId(Integer accessibilityDepartmentId) {
        this.accessibilityDepartmentId = accessibilityDepartmentId;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public FormFilter setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public FormFilter setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public FormFilter setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public FormFilter setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public Integer getBundIdEnabled() {
        return bundIdEnabled;
    }

    public FormFilter setBundIdEnabled(Integer bundIdEnabled) {
        this.bundIdEnabled = bundIdEnabled;
        return this;
    }

    public Integer getBayernIdEnabled() {
        return bayernIdEnabled;
    }

    public FormFilter setBayernIdEnabled(Integer bayernIdEnabled) {
        this.bayernIdEnabled = bayernIdEnabled;
        return this;
    }

    public Integer getMukEnabled() {
        return mukEnabled;
    }

    public FormFilter setMukEnabled(Integer mukEnabled) {
        this.mukEnabled = mukEnabled;
        return this;
    }

    public Integer getShIdEnabled() {
        return shIdEnabled;
    }

    public FormFilter setShIdEnabled(Integer shIdEnabled) {
        this.shIdEnabled = shIdEnabled;
        return this;
    }

    public String getPdfBodyTemplateKey() {
        return pdfBodyTemplateKey;
    }

    public FormFilter setPdfBodyTemplateKey(String pdfBodyTemplateKey) {
        this.pdfBodyTemplateKey = pdfBodyTemplateKey;
        return this;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public FormFilter setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
        return this;
    }
}
