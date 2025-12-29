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
    userFullName: string;
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
    string,
    VDepartmentMembershipWithDetailsFilter
> {
    public constructor() {
        super('/api/department-memberships-with-details/');
    }

    public initialize(): VDepartmentMembershipWithDetailsEntity {
        return {
            departmentAdditionalInfo: null,
            departmentAddress: null,
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
            membershipAsDeputyForUserDeletedInIdp: null,
            membershipAsDeputyForUserEmail: null,
            membershipAsDeputyForUserEnabled: null,
            membershipAsDeputyForUserFirstName: null,
            membershipAsDeputyForUserFullName: null,
            membershipAsDeputyForUserId: null,
            membershipAsDeputyForUserLastName: null,
            membershipAsDeputyForUserSystemRoleId: null,
            membershipAsDeputyForUserVerified: null,
            userEmail: null,
            userFirstName: null,
            userFullName: null,
            userLastName: null,
            departmentDepth: 0,
            departmentId: 0,
            departmentName: "",
            domainRolePermissions: [],
            domainRoles: [],
            membershipId: 0,
            membershipIsDeputy: false,
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
                userFullName: filters?.userSearch,
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

        console.log('Assignments:', assignments);
        console.log('Memberships:', memberships);

        const membershipsWithRoles: VDepartmentMembershipWithDetailsEntityWithRoles[] = memberships
            .map((membership) => {
                const membershipRoles = assignments
                    .filter((assignment) => assignment.id === membership.membershipId);

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