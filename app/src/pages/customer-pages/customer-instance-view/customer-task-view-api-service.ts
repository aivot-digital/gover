import {BaseApiService} from '../../../services/base-api-service';
import {ProcessInstanceStatus} from '../../../modules/process/enums/process-instance-status';
import {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {AuthoredElementValues} from '../../../models/element-data';
import {TaskViewEvent} from '../../../modules/process/services/process-instance-task-api-service';

export interface ProcessInstanceStatusResponse {
    title: string;
    status: ProcessInstanceStatus;
    currentTasks: string[];
}

export interface TaskViewResponse {
    layout: GroupLayout;
    data: AuthoredElementValues;
    events: Array<TaskViewEvent>;
}

export class CustomerTaskViewApiService extends BaseApiService {
    public async getInstanceStatus(instanceAccessKey: string): Promise<ProcessInstanceStatusResponse> {
        return await this.get<ProcessInstanceStatusResponse>(`/api/public/processes/${instanceAccessKey}/`);
    }

    public async getTaskView(instanceAccessKey: string, taskAccessKey: string): Promise<TaskViewResponse> {
        return await this.get<TaskViewResponse>(`/api/public/processes/${instanceAccessKey}/tasks/${taskAccessKey}/`);
    }
}