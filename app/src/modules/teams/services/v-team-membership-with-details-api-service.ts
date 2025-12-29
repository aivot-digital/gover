import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {
    VTeamMembershipWithDetailsEntity,
} from "../entities/v-team-membership-with-details-entity";
import {SystemUserRole} from "../../users/models/user";
import {TeamMembershipsApiService} from "./team-memberships-api-service";
import {SortOrder} from "../../../components/generic-list/generic-list-props";
import {Page} from "../../../models/dtos/page";
import {VTeamUserRoleAssignmentWithDetailsApiService} from "./v-team-user-role-assignment-with-details-api-service";

interface VTeamMembershipWithDetailsFilter {
    teamIds: number[];
    teamId: number;
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

export type ListTeamMembershipsWithRolesFilter = Partial<{
    teamId: number;
    teamSearch: string;
    userId: string;
    userSearch: string;
    deletedUser: boolean;
    enabledUser: boolean;
}>;

export class VTeamMembershipWithDetailsApiService extends BaseCrudApiService<
    VTeamMembershipWithDetailsEntity,
    VTeamMembershipWithDetailsEntity,
    VTeamMembershipWithDetailsEntity,
    VTeamMembershipWithDetailsEntity,
    number,
    VTeamMembershipWithDetailsFilter
> {

    constructor() {
        super('/api/team-memberships-with-details/');
    }

    public initialize(): VTeamMembershipWithDetailsEntity {
        return VTeamMembershipWithDetailsApiService.initialize();
    }

    public static initialize(): VTeamMembershipWithDetailsEntity {
        return {
            membershipAsDeputyForUserDeletedInIdp: null,
            membershipAsDeputyForUserEmail: null,
            membershipAsDeputyForUserEnabled: null,
            membershipAsDeputyForUserFirstName: null,
            membershipAsDeputyForUserFullName: null,
            membershipAsDeputyForUserId: null,
            membershipAsDeputyForUserLastName: null,
            membershipAsDeputyForUserSystemRoleId: null,
            membershipAsDeputyForUserVerified: null,
            membershipIsDeputy: false,
            userEmail: null,
            userFirstName: null,
            userFullName: null,
            userLastName: null,
            domainRolePermissions: [],
            domainRoles: [],
            membershipId: 0,
            teamId: 0,
            teamName: "",
            userDeletedInIdp: false,
            userEnabled: false,
            userId: "",
            userSystemRoleId: 0,
            userVerified: false
        };
    }

    public async listTeamMembershipsWithRoles(
        page: number,
        limit: number,
        sort?: 'name' | 'fullName',
        order?: SortOrder,
        filters?: Partial<ListTeamMembershipsWithRolesFilter>,
    ): Promise<Page<VTeamMembershipWithDetailsEntity>> {
        const userRoleAssignmentService = new VTeamUserRoleAssignmentWithDetailsApiService();

        const [assignmentsPage, membershipsPage] = await Promise.all([
            userRoleAssignmentService.listAll({
                teamId: filters?.teamId,
                fullName: filters?.userSearch,
                name: filters?.teamSearch,
                userId: filters?.userId,
            }),
            this.list(page, limit, sort as any, order, {
                userId: filters?.userId,
                name: filters?.teamSearch,
                teamId: filters?.teamId,
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

        const membershipsWithRoles: VTeamMembershipWithDetailsEntity[] = memberships
            .map((membership) => {
                const membershipRoles = assignments
                    .filter((assignment) => assignment.membershipId === membership.membershipId);

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