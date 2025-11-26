package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
public class DepartmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(length = 255)
    @Length(max = 255, message = "Die Adresse darf maximal 255 Zeichen lang sein")
    private String address;

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
    @Length(max = 255, message = "Die technische Support Adresse darf maximal 255 Zeichen lang sein")
    private String technicalSupportAddress;

    @Nullable
    @Column(length = 96)
    @Length(max = 96, message = "Die technische Support Telefonnummer darf maximal 96 Zeichen lang sein")
    private String technicalSupportPhone;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String technicalSupportInfo;

    @Nullable
    @Column(length = 255)
    @Length(max = 255, message = "Die fachliche Support Adresse darf maximal 255 Zeichen lang sein")
    private String specialSupportAddress;

    @Nullable
    @Column(length = 96)
    @Length(max = 96, message = "Die fachliche Support Telefonnummer darf maximal 96 Zeichen lang sein")
    private String specialSupportPhone;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String specialSupportInfo;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String additionalInfo;

    @Nullable
    @Column(length = 255)
    @Length(max = 255, message = "Die Abteilungs-E-Mail darf maximal 255 Zeichen lang sein")
    private String departmentMail;

    @Nullable
    private Integer themeId;

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private LocalDateTime updated;

    // region Signals

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
    public String getAddress() {
        return address;
    }

    public DepartmentEntity setAddress(@Nullable String address) {
        this.address = address;
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
    public String getTechnicalSupportAddress() {
        return technicalSupportAddress;
    }

    public DepartmentEntity setTechnicalSupportAddress(@Nullable String technicalSupportAddress) {
        this.technicalSupportAddress = technicalSupportAddress;
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
    public String getSpecialSupportAddress() {
        return specialSupportAddress;
    }

    public DepartmentEntity setSpecialSupportAddress(@Nullable String specialSupportAddress) {
        this.specialSupportAddress = specialSupportAddress;
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
    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public DepartmentEntity setAdditionalInfo(@Nullable String additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    @Nullable
    public String getDepartmentMail() {
        return departmentMail;
    }

    public DepartmentEntity setDepartmentMail(@Nullable String departmentMail) {
        this.departmentMail = departmentMail;
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
    public LocalDateTime getCreated() {
        return created;
    }

    public DepartmentEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public DepartmentEntity setUpdated(@Nonnull LocalDateTime updated) {
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
