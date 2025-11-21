package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "v_organizational_units_shadowed")
public class VOrganizationalUnitShadowedEntity {
    @Id
    private Integer id;
    @Nonnull
    private String name;
    @Nullable
    private String address;
    @Nullable
    private String imprint;
    @Nullable
    private String commonPrivacy;
    @Nullable
    private String commonAccessibility;
    @Nullable
    private String technicalSupportAddress;
    @Nullable
    private String technicalSupportPhone;
    @Nullable
    private String technicalSupportInfo;
    @Nullable
    private String specialSupportAddress;
    @Nullable
    private String specialSupportPhone;
    @Nullable
    private String specialSupportInfo;
    @Nullable
    private String additionalInfo;
    @Nullable
    private String departmentMail;
    @Nullable
    private Integer themeId;
    @Nonnull
    private LocalDateTime created;
    @Nonnull
    private LocalDateTime updated;

    // region Getters & Setters

    public Integer getId() {
        return id;
    }

    public VOrganizationalUnitShadowedEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public VOrganizationalUnitShadowedEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getAddress() {
        return address;
    }

    public VOrganizationalUnitShadowedEntity setAddress(@Nullable String address) {
        this.address = address;
        return this;
    }

    @Nullable
    public String getImprint() {
        return imprint;
    }

    public VOrganizationalUnitShadowedEntity setImprint(@Nullable String imprint) {
        this.imprint = imprint;
        return this;
    }

    @Nullable
    public String getCommonPrivacy() {
        return commonPrivacy;
    }

    public VOrganizationalUnitShadowedEntity setCommonPrivacy(@Nullable String commonPrivacy) {
        this.commonPrivacy = commonPrivacy;
        return this;
    }

    @Nullable
    public String getCommonAccessibility() {
        return commonAccessibility;
    }

    public VOrganizationalUnitShadowedEntity setCommonAccessibility(@Nullable String commonAccessibility) {
        this.commonAccessibility = commonAccessibility;
        return this;
    }

    @Nullable
    public String getTechnicalSupportAddress() {
        return technicalSupportAddress;
    }

    public VOrganizationalUnitShadowedEntity setTechnicalSupportAddress(@Nullable String technicalSupportAddress) {
        this.technicalSupportAddress = technicalSupportAddress;
        return this;
    }

    @Nullable
    public String getTechnicalSupportPhone() {
        return technicalSupportPhone;
    }

    public VOrganizationalUnitShadowedEntity setTechnicalSupportPhone(@Nullable String technicalSupportPhone) {
        this.technicalSupportPhone = technicalSupportPhone;
        return this;
    }

    @Nullable
    public String getTechnicalSupportInfo() {
        return technicalSupportInfo;
    }

    public VOrganizationalUnitShadowedEntity setTechnicalSupportInfo(@Nullable String technicalSupportInfo) {
        this.technicalSupportInfo = technicalSupportInfo;
        return this;
    }

    @Nullable
    public String getSpecialSupportAddress() {
        return specialSupportAddress;
    }

    public VOrganizationalUnitShadowedEntity setSpecialSupportAddress(@Nullable String specialSupportAddress) {
        this.specialSupportAddress = specialSupportAddress;
        return this;
    }

    @Nullable
    public String getSpecialSupportPhone() {
        return specialSupportPhone;
    }

    public VOrganizationalUnitShadowedEntity setSpecialSupportPhone(@Nullable String specialSupportPhone) {
        this.specialSupportPhone = specialSupportPhone;
        return this;
    }

    @Nullable
    public String getSpecialSupportInfo() {
        return specialSupportInfo;
    }

    public VOrganizationalUnitShadowedEntity setSpecialSupportInfo(@Nullable String specialSupportInfo) {
        this.specialSupportInfo = specialSupportInfo;
        return this;
    }

    @Nullable
    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public VOrganizationalUnitShadowedEntity setAdditionalInfo(@Nullable String additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    @Nullable
    public String getDepartmentMail() {
        return departmentMail;
    }

    public VOrganizationalUnitShadowedEntity setDepartmentMail(@Nullable String departmentMail) {
        this.departmentMail = departmentMail;
        return this;
    }

    @Nullable
    public Integer getThemeId() {
        return themeId;
    }

    public VOrganizationalUnitShadowedEntity setThemeId(@Nullable Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public VOrganizationalUnitShadowedEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public VOrganizationalUnitShadowedEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }


    // endregion
}
