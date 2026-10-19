import {type ProcessAssignmentOption} from '../entities/process-assignment-option';
import {BaseReadApiService} from '../../../services/base-read-api-service';
import {ProcessInstanceTaskEntity} from '../entities/process-instance-task-entity';
import {ProcessTaskStatus} from '../enums/process-task-status';
import {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {AuthoredElementValues, DerivedRuntimeElementData} from '../../../models/element-data';
import {FileUploadElementItem} from '../../../models/elements/form/input/file-upload-element';
import type {CustomerTaskViewResponse} from '../models/customer-task-view';
import {type ProcessTaskDetails} from '../entities/process-task-details';

interface ProcessInstanceTaskFilter {
    id: number;
    accessKey: string;
    processInstanceId: number;
    processDefinitionId: number;
    processDefinitionVersion: number;
    processDefinitionNodeId: number;
    assignedUserId: string;
    status: ProcessTaskStatus;
}

export interface TaskView {
    layout: GroupLayout;
    data: AuthoredElementValues;
    events: TaskViewEvent[];
}

export type TaskViewEventVariant = 'contained' | 'outlined' | 'text';

export type TaskViewEventColor = 'primary' | 'secondary' | 'error' | 'success';

export type TaskViewEventAlignment = 'left' | 'right';

export interface TaskViewEvent {
    label: string;
    event: string;
    variant?: TaskViewEventVariant | null;
    color?: TaskViewEventColor | null;
    alignment?: TaskViewEventAlignment | null;
}

async function appendTaskViewFiles(formData: FormData, files: FileUploadElementItem[]): Promise<void> {
    for (const file of files) {
        const blob = await fetch(file.uri).then((response) => response.blob());
        formData.append('files', blob, file.name);
        formData.append('fileUris', file.uri);
    }
}

export class ProcessInstanceTaskApiService extends BaseReadApiService<
    ProcessInstanceTaskEntity,
    ProcessInstanceTaskEntity,
    number,
    ProcessInstanceTaskFilter
> {
    constructor() {
        super('/api/process-instance-tasks/');
    }

    public assignmentOptions(id: number): Promise<ProcessAssignmentOption[]> {
        return this.get(`${this.buildPath(id)}assignment-options/`);
    }

    initialize(): ProcessInstanceTaskEntity {
        return {
            accessKey: '',
            assignedUserId: null,
            assignedCustomerIdentityId: null,
            finished: null,
            id: 0,
            previousProcessInstanceTaskId: null,
            restartForTaskId: null,
            previousProcessNodeId: null,
            previousProcessNodePortKey: null,
            status: ProcessTaskStatus.Running,
            statusOverride: null,
            executionSummaryMarkdown: null,
            runtimeData: {},
            nodeData: {},
            processId: 0,
            processNodeId: 0,
            processVersion: 0,
            processInstanceId: 0,
            runtime: null,
            started: new Date().toISOString(),
            updated: new Date().toISOString(),
            processData: {},
            processDataDiff: {},
            deadline: null,
            deadlineKey: null,
            deadlineStrict: false,
        };
    }

    public getStaffTaskView(instanceId: number, taskId: number): Promise<TaskView> {
        return this.get(`/api/processes/${instanceId}/tasks/${taskId}/`);
    }

    public retrieveDetails(taskId: number): Promise<ProcessTaskDetails> {
        return this.get(`/api/process-instance-tasks/${taskId}/details/`);
    }

    public deriveStaffTaskView(
        instanceId: number,
        taskId: number,
        aev: AuthoredElementValues,
        skipErrorsForElements: string[] = ['ALL'],
    ): Promise<DerivedRuntimeElementData> {
        return this.post<AuthoredElementValues, DerivedRuntimeElementData>(
            `/api/processes/${instanceId}/tasks/${taskId}/derive/`,
            aev,
            {
                query: {
                    skipErrorsFor: skipErrorsForElements,
                },
            },
        );
    }

    public getCustomerTaskView(instanceAccessKey: string, taskAccessKey: string): Promise<CustomerTaskViewResponse> {
        return this.get(`/api/public/processes/${instanceAccessKey}/tasks/${taskAccessKey}/`, {
            skipAuthCheck: true,
            doNotHandleStatusCodes: true,
        });
    }

    /**
     * @param files the transient file items of the payload whose files to upload, usually from collectTaskViewFileUploads
     */
    public async putStaffTaskView(
        instanceId: number,
        taskId: number,
        payload: AuthoredElementValues,
        files: FileUploadElementItem[],
        event?: string,
    ): Promise<TaskView> {
        const formData = new FormData();
        formData.set('inputs', JSON.stringify(payload));
        await appendTaskViewFiles(formData, files);

        return this.putFormData<TaskView>(`/api/processes/${instanceId}/tasks/${taskId}/`, formData, {
            query: {
                event: event,
            },
        });
    }

    /**
     * @param files the transient file items of the payload whose files to upload, usually from collectTaskViewFileUploads
     */
    public async putCustomerTaskView(
        instanceAccessKey: string,
        taskAccessKey: string,
        payload: AuthoredElementValues,
        files: FileUploadElementItem[],
        event: string,
    ): Promise<CustomerTaskViewResponse> {
        const formData = new FormData();
        formData.set('inputs', JSON.stringify(payload));
        await appendTaskViewFiles(formData, files);

        return this.putFormData<CustomerTaskViewResponse>(
            `/api/public/processes/${instanceAccessKey}/tasks/${taskAccessKey}/`,
            formData,
            {
                query: {
                    event: event,
                },
                skipAuthCheck: true,
                doNotHandleStatusCodes: true,
            },
        );
    }

    public reassign(id: number, assignedUserId: string | null): Promise<ProcessInstanceTaskEntity> {
        return this.put<{assignedUserId: string | null}, ProcessInstanceTaskEntity>(`${this.buildPath(id)}reassign/`, {
            assignedUserId,
        });
    }

    public rerunFailedTask(taskId: number): Promise<ProcessInstanceTaskEntity> {
        return this.put<any, ProcessInstanceTaskEntity>(this.buildPath(taskId) + 'rerun-failed/', {});
    }

    public async getAssignedTaskCount(): Promise<number> {
        const response = await this.get<{count: number}>('/api/process-instance-tasks/assigned-count/');
        return response.count;
    }
}
