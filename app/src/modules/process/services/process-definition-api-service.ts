import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {ProcessEntity} from "../entities/process-entity";
import {ProcessExport} from "../entities/process-export";

interface ProcessDefinitionFilter {
    id: number;
    name: string;
    departmentId: number;
    departmentIdNot: number;
}

export class ProcessDefinitionApiService extends BaseCrudApiService<
    ProcessEntity,
    ProcessEntity,
    ProcessEntity,
    ProcessEntity,
    number,
    ProcessDefinitionFilter
> {
    constructor() {
        super('/api/processes/');
    }

    initialize(): ProcessEntity {
        return ProcessDefinitionApiService.initialize();
    }

    public static initialize(): ProcessEntity {
        return {
            id: 0,
            internalTitle: "",
            departmentId: 0,
            accessKey: "",
            versionCount: 0,
            draftedVersion: null,
            publishedVersion: null,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        };
    }

    public export(processId: number, version: number | 'latest'): Promise<ProcessExport> {
        return this.get<ProcessExport>(`/api/processes/${processId}/export/${version}/`)
    }

    public import(processData: ProcessExport): Promise<ProcessEntity> {
        return this.post<ProcessExport, ProcessEntity>(`/api/processes/import/`, processData);
    }
}