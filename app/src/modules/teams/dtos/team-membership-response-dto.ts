import {TeamUserRoleAssignmentResponseDTO} from '../../user-roles/dtos/team-user-role-assignment-response-dto';

export interface TeamMembershipResponseDTO {
    id: number;
    teamId: number;
    teamName: string;
    userId: string;
    userFirstName: string;
    userLastName: string;
    userFullName: string;
    userEmail: string;
    userEnabled: boolean;
    userVerified: boolean;
    userGlobalAdmin: boolean;
    userDeletedInIdp: boolean;
}

export interface TeamMembershipWithRoles extends TeamMembershipResponseDTO {
    roles: TeamUserRoleAssignmentResponseDTO[];
}