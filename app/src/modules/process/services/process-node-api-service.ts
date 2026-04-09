import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {type ProcessNodeEntity} from '../entities/process-node-entity';
import {type ProcessNodeExport} from '../entities/process-node-export';
import {type GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {generateId} from '../../../utils/id-utils';
import {ProcessNodeProblems} from '../entities/process-node-problems';

interface ProcessDefinitionNodeFilter {
    id: number;
    processId: number;
    processVersion: number;
    dataKey: string;
    processNodeDefinitionKey: string;
    processNodeDefinitionVersion: number;
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
            savedWithErrors: false,
        };
    }

    public getConfigurationLayout(id: number): Promise<GroupLayout> {
        return this.get(`${this.path}${id}/configuration/`);
    }

    public getTesting(id: number): Promise<GroupLayout> {
        return this.get(`${this.path}${id}/testing/`);
    }

    public export(id: number): Promise<ProcessNodeExport> {
        return this.get(`${this.path}${id}/export/`);
    }

    public import(processId: number, processVersion: number, nodeData: ProcessNodeExport): Promise<ProcessNodeEntity> {
        return this.post(`/api/process-nodes/import/${processId}/${processVersion}/`, nodeData);
    }

    public async validate(id: number): Promise<ProcessNodeProblems | null> {
        try {
            return await this.get<ProcessNodeProblems>(`${this.path}${id}/problems/`);
        } catch (err) {
            return null;
        }
    }
}
