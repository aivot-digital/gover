import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {ProcessDefinitionNodeEntity} from "../entities/process-definition-node-entity";
import {GroupLayout} from "../../../models/elements/form/layout/group-layout";

interface ProcessDefinitionNodeFilter {
    id: number;
    processDefinitionId: number;
    processDefinitionVersion: number;
    dataKey: string;
    codeKey: string;
}

export class ProcessDefinitionNodeApiService extends BaseCrudApiService<
    ProcessDefinitionNodeEntity,
    ProcessDefinitionNodeEntity,
    ProcessDefinitionNodeEntity,
    ProcessDefinitionNodeEntity,
    number,
    ProcessDefinitionNodeFilter
> {
    constructor() {
        super('/api/process-definition-nodes/');
    }

    initialize(): ProcessDefinitionNodeEntity {
        return {
            id: 0,
            processDefinitionId: 0,
            processDefinitionVersion: 0,
            name: null,
            description: null,
            dataKey: "",
            codeKey: "",
            configuration: {},
        };
    }

    public getConfigurationLayout(id: number): Promise<GroupLayout> {
        return this.get(`${this.path}${id}/configuration/`);
    }
}