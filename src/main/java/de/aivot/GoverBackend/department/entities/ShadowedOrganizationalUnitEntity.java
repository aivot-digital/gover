package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizational_units_shadowed")
public class ShadowedOrganizationalUnitEntity {
    @Id
    private Integer id;
    @Nullable
    private String name;
    @Nullable
    private String address;
    @Nullable
    private String imprint;
    @Nullable
    private String privacy;
    @Nullable
    private String accessibility;
    @Nullable
    private String technicalSupportAddress;
    @Nullable
    private String specialSupportAddress;
    @Nullable
    private String contactLegal;
    @Nullable
    private String contactTechnical;
    @Nullable
    private String additionalInfo;
    @Nullable
    private String departmentMail;
    @Nullable
    private Integer themeId;
    @Nonnull
    private Integer depth;
    @Nullable
    private Integer parentOrgUnitId;
    @Nonnull
    private LocalDateTime created;
    @Nonnull
    private LocalDateTime updated;

    // region Getters & Setters

    public Integer getId() {
        return id;
    }

    public ShadowedOrganizationalUnitEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public ShadowedOrganizationalUnitEntity setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getAddress() {
        return address;
    }

    public ShadowedOrganizationalUnitEntity setAddress(@Nullable String address) {
        this.address = address;
        return this;
    }

    @Nullable
    public String getImprint() {
        return imprint;
    }

    public ShadowedOrganizationalUnitEntity setImprint(@Nullable String imprint) {
        this.imprint = imprint;
        return this;
    }

    @Nullable
    public String getPrivacy() {
        return privacy;
    }

    public ShadowedOrganizationalUnitEntity setPrivacy(@Nullable String privacy) {
        this.privacy = privacy;
        return this;
    }

    @Nullable
    public String getAccessibility() {
        return accessibility;
    }

    public ShadowedOrganizationalUnitEntity setAccessibility(@Nullable String accessibility) {
        this.accessibility = accessibility;
        return this;
    }

    @Nullable
    public String getTechnicalSupportAddress() {
        return technicalSupportAddress;
    }

    public ShadowedOrganizationalUnitEntity setTechnicalSupportAddress(@Nullable String technicalSupportAddress) {
        this.technicalSupportAddress = technicalSupportAddress;
        return this;
    }

    @Nullable
    public String getSpecialSupportAddress() {
        return specialSupportAddress;
    }

    public ShadowedOrganizationalUnitEntity setSpecialSupportAddress(@Nullable String specialSupportAddress) {
        this.specialSupportAddress = specialSupportAddress;
        return this;
    }

    @Nullable
    public String getContactLegal() {
        return contactLegal;
    }

    public ShadowedOrganizationalUnitEntity setContactLegal(@Nullable String contactLegal) {
        this.contactLegal = contactLegal;
        return this;
    }

    @Nullable
    public String getContactTechnical() {
        return contactTechnical;
    }

    public ShadowedOrganizationalUnitEntity setContactTechnical(@Nullable String contactTechnical) {
        this.contactTechnical = contactTechnical;
        return this;
    }

    @Nullable
    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public ShadowedOrganizationalUnitEntity setAdditionalInfo(@Nullable String additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    @Nullable
    public String getDepartmentMail() {
        return departmentMail;
    }

    public ShadowedOrganizationalUnitEntity setDepartmentMail(@Nullable String departmentMail) {
        this.departmentMail = departmentMail;
        return this;
    }

    @Nullable
    public Integer getThemeId() {
        return themeId;
    }

    public ShadowedOrganizationalUnitEntity setThemeId(@Nullable Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    @Nonnull
    public Integer getDepth() {
        return depth;
    }

    public ShadowedOrganizationalUnitEntity setDepth(@Nonnull Integer depth) {
        this.depth = depth;
        return this;
    }

    @Nullable
    public Integer getParentOrgUnitId() {
        return parentOrgUnitId;
    }

    public ShadowedOrganizationalUnitEntity setParentOrgUnitId(@Nullable Integer parentOrgUnitId) {
        this.parentOrgUnitId = parentOrgUnitId;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public ShadowedOrganizationalUnitEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public ShadowedOrganizationalUnitEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}
