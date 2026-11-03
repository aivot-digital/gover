import {CodeListSourceType} from '../enums/code-list-source-type';
import {CodeListStatus} from '../enums/code-list-status';

// Maps a stable column key to a display name and the 1-based position of the column in the item cells. Everything
// that references a column uses the key; a missing index means the column no longer exists in the source.
export interface CodeListColumnMapping {
    key: string;
    name: string;
    index: number | null;
    // Set by the synchronization for a missing column, so the mapping returns with that column even after a key rename.
    formerSourceColumn?: string | null;
}

export interface CodeList {
    key: string;
    id: number;
    sourceType: CodeListSourceType;
    sourceRef: string;
    name: string;
    description: string;
    // The source column identifiers in the order of the item cells. They are managed by the server.
    columns: string[];
    columnMappings: CodeListColumnMapping[];
    keyColumnKey: string | null;
    status: CodeListStatus;
    statusMessage?: string | null;
    lastSync?: string | null;
    created: string;
    updated: string;
}

export interface CodeListSourceColumns {
    columns: string[];
    columnMappings: CodeListColumnMapping[];
    keyColumnKey: string | null;
}

/** Returns the mappings that point to a stored column, ordered by their position. */
export function getAvailableColumnMappings(codeList: Pick<CodeList, 'columnMappings'>): CodeListColumnMapping[] {
    return codeList.columnMappings
        .filter((mapping) => mapping.index != null)
        .sort((a, b) => (a.index ?? 0) - (b.index ?? 0));
}
