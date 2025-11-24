import {BaseCrudApiService} from '../../services/base-crud-api-service';
import {TeamMembershipRequestDTO} from './dtos/team-membership-request-dto';
import {TeamMembershipResponseDTO} from './dtos/team-membership-response-dto';

interface TeamMembershipFilter {
    teamId: number;
    userId: string;
}

export class TeamMembershipsApiService extends BaseCrudApiService<TeamMembershipRequestDTO, TeamMembershipResponseDTO, TeamMembershipResponseDTO, TeamMembershipRequestDTO, number, TeamMembershipFilter> {
    constructor() {
        super('/api/team-memberships/');
    }

    public initialize(): TeamMembershipResponseDTO {
        return {
            created: '',
            id: 0,
            teamId: 0,
            updated: '',
            userId: '',
        };
    }
}