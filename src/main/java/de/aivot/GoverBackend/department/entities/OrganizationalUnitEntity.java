package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizational_units")
public class OrganizationalUnitEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nonnull
    @Column(length = 96)
    private String name;

    @Nullable
    @Column(length = 255)
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
    private String technicalSupportAddress;

    @Nullable
    @Column(length = 96)
    private String technicalSupportPhone;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String technicalSupportInfo;

    @Nullable
    @Column(length = 255)
    private String specialSupportAddress;

    @Nullable
    @Column(length = 96)
    private String specialSupportPhone;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String specialSupportInfo;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String additionalInfo;

    @Nullable
    @Column(length = 255)
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

    public OrganizationalUnitEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public OrganizationalUnitEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getAddress() {
        return address;
    }

    public OrganizationalUnitEntity setAddress(@Nullable String address) {
        this.address = address;
        return this;
    }

    @Nullable
    public String getImprint() {
        return imprint;
    }

    public OrganizationalUnitEntity setImprint(@Nullable String imprint) {
        this.imprint = imprint;
        return this;
    }

    @Nullable
    public String getCommonPrivacy() {
        return commonPrivacy;
    }

    public OrganizationalUnitEntity setCommonPrivacy(@Nullable String commonPrivacy) {
        this.commonPrivacy = commonPrivacy;
        return this;
    }

    @Nullable
    public String getCommonAccessibility() {
        return commonAccessibility;
    }

    public OrganizationalUnitEntity setCommonAccessibility(@Nullable String commonAccessibility) {
        this.commonAccessibility = commonAccessibility;
        return this;
    }

    @Nullable
    public String getTechnicalSupportAddress() {
        return technicalSupportAddress;
    }

    public OrganizationalUnitEntity setTechnicalSupportAddress(@Nullable String technicalSupportAddress) {
        this.technicalSupportAddress = technicalSupportAddress;
        return this;
    }

    @Nullable
    public String getTechnicalSupportPhone() {
        return technicalSupportPhone;
    }

    public OrganizationalUnitEntity setTechnicalSupportPhone(@Nullable String technicalSupportPhone) {
        this.technicalSupportPhone = technicalSupportPhone;
        return this;
    }

    @Nullable
    public String getTechnicalSupportInfo() {
        return technicalSupportInfo;
    }

    public OrganizationalUnitEntity setTechnicalSupportInfo(@Nullable String technicalSupportInfo) {
        this.technicalSupportInfo = technicalSupportInfo;
        return this;
    }

    @Nullable
    public String getSpecialSupportAddress() {
        return specialSupportAddress;
    }

    public OrganizationalUnitEntity setSpecialSupportAddress(@Nullable String specialSupportAddress) {
        this.specialSupportAddress = specialSupportAddress;
        return this;
    }

    @Nullable
    public String getSpecialSupportPhone() {
        return specialSupportPhone;
    }

    public OrganizationalUnitEntity setSpecialSupportPhone(@Nullable String specialSupportPhone) {
        this.specialSupportPhone = specialSupportPhone;
        return this;
    }

    @Nullable
    public String getSpecialSupportInfo() {
        return specialSupportInfo;
    }

    public OrganizationalUnitEntity setSpecialSupportInfo(@Nullable String specialSupportInfo) {
        this.specialSupportInfo = specialSupportInfo;
        return this;
    }

    @Nullable
    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public OrganizationalUnitEntity setAdditionalInfo(@Nullable String additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    @Nullable
    public String getDepartmentMail() {
        return departmentMail;
    }

    public OrganizationalUnitEntity setDepartmentMail(@Nullable String departmentMail) {
        this.departmentMail = departmentMail;
        return this;
    }

    @Nullable
    public Integer getThemeId() {
        return themeId;
    }

    public OrganizationalUnitEntity setThemeId(@Nullable Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public OrganizationalUnitEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public OrganizationalUnitEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}
