import {CodeListSourceType} from '../enums/code-list-source-type';
import {CodeListStatus} from '../enums/code-list-status';

export interface CodeList {
    key: string;
    id: number;
    sourceType: CodeListSourceType;
    sourceRef: string;
    name: string;
    description: string;
    columns: string[];
    valueColumnIndex: number;
    labelColumnIndex: number;
    status: CodeListStatus;
    statusMessage?: string | null;
    lastSync?: string | null;
    created: string;
    updated: string;
}
