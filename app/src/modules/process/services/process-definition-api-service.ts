import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {ProcessDefinitionEntity} from "../entities/process-definition-entity";
import {ProcessExport} from "../entities/process-export";

interface ProcessDefinitionFilter {
    id: number;
    name: string;
    departmentId: number;
    departmentIdNot: number;
}

export class ProcessDefinitionApiService extends BaseCrudApiService<
    ProcessDefinitionEntity,
    ProcessDefinitionEntity,
    ProcessDefinitionEntity,
    ProcessDefinitionEntity,
    number,
    ProcessDefinitionFilter
> {
    constructor() {
        super('/api/process-definitions/');
    }

    initialize(): ProcessDefinitionEntity {
        return {
            id: 0,
            name: "",
            departmentId: 0,
            versionCount: 0,
            draftedVersion: null,
            publishedVersion: null,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        };
    }

    public export(processId: number, version: number | 'latest'): Promise<ProcessExport> {
        return this.get<ProcessExport>(`/api/process-definitions/${processId}/export/${version}/`)
    }

    public import(processData: ProcessExport): Promise<ProcessDefinitionEntity> {
        return this.post<ProcessExport, ProcessDefinitionEntity>(`/api/process-definitions/import/`, processData);
    }
}