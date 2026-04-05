import {BaseApiService} from '../../services/base-api-service';
import {Page} from '../../models/dtos/page';
import {AuditLogEntity} from './models/audit-log-entity';

export interface AuditLogFilter {
    id: number;
    actorType: string;
    actorId: string;
    actors: string[];
    triggerType: string;
    triggerTypes: string[];
    entityRef: string;
    entityRefType: string;
    module: string;
    modules: string[];
    ipAddress: string;
    timestampFrom: string;
    timestampTo: string;
}

export interface AuditLogFilterActorOption {
    value: string;
    label: string;
}

export interface AuditLogFilterOptions {
    modules: string[];
    triggerTypes: string[];
    actors: AuditLogFilterActorOption[];
}

export class AuditLogsApiService extends BaseApiService {
    private readonly path = '/api/audit-logs/';

    public async list(
        page: number,
        limit: number,
        sort?: string,
        order?: 'ASC' | 'DESC',
        filters?: Partial<AuditLogFilter>,
    ): Promise<Page<AuditLogEntity>> {
        return await this.get<Page<AuditLogEntity>>(this.path, {
            query: {
                page: page,
                size: limit,
                sort: sort != null && order != null ? `${sort},${order}` : undefined,
                ...filters,
            },
        });
    }

    public async retrieve(id: number): Promise<AuditLogEntity> {
        return await this.get<AuditLogEntity>(`${this.path}${id}/`);
    }

    public async getFilterOptions(): Promise<AuditLogFilterOptions> {
        return await this.get<AuditLogFilterOptions>(`${this.path}filter-options/`);
    }
}
