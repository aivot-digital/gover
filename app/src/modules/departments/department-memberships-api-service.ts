import {DepartmentMembershipResponseDTO, DepartmentMembershipWithRoles} from './dtos/department-membership-response-dto';
import {DepartmentMembershipRequestDTO} from './dtos/department-membership-request-dto';
import {BaseCrudApiService} from '../../services/base-crud-api-service';
import {Page} from '../../models/dtos/page';
import {OrgUserRoleAssignmentsApiService} from '../user-roles/org-user-role-assignments-api-service';
import {SortOrder} from '../../components/generic-list/generic-list-props';

export interface DepartmentMembershipFilters {
    organizationalUnitIds: number[];
    organizationalUnitId: number;
    organizationalUnitName: string;

    userId: string;
    userIds: string[];
    userFullName: string;
    userEmail: string;

    userEnabled: boolean;
    userVerified: boolean;
    userGlobalAdmin: boolean;
    userDeletedInIdp: boolean;
}

export type ListDepartmentMembershipsWithRolesFilter = Partial<{
    organizationalUnitId: number;
    organizationalUnitSearch: string;
    userId: string;
    userSearch: string;
    deletedUser: boolean;
    enabledUser: boolean;
}>;

export class DepartmentMembershipsApiService extends BaseCrudApiService<DepartmentMembershipRequestDTO, DepartmentMembershipResponseDTO, DepartmentMembershipResponseDTO, DepartmentMembershipRequestDTO, number, DepartmentMembershipFilters> {
    public constructor() {
        super('/api/organizational-unit-memberships/');
    }

    public initialize(): DepartmentMembershipResponseDTO {
        return {
            organizationalUnitDepth: 0,
            organizationalUnitParentOrgUnitId: undefined,
            membershipId: 0,
            orgUnitId: 0,
            orgUnitName: '',
            userId: '',
            userEmail: '',
            userFirstName: '',
            userLastName: '',
            userFullName: '',
            userEnabled: false,
            userDeletedInIdp: true,
            userGlobalAdmin: false,
            userVerified: false,
        };
    }

    public async listDepartmentMembershipsWithRoles(
        page: number,
        limit: number,
        sort?: 'organizationalUnitName' | 'userFullName',
        order?: SortOrder,
        filters?:ListDepartmentMembershipsWithRolesFilter,
    ): Promise<Page<DepartmentMembershipWithRoles>> {
        const [assignmentsPage, membershipsPage] = await Promise.all([
            new OrgUserRoleAssignmentsApiService().listAll({
                orgUnitMembershipOrganizationalUnitId: filters?.organizationalUnitId,
                orgUnitMembershipUserFullName: filters?.userSearch,

                orgUnitMembershipOrganizationalUnitName: filters?.organizationalUnitSearch,
                orgUnitMembershipUserId: filters?.userId,
            }),
            this.list(page, limit, sort as any, order, {
                userId: filters?.userId,
                organizationalUnitName: filters?.organizationalUnitSearch,
                organizationalUnitId: filters?.organizationalUnitId,
                userFullName: filters?.userSearch,
                userDeletedInIdp: filters?.deletedUser,
                userEnabled: filters?.enabledUser,
            }),
        ]);

        const {
            content: assignments,
        } = assignmentsPage;

        const {
            content: memberships,
        } = membershipsPage;

        const membershipsWithRoles: DepartmentMembershipWithRoles[] = memberships
            .map((membership) => {
                const membershipRoles = assignments
                    .filter((assignment) => assignment.orgUnitMembershipId === membership.membershipId);

                return {
                    ...membership,
                    roles: membershipRoles,
                };
            });

        return {
            ...membershipsPage,
            content: membershipsWithRoles,
        };
    }
}