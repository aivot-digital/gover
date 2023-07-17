package de.aivot.GoverBackend.models.entities;

import de.aivot.GoverBackend.converters.RootElementConverter;
import de.aivot.GoverBackend.enums.ApplicationStatus;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.pdf.ApplicationPdfDto;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.script.ScriptEngine;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "applications", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"slug", "version"})
})
public class Application {
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
    @Column(columnDefinition = "jsonb")
    private RootElement root;

    @ManyToOne(fetch = FetchType.EAGER)
    private Destination destination;

    @ManyToOne(fetch = FetchType.LAZY)
    private Department legalSupportDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    private Department technicalSupportDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    private Department imprintDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    private Department privacyDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    private Department accessibilityDepartment;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private Department developingDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    private Department managingDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    private Department responsibleDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    private Theme theme;

    @NotNull
    private LocalDateTime created;

    @NotNull
    private LocalDateTime updated;

    private Integer customerAccessHours;

    private Integer submissionDeletionWeeks;

    @Column(updatable = false)
    private Integer openSubmissions;

    @Column(updatable = false)
    private Integer inProgressSubmissions;

    @Column(updatable = false)
    private Integer totalSubmissions;


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

    public String getApplicationTitle() {
        if (root.getHeadline() != null) {
            return root.getHeadline();
        }

        if (title != null) {
            return title;
        }

        return slug;
    }

    public ApplicationPdfDto toPdfDto(Map<String, Object> customerData, ScriptEngine scriptEngine) {
        return new ApplicationPdfDto(this, customerData, scriptEngine);
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

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public Department getLegalSupportDepartment() {
        return legalSupportDepartment;
    }

    public void setLegalSupportDepartment(Department legalSupportDepartment) {
        this.legalSupportDepartment = legalSupportDepartment;
    }

    public Department getTechnicalSupportDepartment() {
        return technicalSupportDepartment;
    }

    public void setTechnicalSupportDepartment(Department technicalSupportDepartment) {
        this.technicalSupportDepartment = technicalSupportDepartment;
    }

    public Department getImprintDepartment() {
        return imprintDepartment;
    }

    public void setImprintDepartment(Department imprintDepartment) {
        this.imprintDepartment = imprintDepartment;
    }

    public Department getPrivacyDepartment() {
        return privacyDepartment;
    }

    public void setPrivacyDepartment(Department privacyDepartment) {
        this.privacyDepartment = privacyDepartment;
    }

    public Department getAccessibilityDepartment() {
        return accessibilityDepartment;
    }

    public void setAccessibilityDepartment(Department accessibilityDepartment) {
        this.accessibilityDepartment = accessibilityDepartment;
    }

    public Department getDevelopingDepartment() {
        return developingDepartment;
    }

    public void setDevelopingDepartment(Department developingDepartment) {
        this.developingDepartment = developingDepartment;
    }

    public Department getManagingDepartment() {
        return managingDepartment;
    }

    public void setManagingDepartment(Department managingDepartment) {
        this.managingDepartment = managingDepartment;
    }

    public Department getResponsibleDepartment() {
        return responsibleDepartment;
    }

    public void setResponsibleDepartment(Department responsibleDepartment) {
        this.responsibleDepartment = responsibleDepartment;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
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

    public Integer getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(Integer totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public Integer getOpenSubmissions() {
        return openSubmissions;
    }

    public void setOpenSubmissions(Integer openSubmissions) {
        this.openSubmissions = openSubmissions;
    }

    public Integer getInProgressSubmissions() {
        return inProgressSubmissions;
    }

    public void setInProgressSubmissions(Integer inProgressSubmissions) {
        this.inProgressSubmissions = inProgressSubmissions;
    }


    // endregion
}
