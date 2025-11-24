import {User} from '../../users/models/user';
import {OrgUserRoleAssignmentResponseDTO} from '../../user-roles/dtos/org-user-role-assignment-response-dto';

export interface DepartmentMembershipResponseDTO {
    membershipId: number;
    orgUnitId: number;
    orgUnitName: string;
    organizationalUnitParentOrgUnitId: number | null | undefined;
    organizationalUnitDepth: number;
    userId: string;
    userFirstName: string;
    userLastName: string;
    userFullName: string;
    userEmail: string;
    userEnabled: boolean;
    userDeletedInIdp: boolean;
    userVerified: boolean;
    userGlobalAdmin: boolean;
}

export function departmentMembershipResponseDTOasUser(membership: DepartmentMembershipResponseDTO): User {
    return {
        id: membership.userId,
        firstName: membership.userFirstName,
        lastName: membership.userLastName,
        fullName: membership.userFullName,
        enabled: membership.userEnabled,
        deletedInIdp: membership.userDeletedInIdp,
        globalAdmin: membership.userGlobalAdmin,
        verified: membership.userVerified,
        email: membership.userEmail,
    };
}

export interface DepartmentMembershipWithRoles extends DepartmentMembershipResponseDTO {
    roles: OrgUserRoleAssignmentResponseDTO[];
}


