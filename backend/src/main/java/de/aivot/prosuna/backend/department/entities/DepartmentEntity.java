package de.aivot.prosuna.backend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;

@Entity
@Table(name = "departments")
public class DepartmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "departments_id_seq")
    @SequenceGenerator(name = "departments_id_seq", allocationSize = 1)
    private Integer id;

    @Nullable
    private Integer parentDepartmentId;

    @Nonnull
    private Integer depth;

    @Nonnull
    @Column(length = 96)
    @NotNull(message = "Der Name darf nicht null sein")
    @NotBlank(message = "Der Name darf nicht leer sein")
    @Length(min = 3, max = 96, message = "Der Name muss zwischen 3 und 96 Zeichen lang sein")
    private String name;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String postalAddress;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String imprint;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String commonPrivacy;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String commonAccessibility;

    @Nullable
    @Column(length = 255)
    @Length(max = 255, message = "Die technische Support E-Mail-Adresse darf maximal 255 Zeichen lang sein")
    private String technicalSupportEmail;

    @Nullable
    @Column(length = 96)
    @Length(max = 96, message = "Die technische Support Telefonnummer darf maximal 96 Zeichen lang sein")
    private String technicalSupportPhone;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String technicalSupportInfo;

    @Nullable
    @Column(length = 255)
    @Length(max = 255, message = "Die fachliche Support E-Mail-Adresse darf maximal 255 Zeichen lang sein")
    private String specialSupportEmail;

    @Nullable
    @Column(length = 96)
    @Length(max = 96, message = "Die fachliche Support Telefonnummer darf maximal 96 Zeichen lang sein")
    private String specialSupportPhone;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String specialSupportInfo;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String defaultMailSignature;

    @Nullable
    private Integer themeId;

    @Nonnull
    private Instant created;

    @Nonnull
    private Instant updated;

    // region Signals

    @PrePersist
    public void prePersist() {
        created = Instant.now();
        updated = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = Instant.now();
    }

    // endregion

    // region Getters & Setters

    public Integer getId() {
        return id;
    }

    public DepartmentEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public DepartmentEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getPostalAddress() {
        return postalAddress;
    }

    public DepartmentEntity setPostalAddress(@Nullable String postalAddress) {
        this.postalAddress = postalAddress;
        return this;
    }

    @Nullable
    public String getImprint() {
        return imprint;
    }

    public DepartmentEntity setImprint(@Nullable String imprint) {
        this.imprint = imprint;
        return this;
    }

    @Nullable
    public String getCommonPrivacy() {
        return commonPrivacy;
    }

    public DepartmentEntity setCommonPrivacy(@Nullable String commonPrivacy) {
        this.commonPrivacy = commonPrivacy;
        return this;
    }

    @Nullable
    public String getCommonAccessibility() {
        return commonAccessibility;
    }

    public DepartmentEntity setCommonAccessibility(@Nullable String commonAccessibility) {
        this.commonAccessibility = commonAccessibility;
        return this;
    }

    @Nullable
    public String getTechnicalSupportEmail() {
        return technicalSupportEmail;
    }

    public DepartmentEntity setTechnicalSupportEmail(@Nullable String technicalSupportEmail) {
        this.technicalSupportEmail = technicalSupportEmail;
        return this;
    }

    @Nullable
    public String getTechnicalSupportPhone() {
        return technicalSupportPhone;
    }

    public DepartmentEntity setTechnicalSupportPhone(@Nullable String technicalSupportPhone) {
        this.technicalSupportPhone = technicalSupportPhone;
        return this;
    }

    @Nullable
    public String getTechnicalSupportInfo() {
        return technicalSupportInfo;
    }

    public DepartmentEntity setTechnicalSupportInfo(@Nullable String technicalSupportInfo) {
        this.technicalSupportInfo = technicalSupportInfo;
        return this;
    }

    @Nullable
    public String getSpecialSupportEmail() {
        return specialSupportEmail;
    }

    public DepartmentEntity setSpecialSupportEmail(@Nullable String specialSupportEmail) {
        this.specialSupportEmail = specialSupportEmail;
        return this;
    }

    @Nullable
    public String getSpecialSupportPhone() {
        return specialSupportPhone;
    }

    public DepartmentEntity setSpecialSupportPhone(@Nullable String specialSupportPhone) {
        this.specialSupportPhone = specialSupportPhone;
        return this;
    }

    @Nullable
    public String getSpecialSupportInfo() {
        return specialSupportInfo;
    }

    public DepartmentEntity setSpecialSupportInfo(@Nullable String specialSupportInfo) {
        this.specialSupportInfo = specialSupportInfo;
        return this;
    }

    @Nullable
    public String getDefaultMailSignature() {
        return defaultMailSignature;
    }

    public DepartmentEntity setDefaultMailSignature(@Nullable String defaultMailSignature) {
        this.defaultMailSignature = defaultMailSignature;
        return this;
    }

    @Nullable
    public Integer getThemeId() {
        return themeId;
    }

    public DepartmentEntity setThemeId(@Nullable Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public DepartmentEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public DepartmentEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public Integer getParentDepartmentId() {
        return parentDepartmentId;
    }

    public DepartmentEntity setParentDepartmentId(@Nullable Integer parentOrgUnitId) {
        this.parentDepartmentId = parentOrgUnitId;
        return this;
    }

    @Nonnull
    public Integer getDepth() {
        return depth;
    }

    public DepartmentEntity setDepth(@Nonnull Integer depth) {
        this.depth = depth;
        return this;
    }

    // endregion
}
