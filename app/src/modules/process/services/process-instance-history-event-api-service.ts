import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {
    ProcessInstanceHistoryEventEntity
} from "../entities/process-instance-history-event-entity";
import {ProcessHistoryEventType} from "../enums/process-history-event-type";

interface ProcessInstanceHistoryEventFilter {
    id: number;
    triggeringUserId: string;
    processInstanceId: number;
    processInstanceTaskId: number;
}

export class ProcessInstanceHistoryEventApiService extends BaseCrudApiService<
    ProcessInstanceHistoryEventEntity,
    ProcessInstanceHistoryEventEntity,
    ProcessInstanceHistoryEventEntity,
    ProcessInstanceHistoryEventEntity,
    number,
    ProcessInstanceHistoryEventFilter
> {
    constructor() {
        super('/api/process-instance-history-events/');
    }

    initialize(): ProcessInstanceHistoryEventEntity {
        return {
            details: {},
            id: 0,
            message: "",
            processInstanceId: 0,
            processInstanceTaskId: null,
            timestamp: new Date().toISOString(),
            title: "",
            type: ProcessHistoryEventType.Update,
            triggeringUserId: null,
        };
    }
}