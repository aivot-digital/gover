import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {VDepartmentMembershipWithDetailsEntity, VDepartmentMembershipWithDetailsEntityWithRoles} from '../entities/v-department-membership-with-details-entity';
import {SortOrder} from '../../../components/generic-list/generic-list-props';
import {Page} from '../../../models/dtos/page';
import {VDepartmentUserRoleAssignmentWithDetailsService} from './v-department-user-role-assignment-with-details-service';

interface VDepartmentMembershipWithDetailsFilter {
    departmentId: number;
    departmentIds: number[];
    name: string;
    userId: string;
    userIds: string[];
    fullName: string;
    email: string;
    enabled: boolean;
    verified: boolean;
    globalAdmin: boolean;
    deletedInIdp: boolean;
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
    number,
    VDepartmentMembershipWithDetailsFilter
> {
    public constructor() {
        super('/api/department-memberships-with-details/');
    }

    public initialize(): VDepartmentMembershipWithDetailsEntity {
        return {
            created: '',
            departmentId: 0,
            depth: 0,
            id: 0,
            name: '',
            updated: '',
            userId: '',
        };
    }

    public async listDepartmentMembershipsWithRoles(
        page: number,
        limit: number,
        sort?: 'organizationalUnitName' | 'userFullName',
        order?: SortOrder,
        filters?: Partial<ListDepartmentMembershipsWithRolesFilter>,
    ): Promise<Page<VDepartmentMembershipWithDetailsEntityWithRoles>> {
        const userRoleAssignmentService = new VDepartmentUserRoleAssignmentWithDetailsService();

        const [assignmentsPage, membershipsPage] = await Promise.all([
            userRoleAssignmentService.listAll({
                departmentId: filters?.departmentId,
                fullName: filters?.userSearch,
                name: filters?.departmentSearch,
                userId: filters?.userId,
            }),
            this.list(page, limit, sort as any, order, {
                userId: filters?.userId,
                name: filters?.departmentSearch,
                departmentId: filters?.departmentId,
                fullName: filters?.userSearch,
                deletedInIdp: filters?.deletedUser,
                enabled: filters?.enabledUser,
            }),
        ]);

        const {
            content: assignments,
        } = assignmentsPage;

        const {
            content: memberships,
        } = membershipsPage;

        const membershipsWithRoles: VDepartmentMembershipWithDetailsEntityWithRoles[] = memberships
            .map((membership) => {
                const membershipRoles = assignments
                    .filter((assignment) => assignment.id === membership.id);

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