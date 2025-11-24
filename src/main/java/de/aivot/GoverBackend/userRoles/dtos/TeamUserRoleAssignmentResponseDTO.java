package de.aivot.GoverBackend.userRoles.dtos;

import de.aivot.GoverBackend.teams.entities.VTeamMembershipWithDetailsEntity;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;
import de.aivot.GoverBackend.userRoles.entities.VTeamUserRoleAssignmentsWithDetailsEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Id;

public record TeamUserRoleAssignmentResponseDTO(
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
        Integer teamMembershipId,
        @Nonnull
        Integer teamMembershipTeamId,
        @Nonnull
        String teamMembershipTeamName,
        @Nonnull
        String teamMembershipUserId,
        @Nonnull
        String teamMembershipUserFirstName,
        @Nonnull
        String teamMembershipUserLastName,
        @Nonnull
        String teamMembershipUserFullName,
        @Nonnull
        String teamMembershipUserEmail,
        @Nonnull
        Boolean teamMembershipUserEnabled,
        @Nonnull
        Boolean teamMembershipUserVerified,
        @Nonnull
        Boolean teamMembershipUserGlobalAdmin,
        @Nonnull
        Boolean teamMembershipUserDeletedInIdp
) {
    public static TeamUserRoleAssignmentResponseDTO fromEntity(UserRoleAssignmentEntity ura, UserRoleEntity ur, VTeamMembershipWithDetailsEntity oum) {
        return new TeamUserRoleAssignmentResponseDTO(
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
                oum.getTeamId(),
                oum.getTeamName(),
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

    public static TeamUserRoleAssignmentResponseDTO fromEntity(VTeamUserRoleAssignmentsWithDetailsEntity entity) {
        return new TeamUserRoleAssignmentResponseDTO(
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

                entity.getTeamMembershipId(),
                entity.getTeamMembershipTeamId(),
                entity.getTeamMembershipTeamName(),
                entity.getTeamMembershipUserId(),
                entity.getTeamMembershipUserFirstName(),
                entity.getTeamMembershipUserLastName(),
                entity.getTeamMembershipUserFullName(),
                entity.getTeamMembershipUserEmail(),
                entity.getTeamMembershipUserEnabled(),
                entity.getTeamMembershipUserVerified(),
                entity.getTeamMembershipUserGlobalAdmin(),
                entity.getTeamMembershipUserDeletedInIdp()
        );
    }
}
