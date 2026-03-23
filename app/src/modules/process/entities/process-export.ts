import {ProcessEntity} from "./process-entity";
import {ProcessVersionEntity} from "./process-version-entity";
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
    process: ProcessEntity;
    version: ProcessVersionEntity;
    nodes: ProcessNodeEntity[];
    edges: ProcessDefinitionEdgeEntity[];
}