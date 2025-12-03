import {VDepartmentUserRoleAssignmentWithDetailsEntity} from './v-department-user-role-assignment-with-details-entity';
import {User} from '../../users/models/user';

export interface VDepartmentMembershipWithDetailsEntity {
    id: number;
    departmentId: number;
    userId: string;
    created: string;
    updated: string;
    name: string;
    address?: string | null;
    imprint?: string | null;
    commonPrivacy?: string | null;
    commonAccessibility?: string | null;
    technicalSupportAddress?: string | null;
    specialSupportAddress?: string | null;
    departmentMail?: string | null;
    themeId?: number | null;
    technicalSupportPhone?: string | null;
    technicalSupportInfo?: string | null;
    specialSupportPhone?: string | null;
    specialSupportInfo?: string | null;
    additionalInfo?: string | null;
    depth: number;
    parentDepartmentId?: number | null;
    parentNames?: string[] | null;
    parentIds?: number[] | null;
    email?: string | null;
    firstName?: string | null;
    lastName?: string | null;
    fullName?: string | null;
    enabled?: boolean | null;
    verified?: boolean | null;
    globalRole?: 0 | 1 | 2 | null;
    superAdmin?: boolean | null;
    systemAdmin?: boolean | null;
    deletedInIdp?: boolean | null;
}

export interface VDepartmentMembershipWithDetailsEntityWithRoles extends VDepartmentMembershipWithDetailsEntity {
    roles: VDepartmentUserRoleAssignmentWithDetailsEntity[];
}

export function vDepartmentMembershipWithDetailsEntityAsUser(mem: VDepartmentMembershipWithDetailsEntity): User {
    return {
        id: mem.userId,
        firstName: mem.firstName ?? '',
        lastName: mem.lastName ?? '',
        enabled: mem.enabled ?? false,
        deletedInIdp: mem.deletedInIdp ?? true,
        fullName: mem.fullName ?? '',
        email: mem.email ?? '',
        globalRole: mem.globalRole ?? 0,
        isSuperAdmin: mem.superAdmin ?? false,
        isSystemAdmin: mem.systemAdmin ?? false,
        verified: mem.verified ?? false,
    };
}