import {VDepartmentUserRoleAssignmentWithDetailsEntity} from './v-department-user-role-assignment-with-details-entity';
import {User} from '../../users/models/user';
import {UserRoleResponseDTO} from "../../user-roles/dtos/user-role-response-dto";
import {Permission} from "../../../data/permissions/permission";

export interface VDepartmentMembershipWithDetailsEntity {
    membershipId: number;

    membershipIsDeputy: boolean;
    membershipAsDeputyForUserId: string | null;
    membershipAsDeputyForUserEmail: string | null;
    membershipAsDeputyForUserFirstName: string | null;
    membershipAsDeputyForUserLastName: string | null;
    membershipAsDeputyForUserFullName: string | null;
    membershipAsDeputyForUserEnabled: boolean | null;
    membershipAsDeputyForUserVerified: boolean | null;
    membershipAsDeputyForUserDeletedInIdp: boolean | null;
    membershipAsDeputyForUserSystemRoleId: number | null;

    userId: string;
    userEmail: string | null;
    userFirstName: string | null;
    userLastName: string | null;
    userFullName: string | null;
    userEnabled: boolean;
    userVerified: boolean;
    userDeletedInIdp: boolean;
    userSystemRoleId: number;

    departmentId: number;
    departmentName: string;
    departmentAddress: string | null;
    departmentImprint: string | null;
    departmentCommonPrivacy?: string;
    departmentCommonAccessibility: string | null;
    departmentTechnicalSupportAddress: string | null;
    departmentTechnicalSupportPhone: string | null;
    departmentTechnicalSupportInfo: string | null;
    departmentSpecialSupportAddress: string | null;
    departmentSpecialSupportPhone: string | null;
    departmentSpecialSupportInfo: string | null;
    departmentMail: string | null;
    departmentThemeId: number | null;
    departmentAdditionalInfo: string | null;
    departmentDepth: number;
    departmentParentDepartmentId: number | null;
    departmentParentNames: string[] | null;
    departmentParentIds: number[] | null;

    domainRoles: UserRoleResponseDTO[];
    domainRolePermissions: Permission[];
}

export interface VDepartmentMembershipWithDetailsEntityWithRoles extends VDepartmentMembershipWithDetailsEntity {
    roles: VDepartmentUserRoleAssignmentWithDetailsEntity[];
}

export function vDepartmentMembershipWithDetailsEntityAsUser(mem: VDepartmentMembershipWithDetailsEntity): User {
    return {
        id: mem.userId,
        firstName: mem.userFirstName ?? '',
        lastName: mem.userLastName ?? '',
        enabled: mem.userEnabled ?? false,
        deletedInIdp: mem.userDeletedInIdp ?? true,
        fullName: mem.userFullName ?? '',
        email: mem.userEmail ?? '',
        globalRole: mem.userSystemRoleId ?? 0,
        isSuperAdmin: (mem.userSystemRoleId ?? 2) === 0,
        isSystemAdmin: (mem.userSystemRoleId ?? 2) === 0,
        verified: mem.userVerified ?? false,
        systemRoleId: mem.userSystemRoleId,
    };
}