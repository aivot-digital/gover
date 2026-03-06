import {BaseApiService} from '../../services/base-api-service';
import {Page} from '../../models/dtos/page';
import {AuditLogEntity} from './models/audit-log-entity';

export interface AuditLogFilter {
    id: number;
    triggeringUserId: string;
    actorType: string;
    actorId: string;
    actorLabel: string;
    triggerType: string;
    triggerRef: string;
    serviceName: string;
    instanceId: string;
    actionType: string;
    component: string;
    entityType: string;
    entityId: string;
    changedData: boolean;
    actionResult: string;
    source: string;
    requestId: string;
    sessionId: string;
    severity: string;
    tag: string;
    eventTsFrom: string;
    eventTsTo: string;
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
}
