export interface UserRoleAssignmentEntity {
    id: number;
    departmentMembershipId: number | null;
    teamMembershipId: number | null;
    userRoleId: number;
    created: string;
}