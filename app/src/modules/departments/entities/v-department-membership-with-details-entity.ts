import {User} from '../../users/models/user';
import {UserRoleResponseDTO} from "../../user-roles/dtos/user-role-response-dto";
import {Permission} from "../../../data/permissions/permission";
import {KeysToSnakeCase} from "../../../utils/camel-to-snake";

export interface VDepartmentMembershipWithDetailsEntity {
    membershipId: number;

    membershipHasDeputies: boolean;
    membershipDeputies: KeysToSnakeCase<User>[];

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
    departmentCommonPrivacy: string | null;
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

    domainRoles: KeysToSnakeCase<UserRoleResponseDTO>[];
    domainRolePermissions: Permission[];
}
