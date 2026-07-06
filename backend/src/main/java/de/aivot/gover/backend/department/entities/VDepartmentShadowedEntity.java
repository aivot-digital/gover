package de.aivot.gover.backend.department.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "v_departments_shadowed")
public class VDepartmentShadowedEntity {
    @Id
    private Integer id;
    private String name;
    private String postalAddress;
    private String imprint;
    private String commonPrivacy;
    private String commonAccessibility;
    private String technicalSupportEmail;
    private String specialSupportEmail;
    private Instant created;
    private Instant updated;
    private Integer themeId;
    private String technicalSupportPhone;
    private String technicalSupportInfo;
    private String specialSupportPhone;
    private String specialSupportInfo;
    private String defaultMailSignature;
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

    public String getPostalAddress() {
        return postalAddress;
    }

    public VDepartmentShadowedEntity setPostalAddress(String postalAddress) {
        this.postalAddress = postalAddress;
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

    public String getTechnicalSupportEmail() {
        return technicalSupportEmail;
    }

    public VDepartmentShadowedEntity setTechnicalSupportEmail(String technicalSupportEmail) {
        this.technicalSupportEmail = technicalSupportEmail;
        return this;
    }

    public String getSpecialSupportEmail() {
        return specialSupportEmail;
    }

    public VDepartmentShadowedEntity setSpecialSupportEmail(String specialSupportEmail) {
        this.specialSupportEmail = specialSupportEmail;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public VDepartmentShadowedEntity setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public Instant getUpdated() {
        return updated;
    }

    public VDepartmentShadowedEntity setUpdated(Instant updated) {
        this.updated = updated;
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

    public String getDefaultMailSignature() {
        return defaultMailSignature;
    }

    public VDepartmentShadowedEntity setDefaultMailSignature(String defaultMailSignature) {
        this.defaultMailSignature = defaultMailSignature;
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
