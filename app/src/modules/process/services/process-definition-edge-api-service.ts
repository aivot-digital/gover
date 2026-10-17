import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {ProcessDefinitionEdgeEntity} from "../entities/process-definition-edge-entity";

interface ProcessDefinitionEdgeFilter {
    id: number;
    processDefinitionId: number;
    processDefinitionVersion: number;
    fromNodeId: number;
    toNodeId: number;
    viaPort: string;
}

export class ProcessDefinitionEdgeApiService extends BaseCrudApiService<
    ProcessDefinitionEdgeEntity,
    ProcessDefinitionEdgeEntity,
    ProcessDefinitionEdgeEntity,
    ProcessDefinitionEdgeEntity,
    number,
    ProcessDefinitionEdgeFilter
> {
    constructor() {
        super('/api/process-edges/');
    }

    initialize(): ProcessDefinitionEdgeEntity {
        return {
            id: 0,
            processId: 0,
            processVersion: 0,
            fromNodeId: 0,
            toNodeId: 0,
            viaPort: "",
        };
    }
}