import {ProcessStatus} from '../enums/process-status';

export interface ProcessVersionEntity {
    processId: number;
    processVersion: number;
    status: ProcessStatus;
    publicTitle: string;
    crated: string;
    updated: string;
    published: string | null;
    revoked: string | null;
}