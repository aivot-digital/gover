import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {
    VTeamMembershipWithDetailsEntity,
} from "../entities/v-team-membership-with-details-entity";
import {SortOrder} from "../../../components/generic-list/generic-list-props";
import {Page} from "../../../models/dtos/page";

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

export type ListTeamMembershipsWithDetailsFilter = Partial<{
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
            membershipDeputies: [],
            membershipHasDeputies: false,
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
            userVerified: false,
            domainRoleAssignments: [],
        };
    }

    public async listTeamMembershipsWithDetails(
        page: number,
        limit: number,
        sort?: 'name' | 'fullName',
        order?: SortOrder,
        filters?: Partial<ListTeamMembershipsWithDetailsFilter>,
    ): Promise<Page<VTeamMembershipWithDetailsEntity>> {
        // Domain roles are embedded in the membership details response and redacted server-side when domain_role.read is missing.
        return this.list(page, limit, sort as any, order, {
            userId: filters?.userId,
            name: filters?.teamSearch,
            teamId: filters?.teamId,
            fullName: filters?.userSearch,
            deletedInIdp: filters?.deletedUser,
            enabled: filters?.enabledUser,
        });
    }
}
