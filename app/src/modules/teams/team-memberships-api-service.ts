import {BaseCrudApiService} from '../../services/base-crud-api-service';
import {TeamMembershipRequestDTO} from './dtos/team-membership-request-dto';
import {TeamMembershipResponseDTO, TeamMembershipWithRoles} from './dtos/team-membership-response-dto';
import {SortOrder} from '../../components/generic-list/generic-list-props';
import {Page} from '../../models/dtos/page';
import {TeamUserRoleAssignmentsApiService} from '../user-roles/team-user-role-assignments-api-service';

interface TeamMembershipFilter {
    teamIds: number[];
    teamId: number;
    teamName: string;

    userId: string;
    userIds: string[];
    userFullName: string;
    userEmail: string;
    userEnabled: boolean;
    userVerified: boolean;
    userGlobalAdmin: boolean;
    userDeletedInIdp: boolean;
}

export type ListTeamMembershipsWithRolesFilter = Partial<{
    teamId: number;
    teamSearch: string;
    userId: string;
    userSearch: string;
    deletedUser: boolean;
    enabledUser: boolean;
}>;

export class TeamMembershipsApiService extends BaseCrudApiService<TeamMembershipRequestDTO, TeamMembershipResponseDTO, TeamMembershipResponseDTO, TeamMembershipRequestDTO, number, TeamMembershipFilter> {
    constructor() {
        super('/api/team-memberships/');
    }

    public initialize(): TeamMembershipResponseDTO {
        return {
            id: 0,
            teamId: 0,
            teamName: '',
            userDeletedInIdp: false,
            userEmail: '',
            userEnabled: false,
            userFirstName: '',
            userFullName: '',
            userGlobalAdmin: false,
            userId: '',
            userLastName: '',
            userVerified: false,
        };
    }

    public async listTeamMembershipsWithRoles(
        page: number,
        limit: number,
        sort?: 'organizationalUnitName' | 'userFullName',
        order?: SortOrder,
        filters?: ListTeamMembershipsWithRolesFilter,
    ): Promise<Page<TeamMembershipWithRoles>> {
        const [assignmentsPage, membershipsPage] = await Promise.all([
            new TeamUserRoleAssignmentsApiService().listAll({
                teamMembershipTeamId: filters?.teamId,
                teamMembershipUserFullName: filters?.userSearch,

                teamMembershipTeamName: filters?.teamSearch,
                teamMembershipUserId: filters?.userId,
            }),
            this.list(page, limit, sort as any, order, {
                userId: filters?.userId,
                teamName: filters?.teamSearch,
                teamId: filters?.teamId,
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

        const membershipsWithRoles: TeamMembershipWithRoles[] = memberships
            .map((membership) => {
                const membershipRoles = assignments
                    .filter((assignment) => assignment.teamMembershipId === membership.id);

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