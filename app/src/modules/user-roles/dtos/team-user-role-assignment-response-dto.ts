export interface TeamUserRoleAssignmentResponseDTO {
    userRoleAssignmentId: number;

    userRoleId: number;
    userRoleName: string;
    userRoleDescription?: string;
    userRoleOrgUnitMemberPermissionEdit: boolean;
    userRoleTeamMemberPermissionEdit: boolean;
    userRoleFormPermissionCreate: boolean;
    userRoleFormPermissionRead: boolean;
    userRoleFormPermissionEdit: boolean;
    userRoleFormPermissionDelete: boolean;
    userRoleFormPermissionAnnotate: boolean;
    userRoleFormPermissionPublish: boolean;
    userRoleProcessPermissionCreate: boolean;
    userRoleProcessPermissionRead: boolean;
    userRoleProcessPermissionEdit: boolean;
    userRoleProcessPermissionDelete: boolean;
    userRoleProcessPermissionAnnotate: boolean;
    userRoleProcessPermissionPublish: boolean;
    userRoleProcessInstancePermissionCreate: boolean;
    userRoleProcessInstancePermissionRead: boolean;
    userRoleProcessInstancePermissionEdit: boolean;
    userRoleProcessInstancePermissionDelete: boolean;
    userRoleProcessInstancePermissionAnnotate: boolean;

    teamMembershipId: number;
    teamMembershipTeamId: number;
    teamMembershipTeamName: string;
    teamMembershipUserId: string;
    teamMembershipUserFirstName: string;
    teamMembershipUserLastName: string;
    teamMembershipUserFullName: string;
    teamMembershipUserEmail: string;
    teamMembershipUserEnabled: boolean;
    teamMembershipUserVerified: boolean;
    teamMembershipUserGlobalAdmin: boolean;
    teamMembershipUserDeletedInIdp: boolean;
}