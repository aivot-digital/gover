export interface UserRoleAssignmentRequestDTO {
    departmentMembershipId?: number | null;
    teamMembershipId?: number | null;
    userRoleId: number;
}