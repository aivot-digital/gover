import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {ProcessVersionEntity} from '../entities/process-version-entity';
import {ProcessStatus} from '../enums/process-status';
import {ProcessNodeProblems} from '../entities/process-node-problems';

interface ProcessDefinitionVersionFilter {
    processId: number;
    processVersion: number;
    status: string;
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
            publicTitle: '',
            caseNumberTemplate: null,
            notes: null,
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

    public publish(id: ProcessDefinitionVersionEntityId): Promise<ProcessVersionEntity> {
        return this.put<any, ProcessVersionEntity>(this.buildPath(id) + 'publish/', {});
    }

    public revoke(id: ProcessDefinitionVersionEntityId): Promise<ProcessVersionEntity> {
        return this.put<any, ProcessVersionEntity>(this.buildPath(id) + 'revoke/', {});
    }
}
