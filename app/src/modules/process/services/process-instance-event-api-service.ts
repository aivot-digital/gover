import {BaseReadApiService} from '../../../services/base-read-api-service';
import {type ProcessInstanceEventEntity, ProcessNodeExecutionLogLevel} from '../entities/process-instance-event-entity';
import {
    type ProcessInstanceEventLog,
    type ProcessInstanceEventLogFilter,
    type ProcessInstanceEventLogSortOrder,
} from '../models/process-instance-event-log';

interface ProcessInstanceEventFilter {
    triggeringUserId?: string;
    processInstanceId?: number;
    processInstanceTaskId?: number;
    level?: ProcessNodeExecutionLogLevel;
    isTechnical?: boolean;
    isNotTechnical?: boolean;
    isAudit?: boolean;
    isNotAudit?: boolean;
    title?: string;
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

    getEventLog(options: {
        processInstanceId: number;
        processInstanceTaskId?: number;
        page?: number;
        size?: number;
        search?: string;
        filter?: ProcessInstanceEventLogFilter;
        sortOrder?: ProcessInstanceEventLogSortOrder;
        abort?: AbortSignal;
    }): Promise<ProcessInstanceEventLog> {
        return this.get<ProcessInstanceEventLog>(`${this.path}log/`, {
            abort: options.abort,
            query: {
                processInstanceId: options.processInstanceId,
                processInstanceTaskId: options.processInstanceTaskId,
                page: options.page ?? 0,
                size: options.size ?? 50,
                search: options.search?.trim() || undefined,
                notableOnly: options.filter === 'notable' ? true : undefined,
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
            level: ProcessNodeExecutionLogLevel.Debug,
            message: '',
            processInstanceId: 0,
            timestamp: '',
            title: '',
        };
    }
}
