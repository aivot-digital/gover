import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {type ProcessAccessControlEntity} from '../entities/process-access-control-entity';

interface ProcessAccessControlFilter {
    sourceTeamId: number;
    sourceDepartmentId: number;
    targetProcessId: number;
}

export class ProcessAccessControlApiService extends BaseCrudApiService<ProcessAccessControlEntity, ProcessAccessControlEntity, ProcessAccessControlEntity, ProcessAccessControlEntity, number, ProcessAccessControlFilter> {
    constructor() {
        super('/api/process-access-controls/');
    }

    public initialize(): ProcessAccessControlEntity {
        return ProcessAccessControlApiService.initialize();
    }

    public static initialize(): ProcessAccessControlEntity {
        return {
            id: 0,
            sourceTeamId: null,
            sourceDepartmentId: null,
            targetProcessId: 0,
            permissions: [],
            created: '',
            updated: '',
        };
    }
}
