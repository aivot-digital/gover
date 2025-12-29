import {ProcessStatus} from "../enums/process-status";
import {RetentionTimeUnit} from "../enums/retention-time-unit";

export interface ProcessDefinitionVersionEntity {
    processId: number;
    processVersion: number;
    status: ProcessStatus;
    retentionTimeUnit: RetentionTimeUnit;
    retentionTimeAmount: number;
    crated: string;
    updated: string;
    published: string | null;
    revoked: string | null;
}