import {ProcessHistoryEventScope} from '../enums/process-history-event-scope';
import {type Page} from '../../../models/dtos/page';
import {BaseReadApiService} from '../../../services/base-read-api-service';
import {type ProcessInstanceEventEntity, type ProcessInstanceHistoryEvent, ProcessNodeExecutionLogLevel} from '../entities/process-instance-event-entity';
import {
    type ProcessInstanceEventLog,
    type ProcessInstanceEventLogEntry,
    type ProcessInstanceEventLogFilter,
    type ProcessInstanceEventLogSortOrder,
} from '../models/process-instance-event-log';

export interface ProcessInstanceEventFilter {
    triggeringUserId?: string;
    processInstanceId?: number;
    processInstanceTaskId?: number;
    level?: ProcessNodeExecutionLogLevel;
    isTechnical?: boolean;
    isNotTechnical?: boolean;
    isAudit?: boolean;
    isNotAudit?: boolean;
    title?: string;
    historyRelevant?: boolean;
    concernedUserId?: string;
    concernedIdentityId?: string;
    concernedIdentityTitle?: string;
}

export class ProcessInstanceEventApiService extends BaseReadApiService<
    ProcessInstanceEventEntity,
    ProcessInstanceEventEntity,
    number,
    ProcessInstanceEventFilter
> {
    constructor() {
        super('/api/process-instance-events/');
    }

    async listHistory(processInstanceId: number, options: {
        scope: ProcessHistoryEventScope;
        taskId?: number;
        page?: number;
        size?: number;
        sortOrder?: ProcessInstanceEventLogSortOrder;
    }): Promise<Page<ProcessInstanceHistoryEvent>> {
        const page = await this.get<Page<ProcessInstanceEventLogEntry>>(`${this.path}history/`, {
            query: {
                processInstanceId, scope: options.scope, processInstanceTaskId: options.taskId,
                page: options.page ?? 0, size: options.size ?? 20,
                sort: [`timestamp,${options.sortOrder ?? 'ASC'}`, `id,${options.sortOrder ?? 'ASC'}`],
            },
        });
        return {
            ...page,
            content: page.content.map(event => ({...event, isTechnical: event.technical, isAudit: event.audit})),
        };
    }

    getEventLog(options: {
        processInstanceId: number;
        processInstanceTaskId?: number;
        includeRestartHistory?: boolean;
        page?: number;
        size?: number;
        search?: string;
        filter?: ProcessInstanceEventLogFilter;
        historyRelevant?: boolean;
        concernedUserId?: string;
        concernedIdentityId?: string;
        concernedIdentityTitle?: string;
        sortOrder?: ProcessInstanceEventLogSortOrder;
        abort?: AbortSignal;
    }): Promise<ProcessInstanceEventLog> {
        return this.get<ProcessInstanceEventLog>(`${this.path}log/`, {
            abort: options.abort,
            query: {
                processInstanceId: options.processInstanceId,
                processInstanceTaskId: options.processInstanceTaskId,
                includeRestartHistory: options.includeRestartHistory,
                page: options.page ?? 0,
                size: options.size ?? 50,
                search: options.search?.trim() || undefined,
                notableOnly: options.filter === 'notable' ? true : undefined,
                historyRelevant: options.historyRelevant,
                concernedUserId: options.concernedUserId,
                concernedIdentityId: options.concernedIdentityId,
                concernedIdentityTitle: options.concernedIdentityTitle,
                sort: `timestamp,${options.sortOrder ?? 'DESC'}`,
            },
        });
    }

    initialize(): ProcessInstanceEventEntity {
        return {
            details: {},
            id: 0,
            isAudit: false,
            isTechnical: false,
            historyRelevant: false,
            level: ProcessNodeExecutionLogLevel.Debug,
            message: '',
            processInstanceId: 0,
            timestamp: '',
            title: '',
        };
    }
}
