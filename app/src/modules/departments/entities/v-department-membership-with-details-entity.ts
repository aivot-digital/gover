import {VDepartmentUserRoleAssignmentWithDetailsEntity} from './v-department-user-role-assignment-with-details-entity';
import {User} from '../../users/models/user';
import {UserRoleResponseDTO} from "../../user-roles/dtos/user-role-response-dto";
import {Permission} from "../../../data/permissions/permission";

export interface VDepartmentMembershipWithDetailsEntity {
    membershipId: number;
    membershipIsDeputy: boolean;
    membershipAsDeputyForUserId?: string;
    userId: string;
    userEmail?: string;
    userFirstName?: string;
    userLastName?: string;
    userFullName?: string;
    userEnabled: boolean;
    userVerified: boolean;
    userDeletedInIdp: boolean;
    userSystemRoleId: number;
    departmentId: number;
    departmentName: string;
    departmentAddress?: string;
    departmentImprint?: string;
    departmentCommonPrivacy?: string;
    departmentCommonAccessibility?: string;
    departmentTechnicalSupportAddress?: string;
    departmentTechnicalSupportPhone?: string;
    departmentTechnicalSupportInfo?: string;
    departmentSpecialSupportAddress?: string;
    departmentSpecialSupportPhone?: string;
    departmentSpecialSupportInfo?: string;
    departmentMail?: string;
    departmentThemeId?: number;
    departmentAdditionalInfo?: string;
    departmentDepth: number;
    departmentParentDepartmentId?: number;
    departmentParentNames?: string[];
    departmentParentIds?: number[];
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