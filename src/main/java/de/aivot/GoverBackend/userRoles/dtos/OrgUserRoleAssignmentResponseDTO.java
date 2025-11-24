package de.aivot.GoverBackend.userRoles.dtos;

import de.aivot.GoverBackend.department.entities.VOrganizationalUnitMembershipWithDetailsEntity;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;
import de.aivot.GoverBackend.userRoles.entities.VOrgUserRoleAssignmentsWithDetailsEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Id;

public record OrgUserRoleAssignmentResponseDTO(
        @Id
        Integer userRoleAssignmentId,

        @Nonnull
        Integer userRoleId,
        @Nonnull
        String userRoleName,
        @Nullable
        String userRoleDescription,
        @Nonnull
        Boolean userRoleOrgUnitMemberPermissionEdit,
        @Nonnull
        Boolean userRoleTeamMemberPermissionEdit,
        @Nonnull
        Boolean userRoleFormPermissionCreate,
        @Nonnull
        Boolean userRoleFormPermissionRead,
        @Nonnull
        Boolean userRoleFormPermissionEdit,
        @Nonnull
        Boolean userRoleFormPermissionDelete,
        @Nonnull
        Boolean userRoleFormPermissionAnnotate,
        @Nonnull
        Boolean userRoleFormPermissionPublish,
        @Nonnull
        Boolean userRoleProcessPermissionCreate,
        @Nonnull
        Boolean userRoleProcessPermissionRead,
        @Nonnull
        Boolean userRoleProcessPermissionEdit,
        @Nonnull
        Boolean userRoleProcessPermissionDelete,
        @Nonnull
        Boolean userRoleProcessPermissionAnnotate,
        @Nonnull
        Boolean userRoleProcessPermissionPublish,
        @Nonnull
        Boolean userRoleProcessInstancePermissionCreate,
        @Nonnull
        Boolean userRoleProcessInstancePermissionRead,
        @Nonnull
        Boolean userRoleProcessInstancePermissionEdit,
        @Nonnull
        Boolean userRoleProcessInstancePermissionDelete,
        @Nonnull
        Boolean userRoleProcessInstancePermissionAnnotate,

        @Nonnull
        Integer orgUnitMembershipId,
        @Nonnull
        Integer orgUnitMembershipOrganizationalUnitId,
        @Nonnull
        String orgUnitMembershipOrganizationalUnitName,
        @Nullable
        Integer orgUnitMembershipOrganizationalUnitParentOrgUnitId,
        @Nonnull
        Integer orgUnitMembershipOrganizationalUnitDepth,
        @Nonnull
        String orgUnitMembershipUserId,
        @Nonnull
        String orgUnitMembershipUserFirstName,
        @Nonnull
        String orgUnitMembershipUserLastName,
        @Nonnull
        String orgUnitMembershipUserFullName,
        @Nonnull
        String orgUnitMembershipUserEmail,
        @Nonnull
        Boolean orgUnitMembershipUserEnabled,
        @Nonnull
        Boolean orgUnitMembershipUserVerified,
        @Nonnull
        Boolean orgUnitMembershipUserGlobalAdmin,
        @Nonnull
        Boolean orgUnitMembershipUserDeletedInIdp
) {
    public static OrgUserRoleAssignmentResponseDTO fromEntity(UserRoleAssignmentEntity ura, UserRoleEntity ur, VOrganizationalUnitMembershipWithDetailsEntity oum) {
        return new OrgUserRoleAssignmentResponseDTO(
                ura.getId(),

                ur.getId(),
                ur.getName(),
                ur.getDescription(),
                ur.getOrgUnitMemberPermissionEdit(),
                ur.getTeamMemberPermissionEdit(),
                ur.getFormPermissionCreate(),
                ur.getFormPermissionRead(),
                ur.getFormPermissionEdit(),
                ur.getFormPermissionDelete(),
                ur.getFormPermissionAnnotate(),
                ur.getFormPermissionPublish(),
                ur.getProcessPermissionCreate(),
                ur.getProcessPermissionRead(),
                ur.getProcessPermissionEdit(),
                ur.getProcessPermissionDelete(),
                ur.getProcessPermissionAnnotate(),
                ur.getProcessPermissionPublish(),
                ur.getProcessInstancePermissionCreate(),
                ur.getProcessInstancePermissionRead(),
                ur.getProcessInstancePermissionEdit(),
                ur.getProcessInstancePermissionDelete(),
                ur.getProcessInstancePermissionAnnotate(),

                oum.getMembershipId(),
                oum.getOrganizationalUnitId(),
                oum.getOrganizationalUnitName(),
                oum.getOrganizationalUnitParentOrgUnitId(),
                oum.getOrganizationalUnitDepth(),
                oum.getUserId(),
                oum.getUserFirstName(),
                oum.getUserLastName(),
                oum.getUserFullName(),
                oum.getUserEmail(),
                oum.getUserEnabled(),
                oum.getUserVerified(),
                oum.getUserGlobalAdmin(),
                oum.getUserDeletedInIdp()
        );
    }

    public static OrgUserRoleAssignmentResponseDTO fromEntity(VOrgUserRoleAssignmentsWithDetailsEntity entity) {
        return new OrgUserRoleAssignmentResponseDTO(
                entity.getUserRoleAssignmentId(),

                entity.getUserRoleId(),
                entity.getUserRoleName(),
                entity.getUserRoleDescription(),
                entity.getUserRoleOrgUnitMemberPermissionEdit(),
                entity.getUserRoleTeamMemberPermissionEdit(),
                entity.getUserRoleFormPermissionCreate(),
                entity.getUserRoleFormPermissionRead(),
                entity.getUserRoleFormPermissionEdit(),
                entity.getUserRoleFormPermissionDelete(),
                entity.getUserRoleFormPermissionAnnotate(),
                entity.getUserRoleFormPermissionPublish(),
                entity.getUserRoleProcessPermissionCreate(),
                entity.getUserRoleProcessPermissionRead(),
                entity.getUserRoleProcessPermissionEdit(),
                entity.getUserRoleProcessPermissionDelete(),
                entity.getUserRoleProcessPermissionAnnotate(),
                entity.getUserRoleProcessPermissionPublish(),
                entity.getUserRoleProcessInstancePermissionCreate(),
                entity.getUserRoleProcessInstancePermissionRead(),
                entity.getUserRoleProcessInstancePermissionEdit(),
                entity.getUserRoleProcessInstancePermissionDelete(),
                entity.getUserRoleProcessInstancePermissionAnnotate(),

                entity.getOrgUnitMembershipId(),
                entity.getOrgUnitMembershipOrganizationalUnitId(),
                entity.getOrgUnitMembershipOrganizationalUnitName(),
                entity.getOrgUnitMembershipOrganizationalUnitParentOrgUnitId(),
                entity.getOrgUnitMembershipOrganizationalUnitDepth(),
                entity.getOrgUnitMembershipUserId(),
                entity.getOrgUnitMembershipUserFirstName(),
                entity.getOrgUnitMembershipUserLastName(),
                entity.getOrgUnitMembershipUserFullName(),
                entity.getOrgUnitMembershipUserEmail(),
                entity.getOrgUnitMembershipUserEnabled(),
                entity.getOrgUnitMembershipUserVerified(),
                entity.getOrgUnitMembershipUserGlobalAdmin(),
                entity.getOrgUnitMembershipUserDeletedInIdp()
        );
    }
}
