package de.aivot.GoverBackend.department.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "v_department_user_role_assignments_with_details")
public class VDepartmentUserRoleAssignmentWithDetailsEntity {
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
    private Integer userRoleAssignmentId;
    private String userRoleName;
    private String description;
    private Boolean departmentPermissionEdit;
    private Boolean teamPermissionEdit;
    private Boolean formPermissionCreate;
    private Boolean formPermissionRead;
    private Boolean formPermissionEdit;
    private Boolean formPermissionDelete;
    private Boolean formPermissionAnnotate;
    private Boolean formPermissionPublish;
    private Boolean processPermissionCreate;
    private Boolean processPermissionRead;
    private Boolean processPermissionEdit;
    private Boolean processPermissionDelete;
    private Boolean processPermissionAnnotate;
    private Boolean processPermissionPublish;
    private Boolean processInstancePermissionCreate;
    private Boolean processInstancePermissionRead;
    private Boolean processInstancePermissionEdit;
    private Boolean processInstancePermissionDelete;
    private Boolean processInstancePermissionAnnotate;

    public Integer getId() {
        return id;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public String getName() {
        return name;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getImprint() {
        return imprint;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setImprint(String imprint) {
        this.imprint = imprint;
        return this;
    }

    public String getCommonPrivacy() {
        return commonPrivacy;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setCommonPrivacy(String commonPrivacy) {
        this.commonPrivacy = commonPrivacy;
        return this;
    }

    public String getCommonAccessibility() {
        return commonAccessibility;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setCommonAccessibility(String commonAccessibility) {
        this.commonAccessibility = commonAccessibility;
        return this;
    }

    public String getTechnicalSupportAddress() {
        return technicalSupportAddress;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setTechnicalSupportAddress(String technicalSupportAddress) {
        this.technicalSupportAddress = technicalSupportAddress;
        return this;
    }

    public String getSpecialSupportAddress() {
        return specialSupportAddress;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setSpecialSupportAddress(String specialSupportAddress) {
        this.specialSupportAddress = specialSupportAddress;
        return this;
    }

    public String getDepartmentMail() {
        return departmentMail;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setDepartmentMail(String departmentMail) {
        this.departmentMail = departmentMail;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public String getTechnicalSupportPhone() {
        return technicalSupportPhone;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setTechnicalSupportPhone(String technicalSupportPhone) {
        this.technicalSupportPhone = technicalSupportPhone;
        return this;
    }

    public String getTechnicalSupportInfo() {
        return technicalSupportInfo;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setTechnicalSupportInfo(String technicalSupportInfo) {
        this.technicalSupportInfo = technicalSupportInfo;
        return this;
    }

    public String getSpecialSupportPhone() {
        return specialSupportPhone;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setSpecialSupportPhone(String specialSupportPhone) {
        this.specialSupportPhone = specialSupportPhone;
        return this;
    }

    public String getSpecialSupportInfo() {
        return specialSupportInfo;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setSpecialSupportInfo(String specialSupportInfo) {
        this.specialSupportInfo = specialSupportInfo;
        return this;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    public Integer getDepth() {
        return depth;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setDepth(Integer depth) {
        this.depth = depth;
        return this;
    }

    public Integer getParentDepartmentId() {
        return parentDepartmentId;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setParentDepartmentId(Integer parentDepartmentId) {
        this.parentDepartmentId = parentDepartmentId;
        return this;
    }

    public List<String> getParentNames() {
        return parentNames;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setParentNames(List<String> parentNames) {
        this.parentNames = parentNames;
        return this;
    }

    public List<Integer> getParentIds() {
        return parentIds;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setParentIds(List<Integer> parentIds) {
        this.parentIds = parentIds;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getVerified() {
        return verified;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setVerified(Boolean verified) {
        this.verified = verified;
        return this;
    }

    public Boolean getGlobalAdmin() {
        return globalAdmin;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setGlobalAdmin(Boolean globalAdmin) {
        this.globalAdmin = globalAdmin;
        return this;
    }

    public Boolean getDeletedInIdp() {
        return deletedInIdp;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setDeletedInIdp(Boolean deletedInIdp) {
        this.deletedInIdp = deletedInIdp;
        return this;
    }

    public Integer getUserRoleAssignmentId() {
        return userRoleAssignmentId;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setUserRoleAssignmentId(Integer userRoleAssignmentId) {
        this.userRoleAssignmentId = userRoleAssignmentId;
        return this;
    }

    public String getUserRoleName() {
        return userRoleName;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setUserRoleName(String userRoleName) {
        this.userRoleName = userRoleName;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setDescription(String description) {
        this.description = description;
        return this;
    }

    public Boolean getDepartmentPermissionEdit() {
        return departmentPermissionEdit;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setDepartmentPermissionEdit(Boolean departmentPermissionEdit) {
        this.departmentPermissionEdit = departmentPermissionEdit;
        return this;
    }

    public Boolean getTeamPermissionEdit() {
        return teamPermissionEdit;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setTeamPermissionEdit(Boolean teamPermissionEdit) {
        this.teamPermissionEdit = teamPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setFormPermissionCreate(Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setFormPermissionRead(Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setFormPermissionEdit(Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setFormPermissionDelete(Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setFormPermissionAnnotate(Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setFormPermissionPublish(Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }

    public Boolean getProcessPermissionCreate() {
        return processPermissionCreate;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setProcessPermissionCreate(Boolean processPermissionCreate) {
        this.processPermissionCreate = processPermissionCreate;
        return this;
    }

    public Boolean getProcessPermissionRead() {
        return processPermissionRead;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setProcessPermissionRead(Boolean processPermissionRead) {
        this.processPermissionRead = processPermissionRead;
        return this;
    }

    public Boolean getProcessPermissionEdit() {
        return processPermissionEdit;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setProcessPermissionEdit(Boolean processPermissionEdit) {
        this.processPermissionEdit = processPermissionEdit;
        return this;
    }

    public Boolean getProcessPermissionDelete() {
        return processPermissionDelete;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setProcessPermissionDelete(Boolean processPermissionDelete) {
        this.processPermissionDelete = processPermissionDelete;
        return this;
    }

    public Boolean getProcessPermissionAnnotate() {
        return processPermissionAnnotate;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setProcessPermissionAnnotate(Boolean processPermissionAnnotate) {
        this.processPermissionAnnotate = processPermissionAnnotate;
        return this;
    }

    public Boolean getProcessPermissionPublish() {
        return processPermissionPublish;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setProcessPermissionPublish(Boolean processPermissionPublish) {
        this.processPermissionPublish = processPermissionPublish;
        return this;
    }

    public Boolean getProcessInstancePermissionCreate() {
        return processInstancePermissionCreate;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setProcessInstancePermissionCreate(Boolean processInstancePermissionCreate) {
        this.processInstancePermissionCreate = processInstancePermissionCreate;
        return this;
    }

    public Boolean getProcessInstancePermissionRead() {
        return processInstancePermissionRead;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setProcessInstancePermissionRead(Boolean processInstancePermissionRead) {
        this.processInstancePermissionRead = processInstancePermissionRead;
        return this;
    }

    public Boolean getProcessInstancePermissionEdit() {
        return processInstancePermissionEdit;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setProcessInstancePermissionEdit(Boolean processInstancePermissionEdit) {
        this.processInstancePermissionEdit = processInstancePermissionEdit;
        return this;
    }

    public Boolean getProcessInstancePermissionDelete() {
        return processInstancePermissionDelete;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setProcessInstancePermissionDelete(Boolean processInstancePermissionDelete) {
        this.processInstancePermissionDelete = processInstancePermissionDelete;
        return this;
    }

    public Boolean getProcessInstancePermissionAnnotate() {
        return processInstancePermissionAnnotate;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setProcessInstancePermissionAnnotate(Boolean processInstancePermissionAnnotate) {
        this.processInstancePermissionAnnotate = processInstancePermissionAnnotate;
        return this;
    }
}
