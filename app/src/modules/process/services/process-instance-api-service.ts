import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {ProcessInstanceEntity} from "../entities/process-instance-entity";
import {ProcessInstanceStatus} from "../enums/process-instance-status";

interface ProcessInstanceFilter {
    id: number;
    accessKey: string;
    processDefinitionId: number;
    processDefinitionVersion: number;
    status: ProcessInstanceStatus;
    statusIsNot: ProcessInstanceStatus;
    statusOverride: string;
    assignedFileNumber: string;
    tag: string;
}

export class ProcessInstanceApiService extends BaseCrudApiService<
    ProcessInstanceEntity,
    ProcessInstanceEntity,
    ProcessInstanceEntity,
    ProcessInstanceEntity,
    number,
    ProcessInstanceFilter
> {
    constructor() {
        super('/api/process-instances/');
    }

    initialize(): ProcessInstanceEntity {
        return {
            accessKey: "",
            assignedFileNumbers: [],
            deliveryChannels: [],
            finished: null,
            initialNodeId: 0,
            initialPayload: {},
            processId: 0,
            processVersion: 0,
            runtime: null,
            started: new Date().toISOString(),
            status: ProcessInstanceStatus.Created,
            statusOverride: null,
            tags: [],
            updated: new Date().toISOString(),
            id: 0
        };
    }
}