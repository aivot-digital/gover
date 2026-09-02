import {BaseApiService} from '../../../services/base-api-service';
import {ProcessInstanceStatus} from '../../../modules/process/enums/process-instance-status';
import {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {AuthoredElementValues, DerivedRuntimeElementData} from '../../../models/element-data';
import {TaskViewEvent} from '../../../modules/process/services/process-instance-task-api-service';
import {ProcessTaskStatus} from '../../../modules/process/enums/process-task-status';

export interface ProcessInstanceStatusResponse {
    title: string;
    status: ProcessInstanceStatus;
    statusOverride: string;
    tasks: ProcessInstanceTaskStatusResponse[];
}

export interface ProcessInstanceTaskStatusResponse {
    accessKey: string;
    status: ProcessTaskStatus;
    statusOverride: string;
}

export interface TaskViewResponse {
    layout: GroupLayout;
    data: AuthoredElementValues;
    events: Array<TaskViewEvent>;
}

export class CustomerTaskViewApiService extends BaseApiService {
    public async getInstanceStatus(instanceAccessKey: string): Promise<ProcessInstanceStatusResponse> {
        return await this.get<ProcessInstanceStatusResponse>(`/api/public/processes/${instanceAccessKey}/`, {
            skipAuthCheck: true,
        });
    }

    public async getTaskView(instanceAccessKey: string, taskAccessKey: string): Promise<TaskViewResponse> {
        return await this.get<TaskViewResponse>(`/api/public/processes/${instanceAccessKey}/tasks/${taskAccessKey}/`, {
            skipAuthCheck: true,
        });
    }

    public async deriveTaskView(instanceAccessKey: string, taskAccessKey: string, values: AuthoredElementValues, skipErrorsForElements: string[] = []): Promise<DerivedRuntimeElementData> {
        return await this.post(`/api/public/processes/${instanceAccessKey}/tasks/${taskAccessKey}/derive/`, values, {
            query: {
                // 'test-claim': testClaimKey, TODO: Add if necessary
                skipErrorsFor: skipErrorsForElements,
                skipVisibilitiesFor: [],
                skipValuesFor: [],
                skipOverridesFor: [],
            },
        });
    }
}