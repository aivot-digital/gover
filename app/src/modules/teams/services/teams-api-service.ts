import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {TeamEntity} from "../entities/team-entity";

interface TeamFilter {
    name: string;
}

export class TeamsApiService extends BaseCrudApiService<TeamEntity, TeamEntity, TeamEntity, TeamEntity, number, TeamFilter> {

    constructor() {
        super('/api/teams/');
    }

    public initialize(): TeamEntity {
        return TeamsApiService.initialize();
    }

    public static initialize(): TeamEntity {
        return {
            id: 0,
            name: '',
            created: '',
            updated: '',
        };
    }
}