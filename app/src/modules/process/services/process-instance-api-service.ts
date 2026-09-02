import {RequestOptions} from '../../../services/base-api-service';
import {BaseReadApiService} from '../../../services/base-read-api-service';
import {type ProcessInstanceEntity} from '../entities/process-instance-entity';
import {ProcessInstanceStatus} from '../enums/process-instance-status';

interface ProcessInstanceFilter {
    id: number;
    accessKey: string;
    processId: number;
    processVersion: number;
    createdForTestClaimId: number;
    status: ProcessInstanceStatus;
    statusIsNot: ProcessInstanceStatus;
    statusOverride: string;
    assignedFileNumber: string;
    tag: string;
}

export class ProcessInstanceApiService extends BaseReadApiService<
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
            caseNumber: '',
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

    public restartFailedInstance(id: number): Promise<ProcessInstanceEntity> {
        return this.put<any, ProcessInstanceEntity>(this.buildPath(id) + 'restart-failed/', {});
    }

    public reassign(id: number, assignedUserId: string | null): Promise<ProcessInstanceEntity> {
        return this.put<{ assignedUserId: string | null }, ProcessInstanceEntity>(
            this.buildPath(id) + 'reassign/',
            {assignedUserId},
        );
    }

    public async destroy(id: number, options?: RequestOptions): Promise<void> {
        return await this.delete(this.buildPath(id), options);
    }
}
