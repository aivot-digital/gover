import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {VTeamMembershipWithDetailsEntity} from '../entities/v-team-membership-with-details-entity';

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
    deletedInIdp: boolean;
    domainRoleId: number;
}

export class VTeamMembershipWithDetailsService extends BaseCrudApiService<
    VTeamMembershipWithDetailsEntity,
    VTeamMembershipWithDetailsEntity,
    VTeamMembershipWithDetailsEntity,
    VTeamMembershipWithDetailsEntity,
    number,
    VTeamMembershipWithDetailsFilter
> {
    public constructor() {
        super('/api/team-memberships-with-details/');
    }

    public initialize(): VTeamMembershipWithDetailsEntity {
        return VTeamMembershipWithDetailsService.initialize();
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
            teamName: '',
            userDeletedInIdp: false,
            userEnabled: false,
            userId: '',
            userSystemRoleId: 0,
            userVerified: false,
            domainRoleAssignments: [],
        };
    }
}
