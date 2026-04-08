import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {ProcessVersionEntity} from '../entities/process-version-entity';
import {ProcessStatus} from '../enums/process-status';
import {ProcessNodeProblems} from '../entities/process-node-problems';

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

    public validate(id: ProcessDefinitionVersionEntityId): Promise<ProcessNodeProblems[]> {
        return this.get<ProcessNodeProblems[]>(this.buildPath(id) + 'problems/');
    }
}