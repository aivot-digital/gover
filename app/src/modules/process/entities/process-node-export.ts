import {ProcessEntity} from './process-entity';
import {ProcessNodeEntity} from './process-node-entity';
import {ProcessVersionEntity} from './process-version-entity';

export interface ProcessNodeExport {
    data: ProcessNodeExportData;
    signature?: string | null;
}

export interface ProcessNodeExportData {
    appVersion: string;
    appBuildNumber: string;
    exportTimestamp: string;
    createdByVendor: string;
    process: ProcessEntity;
    version: ProcessVersionEntity;
    node: ProcessNodeEntity;
}
