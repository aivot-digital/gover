import {type Page} from '../../../models/dtos/page';
import {type ProcessInstanceTaskEntity} from '../entities/process-instance-task-entity';

export interface ProcessInstanceHistoryTasks {
    tasks: Page<ProcessInstanceTaskEntity>;
    predecessors: ProcessInstanceTaskEntity[];
    activeTasks: Pick<ProcessInstanceTaskEntity, 'processNodeId' | 'processVersion' | 'status'>[];
}
