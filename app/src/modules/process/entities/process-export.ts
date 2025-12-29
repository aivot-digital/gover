import {ProcessDefinitionEntity} from "./process-definition-entity";
import {ProcessDefinitionVersionEntity} from "./process-definition-version-entity";
import {ProcessNodeEntity} from "./process-node-entity";
import {ProcessDefinitionEdgeEntity} from "./process-definition-edge-entity";

export interface ProcessExport {
    data: ProcessExportData;
    signature?: string | null;
}

export interface ProcessExportData {
    appVersion: string;
    exportTimestamp: string; // ISO date string
    createdByVendor: string;
    process: ProcessDefinitionEntity;
    version: ProcessDefinitionVersionEntity;
    nodes: ProcessNodeEntity[];
    edges: ProcessDefinitionEdgeEntity[];
}