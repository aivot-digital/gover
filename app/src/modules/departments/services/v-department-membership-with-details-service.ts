import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {VDepartmentMembershipWithDetailsEntity} from '../entities/v-department-membership-with-details-entity';
import {SortOrder} from '../../../components/generic-list/generic-list-props';

interface VDepartmentMembershipWithDetailsFilter {
    departmentId: number;
    departmentIds: number[];
    name: string;
    userId: string;
    userIds: string[];
    userFullName: string;
    email: string;
    enabled: boolean;
    verified: boolean;
    globalAdmin: boolean;
    deletedInIdp: boolean;
    domainRoleId: number;
}

export interface ListDepartmentMembershipsWithRolesFilter {
    userId: string;
    departmentSearch: string;
    departmentId: number;
    userSearch: string;
    deletedUser: boolean;
    enabledUser: boolean;
}

export class VDepartmentMembershipWithDetailsService extends BaseCrudApiService<
    VDepartmentMembershipWithDetailsEntity,
    VDepartmentMembershipWithDetailsEntity,
    VDepartmentMembershipWithDetailsEntity,
    VDepartmentMembershipWithDetailsEntity,
    string,
    VDepartmentMembershipWithDetailsFilter
> {
    public constructor() {
        super('/api/department-memberships-with-details/');
    }

    public initialize(): VDepartmentMembershipWithDetailsEntity {
        return {
            membershipDeputies: [],
            departmentAdditionalInfo: null,
            departmentAddress: null,
            departmentCommonPrivacy: null,
            departmentCommonAccessibility: null,
            departmentImprint: null,
            departmentMail: null,
            departmentParentDepartmentId: null,
            departmentParentIds: null,
            departmentParentNames: null,
            departmentSpecialSupportAddress: null,
            departmentSpecialSupportInfo: null,
            departmentSpecialSupportPhone: null,
            departmentTechnicalSupportAddress: null,
            departmentTechnicalSupportInfo: null,
            departmentTechnicalSupportPhone: null,
            departmentThemeId: null,
            userEmail: null,
            userFirstName: null,
            userFullName: null,
            userLastName: null,
            departmentDepth: 0,
            departmentId: 0,
            departmentName: "",
            domainRolePermissions: [],
            domainRoleAssignments: [],
            domainRoles: [],
            membershipId: 0,
            membershipHasDeputies: false,
            userDeletedInIdp: false,
            userEnabled: false,
            userId: "",
            userSystemRoleId: 0,
            userVerified: false
        };
    }

    public async listDepartmentMembershipsWithRoles(
        page: number,
        limit: number,
        sort?: 'name' | 'fullName',
        order?: SortOrder,
        filters?: Partial<ListDepartmentMembershipsWithRolesFilter>,
    ): Promise<any> {
        return {};
    }
}