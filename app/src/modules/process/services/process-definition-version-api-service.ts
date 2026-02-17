import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {ProcessVersionEntity} from "../entities/process-version-entity";
import {ProcessStatus} from "../enums/process-status";
import {RetentionTimeUnit} from "../enums/retention-time-unit";

interface ProcessDefinitionVersionFilter {
    processDefinitionId: number;
    processDefinitionVersion: number;
    status: string;
    retentionTimeUnit: string;
    retentionTimeAmount: number;
}

interface ProcessDefinitionVersionEntityId {
    processDefinitionId: number;
    processDefinitionVersion: number;
}

export class ProcessDefinitionVersionApiService extends BaseCrudApiService<
    ProcessVersionEntity,
    ProcessVersionEntity,
    ProcessVersionEntity,
    ProcessVersionEntity,
    ProcessDefinitionVersionEntityId,
    ProcessDefinitionVersionFilter
> {
    constructor() {
        super('/api/process-versions/');
    }

    buildPath(id: ProcessDefinitionVersionEntityId): string {
        return `${this.path}${id.processDefinitionId}/${id.processDefinitionVersion}/`;
    }

    initialize(): ProcessVersionEntity {
        return ProcessDefinitionVersionApiService.initialize();
    }

    public static initialize(): ProcessVersionEntity {
        return {
            processId: 0,
            processVersion: 0,
            publicTitle: 'Neues Verfahren',
            status: ProcessStatus.Drafted,
            crated: new Date().toISOString(),
            updated: new Date().toISOString(),
            published: null,
            revoked: null,
        };
    }
}