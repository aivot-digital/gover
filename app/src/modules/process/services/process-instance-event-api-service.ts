import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {type ProcessInstanceEventEntity, ProcessNodeExecutionLogLevel} from '../entities/process-instance-event-entity';

interface ProcessInstanceEventFilter {
    processInstanceId: number;
}

export class ProcessInstanceEventApiService extends BaseCrudApiService<
    ProcessInstanceEventEntity,
    ProcessInstanceEventEntity,
    ProcessInstanceEventEntity,
    ProcessInstanceEventEntity,
    number,
    ProcessInstanceEventFilter
> {
    constructor() {
        super('/api/process-instance-events/');
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
