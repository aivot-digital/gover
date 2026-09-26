import {type ProcessInstanceTaskEntity} from './process-instance-task-entity';
import {type ProcessInstanceEntity} from './process-instance-entity';
import {type ProcessEntity} from './process-entity';
import {type ProcessNodeEntity} from './process-node-entity';
import {type ProcessNodeProvider} from '../services/process-node-provider-api-service';

export interface ProcessTaskDetails {
    task: ProcessInstanceTaskEntity;
    instance: ProcessInstanceEntity | null;
    process: Pick<ProcessEntity, 'id' | 'internalTitle'> | null;
    node: Pick<ProcessNodeEntity, 'name' | 'description'> | null;
    provider: Pick<ProcessNodeProvider, 'key' | 'componentKey' | 'name' | 'abstractDescription'> | null;
}
