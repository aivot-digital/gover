package de.aivot.GoverBackend.department.entities;

import de.aivot.GoverBackend.core.converters.JsonArrayConverter;
import jakarta.persistence.*;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "v_department_memberships_with_details")
public class VDepartmentMembershipWithDetailsEntity {
    @Id
    private Integer membershipId;
    private Boolean membershipHasDeputies;
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonArrayConverter.class)
    private List<Map<String, Object>> membershipDeputies;
    private String userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private String userFullName;
    private Boolean userEnabled;
    private Boolean userVerified;
    private Boolean userDeletedInIdp;
    private Integer userSystemRoleId;
    private Integer departmentId;
    private String departmentName;
    private String departmentAddress;
    private String departmentImprint;
    private String departmentCommonPrivacy;
    private String departmentCommonAccessibility;
    private String departmentTechnicalSupportAddress;
    private String departmentTechnicalSupportPhone;
    private String departmentTechnicalSupportInfo;
    private String departmentSpecialSupportAddress;
    private String departmentSpecialSupportPhone;
    private String departmentSpecialSupportInfo;
    private String departmentMail;
    private Integer departmentThemeId;
    private String departmentAdditionalInfo;
    private Integer departmentDepth;
    private Integer departmentParentDepartmentId;
    private List<String> departmentParentNames;
    private List<Integer> departmentParentIds;
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonArrayConverter.class)
    private List<Map<String, Object>> domainRoles;
    private List<String> domainRolePermissions;

    // Getters and Setters

    public Integer getMembershipId() {
        return membershipId;
    }

    public VDepartmentMembershipWithDetailsEntity setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    public Boolean getMembershipHasDeputies() {
        return membershipHasDeputies;
    }

    public VDepartmentMembershipWithDetailsEntity setMembershipHasDeputies(Boolean membershipHasDeputy) {
        this.membershipHasDeputies = membershipHasDeputy;
        return this;
    }

    public List<Map<String, Object>> getMembershipDeputies() {
        return membershipDeputies;
    }

    public VDepartmentMembershipWithDetailsEntity setMembershipDeputies(List<Map<String, Object>> membershipDeputies) {
        this.membershipDeputies = membershipDeputies;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VDepartmentMembershipWithDetailsEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public VDepartmentMembershipWithDetailsEntity setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public VDepartmentMembershipWithDetailsEntity setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
        return this;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public VDepartmentMembershipWithDetailsEntity setUserLastName(String userLastName) {
        this.userLastName = userLastName;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public VDepartmentMembershipWithDetailsEntity setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public VDepartmentMembershipWithDetailsEntity setUserEnabled(Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    public Boolean getUserVerified() {
        return userVerified;
    }

    public VDepartmentMembershipWithDetailsEntity setUserVerified(Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public VDepartmentMembershipWithDetailsEntity setUserDeletedInIdp(Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }

    public Integer getUserSystemRoleId() {
        return userSystemRoleId;
    }

    public VDepartmentMembershipWithDetailsEntity setUserSystemRoleId(Integer userSystemRoleId) {
        this.userSystemRoleId = userSystemRoleId;
        return this;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
        return this;
    }

    public String getDepartmentAddress() {
        return departmentAddress;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentAddress(String departmentAddress) {
        this.departmentAddress = departmentAddress;
        return this;
    }

    public String getDepartmentImprint() {
        return departmentImprint;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentImprint(String departmentImprint) {
        this.departmentImprint = departmentImprint;
        return this;
    }

    public String getDepartmentCommonPrivacy() {
        return departmentCommonPrivacy;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentCommonPrivacy(String departmentCommonPrivacy) {
        this.departmentCommonPrivacy = departmentCommonPrivacy;
        return this;
    }

    public String getDepartmentCommonAccessibility() {
        return departmentCommonAccessibility;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentCommonAccessibility(String departmentCommonAccessibility) {
        this.departmentCommonAccessibility = departmentCommonAccessibility;
        return this;
    }

    public String getDepartmentTechnicalSupportAddress() {
        return departmentTechnicalSupportAddress;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentTechnicalSupportAddress(String departmentTechnicalSupportAddress) {
        this.departmentTechnicalSupportAddress = departmentTechnicalSupportAddress;
        return this;
    }

    public String getDepartmentTechnicalSupportPhone() {
        return departmentTechnicalSupportPhone;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentTechnicalSupportPhone(String departmentTechnicalSupportPhone) {
        this.departmentTechnicalSupportPhone = departmentTechnicalSupportPhone;
        return this;
    }

    public String getDepartmentTechnicalSupportInfo() {
        return departmentTechnicalSupportInfo;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentTechnicalSupportInfo(String departmentTechnicalSupportInfo) {
        this.departmentTechnicalSupportInfo = departmentTechnicalSupportInfo;
        return this;
    }

    public String getDepartmentSpecialSupportAddress() {
        return departmentSpecialSupportAddress;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentSpecialSupportAddress(String departmentSpecialSupportAddress) {
        this.departmentSpecialSupportAddress = departmentSpecialSupportAddress;
        return this;
    }

    public String getDepartmentSpecialSupportPhone() {
        return departmentSpecialSupportPhone;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentSpecialSupportPhone(String departmentSpecialSupportPhone) {
        this.departmentSpecialSupportPhone = departmentSpecialSupportPhone;
        return this;
    }

    public String getDepartmentSpecialSupportInfo() {
        return departmentSpecialSupportInfo;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentSpecialSupportInfo(String departmentSpecialSupportInfo) {
        this.departmentSpecialSupportInfo = departmentSpecialSupportInfo;
        return this;
    }

    public String getDepartmentMail() {
        return departmentMail;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentMail(String departmentMail) {
        this.departmentMail = departmentMail;
        return this;
    }

    public Integer getDepartmentThemeId() {
        return departmentThemeId;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentThemeId(Integer departmentThemeId) {
        this.departmentThemeId = departmentThemeId;
        return this;
    }

    public String getDepartmentAdditionalInfo() {
        return departmentAdditionalInfo;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentAdditionalInfo(String departmentAdditionalInfo) {
        this.departmentAdditionalInfo = departmentAdditionalInfo;
        return this;
    }

    public Integer getDepartmentDepth() {
        return departmentDepth;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentDepth(Integer departmentDepth) {
        this.departmentDepth = departmentDepth;
        return this;
    }

    public Integer getDepartmentParentDepartmentId() {
        return departmentParentDepartmentId;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentParentDepartmentId(Integer departmentParentDepartmentId) {
        this.departmentParentDepartmentId = departmentParentDepartmentId;
        return this;
    }

    public List<String> getDepartmentParentNames() {
        return departmentParentNames;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentParentNames(List<String> departmentParentNames) {
        this.departmentParentNames = departmentParentNames;
        return this;
    }

    public List<Integer> getDepartmentParentIds() {
        return departmentParentIds;
    }

    public VDepartmentMembershipWithDetailsEntity setDepartmentParentIds(List<Integer> departmentParentIds) {
        this.departmentParentIds = departmentParentIds;
        return this;
    }

    public List<Map<String, Object>> getDomainRoles() {
        return domainRoles;
    }

    public VDepartmentMembershipWithDetailsEntity setDomainRoles(List<Map<String, Object>> domainRoles) {
        this.domainRoles = domainRoles;
        return this;
    }

    public List<String> getDomainRolePermissions() {
        return domainRolePermissions;
    }

    public VDepartmentMembershipWithDetailsEntity setDomainRolePermissions(List<String> domainRolePermissions) {
        this.domainRolePermissions = domainRolePermissions;
        return this;
    }

    // endregion
}
