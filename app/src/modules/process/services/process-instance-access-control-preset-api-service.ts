import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {type ProcessInstanceAccessControlPresetEntity} from '../entities/process-instance-access-control-preset-entity';

interface ProcessInstanceAccessControlPresetFilter {
    sourceTeamId: number;
    sourceDepartmentId: number;
    targetProcessId: number;
    targetProcessVersion: number;
}

export class ProcessInstanceAccessControlPresetApiService extends BaseCrudApiService<ProcessInstanceAccessControlPresetEntity, ProcessInstanceAccessControlPresetEntity, ProcessInstanceAccessControlPresetEntity, ProcessInstanceAccessControlPresetEntity, number, ProcessInstanceAccessControlPresetFilter> {
    constructor() {
        super('/api/process-instance-access-control-presets/');
    }

    public initialize(): ProcessInstanceAccessControlPresetEntity {
        return ProcessInstanceAccessControlPresetApiService.initialize();
    }

    public static initialize(): ProcessInstanceAccessControlPresetEntity {
        return {
            id: 0,
            sourceTeamId: null,
            sourceDepartmentId: null,
            targetProcessId: 0,
            targetProcessVersion: 0,
            permissions: [],
            created: '',
            updated: '',
        };
    }
}
