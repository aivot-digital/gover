import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {ProcessEntity} from "../entities/process-entity";
import {ProcessExport} from "../entities/process-export";
import {ProcessVersionEntity} from '../entities/process-version-entity';
import {ProcessSlugHistoryEntity} from '../entities/process-slug-history-entity';

interface ProcessDefinitionFilter {
    internalTitle: string;
    departmentId: number;
    departmentIdNot: number;
    accessKey: string;
    slug: string;
    isPublished: boolean;
    isDrafted: boolean;
    isRevoked: boolean;
}

interface ProcessSlugAvailabilityResponse {
    available: boolean;
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
            slug: "",
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

    public move(processId: number, targetDepartmentId: number): Promise<ProcessEntity> {
        return this.put<any, ProcessEntity>(`/api/processes/${processId}/move/?targetDepartmentId=${targetDepartmentId}`, {});
    }

    public addNewVersion(processId: number, sourceVersionNumber?: number): Promise<ProcessVersionEntity> {
        return this.post<any, ProcessVersionEntity>(`/api/processes/${processId}/new-version/${sourceVersionNumber ?? 'latest'}/`, {});
    }

    public listSlugHistory(processId: number): Promise<ProcessSlugHistoryEntity[]> {
        return this.get<ProcessSlugHistoryEntity[]>(`/api/processes/${processId}/slug-history/`);
    }

    public clearSlugHistory(processId: number): Promise<void> {
        return this.delete(`/api/processes/${processId}/slug-history/`);
    }

    public async checkSlugAvailability(slug: string, processId?: number): Promise<boolean> {
        const response = await this.get<ProcessSlugAvailabilityResponse>('/api/processes/slug-availability/', {
            query: {
                slug,
                processId,
            },
        });

        return response.available;
    }
}
