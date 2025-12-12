import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {ProcessDefinitionVersionEntity} from "../entities/process-definition-version-entity";
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
    ProcessDefinitionVersionEntity,
    ProcessDefinitionVersionEntity,
    ProcessDefinitionVersionEntity,
    ProcessDefinitionVersionEntity,
    ProcessDefinitionVersionEntityId,
    ProcessDefinitionVersionFilter
> {
    constructor() {
        super('/api/process-definition-versions/');
    }

    buildPath(id: ProcessDefinitionVersionEntityId): string {
        return `${this.path}${id.processDefinitionId}/${id.processDefinitionVersion}/`;
    }

    initialize(): ProcessDefinitionVersionEntity {
        return {
            processDefinitionId: 0,
            processDefinitionVersion: 0,
            retentionTimeAmount: 0,
            retentionTimeUnit: RetentionTimeUnit.Days,
            status: ProcessStatus.Drafted,
            crated: new Date().toISOString(),
            updated: new Date().toISOString(),
            published: null,
            revoked: null,
        };
    }
}