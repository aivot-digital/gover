import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {type ProcessInstanceEntity} from '../entities/process-instance-entity';
import {ProcessInstanceStatus} from '../enums/process-instance-status';

interface ProcessInstanceFilter {
    id: number;
    accessKey: string;
    processId: number;
    processVersion: number;
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
            assignedUserId: null,
            createdForTestClaimId: null,
            keepUntil: null,
            accessKey: '',
            assignedFileNumbers: [],
            identities: [],
            finished: null,
            initialNodeId: 0,
            initialPayload: {},
            processId: 0,
            initialProcessVersion: 0,
            runtime: null,
            started: new Date().toISOString(),
            status: ProcessInstanceStatus.Created,
            statusOverride: null,
            updated: new Date().toISOString(),
            id: 0,
        };
    }
}
