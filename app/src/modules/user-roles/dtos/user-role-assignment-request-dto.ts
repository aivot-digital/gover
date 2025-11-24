export interface UserRoleAssignmentRequestDTO {
    organizationalUnitMembershipId?: number | null;
    teamMembershipId?: number | null;
    userRoleId: number;
}