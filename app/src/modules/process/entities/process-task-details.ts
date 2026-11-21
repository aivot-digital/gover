import {type ProcessInstanceTaskEntity} from './process-instance-task-entity';
import {type ProcessInstanceEntity} from './process-instance-entity';
import {type ProcessEntity} from './process-entity';
import {type ProcessNodeEntity} from './process-node-entity';
import {type ProcessNodeProvider} from '../services/process-node-provider-api-service';
import {type ProcessDeputy} from './process-deputy';

export interface ProcessTaskDetails {
    task: ProcessInstanceTaskEntity;
    assignedUserDeputies: ProcessDeputy[];
    instance: ProcessInstanceEntity | null;
    process: Pick<ProcessEntity, 'id' | 'internalTitle'> | null;
    node: Pick<ProcessNodeEntity, 'name' | 'description'> | null;
    provider: Pick<ProcessNodeProvider, 'key' | 'componentKey' | 'name' | 'abstractDescription'> | null;
}
