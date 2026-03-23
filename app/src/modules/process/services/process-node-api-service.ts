import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {type ProcessNodeEntity} from '../entities/process-node-entity';
import {type GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {generateId} from '../../../utils/id-utils';

interface ProcessDefinitionNodeFilter {
    id: number;
    processDefinitionId: number;
    processDefinitionVersion: number;
    dataKey: string;
    codeKey: string;
}

export class ProcessNodeApiService extends BaseCrudApiService<
ProcessNodeEntity,
ProcessNodeEntity,
ProcessNodeEntity,
ProcessNodeEntity,
number,
ProcessDefinitionNodeFilter
> {
    constructor() {
        super('/api/process-nodes/');
    }

    public initialize(): ProcessNodeEntity {
        return ProcessNodeApiService.initialize();
    }

    public static initialize(): ProcessNodeEntity {
        return {
            notes: null,
            outputMappings: {},
            requirements: null,
            timeLimitDays: null,
            id: 0,
            processId: 0,
            processVersion: 0,
            processNodeDefinitionKey: '',
            processNodeDefinitionVersion: 0,
            name: null,
            description: null,
            dataKey: generateId(5),
            configuration: {},
        };
    }

    public getConfigurationLayout(id: number): Promise<GroupLayout> {
        return this.get(`${this.path}${id}/configuration/`);
    }

    public getTesting(id: number): Promise<GroupLayout> {
        return this.get(`${this.path}${id}/testing/`);
    }
}
