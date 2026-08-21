import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {ProcessVersionEntity} from '../entities/process-version-entity';
import {ProcessStatus} from '../enums/process-status';
import {ProcessVersionProblems} from '../entities/process-version-problems';

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
            themeId: null,
            legalSupportDepartmentId: null,
            technicalSupportDepartmentId: null,
            imprintDepartmentId: null,
            privacyDepartmentId: null,
            accessibilityDepartmentId: null,
            processSpecificPrivacyStatement: null,
            processSpecificAccessibilityStatement: null,
            status: ProcessStatus.Drafted,
            crated: new Date().toISOString(),
            updated: new Date().toISOString(),
            published: null,
            revoked: null,
        };
    }

    public validate(id: ProcessDefinitionVersionEntityId): Promise<ProcessVersionProblems> {
        return this.get<ProcessVersionProblems>(this.buildPath(id) + 'problems/');
    }

    public publish(id: ProcessDefinitionVersionEntityId): Promise<ProcessVersionEntity> {
        return this.put<any, ProcessVersionEntity>(this.buildPath(id) + 'publish/', {});
    }

    public revoke(id: ProcessDefinitionVersionEntityId): Promise<ProcessVersionEntity> {
        return this.put<any, ProcessVersionEntity>(this.buildPath(id) + 'revoke/', {});
    }
}
