import {TeamRequestDTO} from './dtos/team-request-dto';
import {TeamResponseDTO} from './dtos/team-response-dto';
import {BaseCrudApiService} from '../../services/base-crud-api-service';

interface TeamFilter {
    name: string;
}

export class TeamsApiService extends BaseCrudApiService<TeamRequestDTO, TeamResponseDTO, TeamResponseDTO, TeamRequestDTO, number, TeamFilter> {

    constructor() {
        super('/api/teams/');
    }

    public initialize(): TeamResponseDTO {
        return TeamsApiService.initialize();
    }

    public static initialize(): TeamResponseDTO {
        return {
            id: 0,
            name: '',
            created: '',
            updated: '',
        };
    }
}