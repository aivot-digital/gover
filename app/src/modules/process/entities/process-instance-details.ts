import {type ProcessInstanceEntity} from './process-instance-entity';
import {type ProcessTaskStatus} from '../enums/process-task-status';

export interface ProcessInstanceDetails {
    instance: ProcessInstanceEntity;
    processName: string;
    departmentId: number;
    departmentName: string | null;
    triggerName: string;
    triggerType: string | null;
    activeTasks: {
        id: number;
        name: string;
        status: ProcessTaskStatus;
        statusOverride?: string | null;
        assignedUserId: string | null;
        deadline?: string | null;
    }[];
}
