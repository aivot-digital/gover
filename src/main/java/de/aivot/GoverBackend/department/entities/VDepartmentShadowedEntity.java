package de.aivot.GoverBackend.department.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "v_departments_shadowed")
public class VDepartmentShadowedEntity {
    @Id
    private Integer id;
    private String name;
    private String address;
    private String imprint;
    private String commonPrivacy;
    private String commonAccessibility;
    private String technicalSupportAddress;
    private String specialSupportAddress;
    private LocalDateTime created;
    private LocalDateTime updated;
    private String departmentMail;
    private Integer themeId;
    private String technicalSupportPhone;
    private String technicalSupportInfo;
    private String specialSupportPhone;
    private String specialSupportInfo;
    private String additionalInfo;
    private Integer depth;
    private Integer parentDepartmentId;
    private List<String> parentNames;
    private List<Integer> parentIds;

    public Integer getId() {
        return id;
    }

    public VDepartmentShadowedEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public VDepartmentShadowedEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public VDepartmentShadowedEntity setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getImprint() {
        return imprint;
    }

    public VDepartmentShadowedEntity setImprint(String imprint) {
        this.imprint = imprint;
        return this;
    }

    public String getCommonPrivacy() {
        return commonPrivacy;
    }

    public VDepartmentShadowedEntity setCommonPrivacy(String commonPrivacy) {
        this.commonPrivacy = commonPrivacy;
        return this;
    }

    public String getCommonAccessibility() {
        return commonAccessibility;
    }

    public VDepartmentShadowedEntity setCommonAccessibility(String commonAccessibility) {
        this.commonAccessibility = commonAccessibility;
        return this;
    }

    public String getTechnicalSupportAddress() {
        return technicalSupportAddress;
    }

    public VDepartmentShadowedEntity setTechnicalSupportAddress(String technicalSupportAddress) {
        this.technicalSupportAddress = technicalSupportAddress;
        return this;
    }

    public String getSpecialSupportAddress() {
        return specialSupportAddress;
    }

    public VDepartmentShadowedEntity setSpecialSupportAddress(String specialSupportAddress) {
        this.specialSupportAddress = specialSupportAddress;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public VDepartmentShadowedEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public VDepartmentShadowedEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public String getDepartmentMail() {
        return departmentMail;
    }

    public VDepartmentShadowedEntity setDepartmentMail(String departmentMail) {
        this.departmentMail = departmentMail;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public VDepartmentShadowedEntity setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public String getTechnicalSupportPhone() {
        return technicalSupportPhone;
    }

    public VDepartmentShadowedEntity setTechnicalSupportPhone(String technicalSupportPhone) {
        this.technicalSupportPhone = technicalSupportPhone;
        return this;
    }

    public String getTechnicalSupportInfo() {
        return technicalSupportInfo;
    }

    public VDepartmentShadowedEntity setTechnicalSupportInfo(String technicalSupportInfo) {
        this.technicalSupportInfo = technicalSupportInfo;
        return this;
    }

    public String getSpecialSupportPhone() {
        return specialSupportPhone;
    }

    public VDepartmentShadowedEntity setSpecialSupportPhone(String specialSupportPhone) {
        this.specialSupportPhone = specialSupportPhone;
        return this;
    }

    public String getSpecialSupportInfo() {
        return specialSupportInfo;
    }

    public VDepartmentShadowedEntity setSpecialSupportInfo(String specialSupportInfo) {
        this.specialSupportInfo = specialSupportInfo;
        return this;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public VDepartmentShadowedEntity setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    public Integer getDepth() {
        return depth;
    }

    public VDepartmentShadowedEntity setDepth(Integer depth) {
        this.depth = depth;
        return this;
    }

    public Integer getParentDepartmentId() {
        return parentDepartmentId;
    }

    public VDepartmentShadowedEntity setParentDepartmentId(Integer parentDepartmentId) {
        this.parentDepartmentId = parentDepartmentId;
        return this;
    }

    public List<String> getParentNames() {
        return parentNames;
    }

    public VDepartmentShadowedEntity setParentNames(List<String> parentNames) {
        this.parentNames = parentNames;
        return this;
    }

    public List<Integer> getParentIds() {
        return parentIds;
    }

    public VDepartmentShadowedEntity setParentIds(List<Integer> parentIds) {
        this.parentIds = parentIds;
        return this;
    }
}
