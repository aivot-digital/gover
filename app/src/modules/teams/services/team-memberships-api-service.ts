import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {TeamMembershipEntity} from '../entities/team-membership-entity';

interface TeamMembershipFilter {
    teamId: number;
    userId: string;
}

export interface TeamMembershipCreateRequest {
    teamId: number;
    userId: string;
    roleIds?: number[];
}

export class TeamMembershipsApiService extends BaseCrudApiService<
    TeamMembershipCreateRequest,
    TeamMembershipEntity,
    TeamMembershipEntity,
    TeamMembershipEntity,
    number,
    TeamMembershipFilter
> {
    constructor() {
        super('/api/team-memberships/');
    }

    public initialize(): TeamMembershipEntity {
        return TeamMembershipsApiService.initialize();
    }

    public static initialize(): TeamMembershipEntity {
        return {
            id: 0,
            teamId: 0,
            userId: '',
            created: '',
            updated: '',
        };
    }
}
