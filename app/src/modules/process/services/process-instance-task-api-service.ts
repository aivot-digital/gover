import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {ProcessInstanceTaskEntity} from "../entities/process-instance-task-entity";
import {ProcessTaskStatus} from "../enums/process-task-status";
import {GroupLayout} from "../../../models/elements/form/layout/group-layout";
import {ElementData} from "../../../models/element-data";

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
    data: ElementData;
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
            status: ProcessTaskStatus.Running,
            statusOverride: null,
            runtimeData: {},
            nodeData: {},
            processDefinitionId: 0,
            processDefinitionNodeId: 0,
            processDefinitionVersion: 0,
            processInstanceId: 0,
            runtime: null,
            started: new Date().toISOString(),
            updated: new Date().toISOString(),
            processData: {},
        };
    }

    public getTaskView(instanceAccessKey: string, taskAccessKey: string): Promise<TaskView> {
        return this.get(`/api/public/processes/${instanceAccessKey}/task/${taskAccessKey}/`);
    }

    public putTaskView(instanceAccessKey: string, taskAccessKey: string, payload: ElementData, event: string): Promise<TaskView> {
        return this.put<ElementData, TaskView>(`/api/public/processes/${instanceAccessKey}/task/${taskAccessKey}/`, payload, {
            query: {
                event: event,
            },
        });
    }

    public rerunFailedTask(taskId: number): Promise<ProcessInstanceTaskEntity> {
        return this.put<any, ProcessInstanceTaskEntity>(this.buildPath(taskId) + 'rerun-failed/', {});
    }
}