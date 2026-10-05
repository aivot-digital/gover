import {ProcessNodeEntity} from './process-node-entity';

export interface ProcessNodeExport {
    appVersion: string;
    appBuildNumber: string;
    exportTimestamp: string;
    createdByVendor: string;
    node: ProcessNodeEntity;
}
