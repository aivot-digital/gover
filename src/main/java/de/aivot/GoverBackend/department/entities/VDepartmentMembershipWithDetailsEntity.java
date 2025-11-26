package de.aivot.GoverBackend.department.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "v_department_memberships_with_details")
public class VDepartmentMembershipWithDetailsEntity {
    @Id
    private Integer id;
    private Integer departmentId;
    private String userId;
    private LocalDateTime created;
    private LocalDateTime updated;
    private String name;
    private String address;
    private String imprint;
    private String commonPrivacy;
    private String commonAccessibility;
    private String technicalSupportAddress;
    private String specialSupportAddress;
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
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Boolean enabled;
    private Boolean verified;
    private Boolean globalAdmin;
    private Boolean deletedInIdp;

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public VDepartmentMembershipWithDetailsEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VDepartmentMembershipWithDetailsEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public VDepartmentMembershipWithDetailsEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public VDepartmentMembershipWithDetailsEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public String getName() {
        return name;
    }

    public VDepartmentMembershipWithDetailsEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public VDepartmentMembershipWithDetailsEntity setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getImprint() {
        return imprint;
    }

    public VDepartmentMembershipWithDetailsEntity setImprint(String imprint) {
        this.imprint = imprint;
        return this;
    }

    public String getCommonPrivacy() {
        return commonPrivacy;
    }

    public VDepartmentMembershipWithDetailsEntity setCommonPrivacy(String commonPrivacy) {
        this.commonPrivacy = commonPrivacy;
        return this;
    }

    public String getCommonAccessibility() {
        return commonAccessibility;
    }

    public VDepartmentMembershipWithDetailsEntity setCommonAccessibility(String commonAccessibility) {
        this.commonAccessibility = commonAccessibility;
        return this;
    }

    public String getTechnicalSupportAddress() {
        return technicalSupportAddress;
    }

    public VDepartmentMembershipWithDetailsEntity setTechnicalSupportAddress(String technicalSupportAddress) {
        this.technicalSupportAddress = technicalSupportAddress;
        return this;
    }

    public String getSpecialSupportAddress() {
        return specialSupportAddress;
    }

    public VDepartmentMembershipWithDetailsEntity setSpecialSupportAddress(String specialSupportAddress) {
        this.specialSupportAddress = specialSupportAddress;
        return this;
    }

    public String getDepartmentMail() {
        return departmentMail;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentMail(String departmentMail) {
        this.departmentMail = departmentMail;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public VDepartmentMembershipWithDetailsEntity setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public String getTechnicalSupportPhone() {
        return technicalSupportPhone;
    }

    public VDepartmentMembershipWithDetailsEntity setTechnicalSupportPhone(String technicalSupportPhone) {
        this.technicalSupportPhone = technicalSupportPhone;
        return this;
    }

    public String getTechnicalSupportInfo() {
        return technicalSupportInfo;
    }

    public VDepartmentMembershipWithDetailsEntity setTechnicalSupportInfo(String technicalSupportInfo) {
        this.technicalSupportInfo = technicalSupportInfo;
        return this;
    }

    public String getSpecialSupportPhone() {
        return specialSupportPhone;
    }

    public VDepartmentMembershipWithDetailsEntity setSpecialSupportPhone(String specialSupportPhone) {
        this.specialSupportPhone = specialSupportPhone;
        return this;
    }

    public String getSpecialSupportInfo() {
        return specialSupportInfo;
    }

    public VDepartmentMembershipWithDetailsEntity setSpecialSupportInfo(String specialSupportInfo) {
        this.specialSupportInfo = specialSupportInfo;
        return this;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public VDepartmentMembershipWithDetailsEntity setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    public Integer getDepth() {
        return depth;
    }

    public VDepartmentMembershipWithDetailsEntity setDepth(Integer depth) {
        this.depth = depth;
        return this;
    }

    public Integer getParentDepartmentId() {
        return parentDepartmentId;
    }

    public VDepartmentMembershipWithDetailsEntity setParentDepartmentId(Integer parentDepartmentId) {
        this.parentDepartmentId = parentDepartmentId;
        return this;
    }

    public List<String> getParentNames() {
        return parentNames;
    }

    public VDepartmentMembershipWithDetailsEntity setParentNames(List<String> parentNames) {
        this.parentNames = parentNames;
        return this;
    }

    public List<Integer> getParentIds() {
        return parentIds;
    }

    public VDepartmentMembershipWithDetailsEntity setParentIds(List<Integer> parentIds) {
        this.parentIds = parentIds;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public VDepartmentMembershipWithDetailsEntity setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public VDepartmentMembershipWithDetailsEntity setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public VDepartmentMembershipWithDetailsEntity setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public VDepartmentMembershipWithDetailsEntity setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public VDepartmentMembershipWithDetailsEntity setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getVerified() {
        return verified;
    }

    public VDepartmentMembershipWithDetailsEntity setVerified(Boolean verified) {
        this.verified = verified;
        return this;
    }

    public Boolean getGlobalAdmin() {
        return globalAdmin;
    }

    public VDepartmentMembershipWithDetailsEntity setGlobalAdmin(Boolean globalAdmin) {
        this.globalAdmin = globalAdmin;
        return this;
    }

    public Boolean getDeletedInIdp() {
        return deletedInIdp;
    }

    public VDepartmentMembershipWithDetailsEntity setDeletedInIdp(Boolean deletedInIdp) {
        this.deletedInIdp = deletedInIdp;
        return this;
    }

    // endregion
}
