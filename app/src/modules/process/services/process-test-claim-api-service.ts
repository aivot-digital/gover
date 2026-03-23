import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {type ProcessTestClaimEntity} from '../entities/process-test-claim-entity';

interface ProcessTestClaimFilter {
    processId: number;
    processVersion: number;
    owningUserId: string;
}

export class ProcessTestClaimApiService extends BaseCrudApiService<ProcessTestClaimEntity, ProcessTestClaimEntity, ProcessTestClaimEntity, ProcessTestClaimEntity, number, ProcessTestClaimFilter> {
    constructor() {
        super('/api/process-test-claims/');
    }

    public initialize(): ProcessTestClaimEntity {
        return ProcessTestClaimApiService.initialize();
    }

    public static initialize(): ProcessTestClaimEntity {
        return {
            accessKey: '',
            created: '',
            id: 0,
            owningUserId: '',
            processId: 0,
            processVersion: 0,
        };
    }
}
