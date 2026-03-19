import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {ProcessInstanceTaskEntity} from "../entities/process-instance-task-entity";
import {ProcessTaskStatus} from "../enums/process-task-status";
import {GroupLayout} from "../../../models/elements/form/layout/group-layout";
import {AuthoredElementValues, DerivedRuntimeElementData, isAuthoredElementValues} from '../../../models/element-data';
import {isFileUploadElementItem} from '../../../models/elements/form/input/file-upload-element';

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

export interface TaskViewEvent {
    label: string;
    event: string;
}

export class ProcessInstanceTaskApiService extends BaseCrudApiService<
    ProcessInstanceTaskEntity,
    ProcessInstanceTaskEntity,
    ProcessInstanceTaskEntity,
    ProcessInstanceTaskEntity,
    number,
    ProcessInstanceTaskFilter
> {
    constructor() {
        super('/api/process-instance-tasks/');
    }

    initialize(): ProcessInstanceTaskEntity {
        return {
            accessKey: "",
            assignedUserId: null,
            finished: null,
            id: 0,
            previousProcessNodeId: null,
            status: ProcessTaskStatus.Running,
            statusOverride: null,
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
            deadline: null,
        };
    }

    public getStaffTaskView(instanceId: number, taskId: number): Promise<TaskView> {
        return this.get(`/api/processes/${instanceId}/tasks/${taskId}/`);
    }

    public getCustomerTaskView(instanceAccessKey: string, taskAccessKey: string): Promise<TaskView> {
        return this.get(`/api/public/processes/${instanceAccessKey}/tasks/${taskAccessKey}/`);
    }

    public async putStaffTaskView(instanceId: number, taskId: number, payload: AuthoredElementValues, event: string): Promise<TaskView> {
        const formData = new FormData();
        formData.set('inputs', JSON.stringify(payload));

        async function appendFiles(data: AuthoredElementValues) {
            for (const key of Object.keys(data)) {
                const value = data[key];

                if (isFileUploadElementItem(value)) {
                    const blob = await fetch(value.uri).then((r) => r.blob());
                    formData.append("files", blob);
                }

                if (isAuthoredElementValues(value)) {
                    await appendFiles(value);
                }
            }
        }
        await appendFiles(payload);

        return this.putFormData<TaskView>(`/api/processes/${instanceId}/tasks/${taskId}/`, formData, {
            query: {
                event: event,
            },
        });
    }

    public async putCustomerTaskView(instanceAccessKey: string, taskAccessKey: string, payload: AuthoredElementValues, event: string): Promise<TaskView> {
        const formData = new FormData();
        formData.set('inputs', JSON.stringify(payload));

        async function appendFiles(data: AuthoredElementValues) {
            for (const key of Object.keys(data)) {
                const value = data[key];

                if (isFileUploadElementItem(value)) {
                    const blob = await fetch(value.uri).then((r) => r.blob());
                    formData.append("files", blob);
                }

                if (isAuthoredElementValues(value)) {
                    await appendFiles(value);
                }
            }
        }
        await appendFiles(payload);

        return this.putFormData<TaskView>(`/api/public/processes/${instanceAccessKey}/tasks/${taskAccessKey}/`, formData, {
            query: {
                event: event,
            },
        });
    }

    public rerunFailedTask(taskId: number): Promise<ProcessInstanceTaskEntity> {
        return this.put<any, ProcessInstanceTaskEntity>(this.buildPath(taskId) + 'rerun-failed/', {});
    }

    public async getAssignedTaskCount(): Promise<number> {
        const response = await this.get<{ count: number }>('/api/process-instance-tasks/assigned-count/');
        return response.count;
    }
}
