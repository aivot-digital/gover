import {CodeListSourceType} from '../enums/code-list-source-type';
import {CodeListStatus} from '../enums/code-list-status';

// The index addresses a stored cell. An unassigned column has no index; when a source column disappears,
// formerSourceColumn also records its identifier for a later synchronization.
export interface CodeListColumnMapping {
    key: string;
    name: string;
    index: number | null;
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
    lockVersion?: number;
}

export interface CodeListSourceColumns {
    columns: string[];
    columnMappings: CodeListColumnMapping[];
    keyColumnKey: string | null;
}

/** Lists the usable columns in the order configured in the column table. */
export function getAvailableColumnMappings(codeList: Pick<CodeList, 'columnMappings'>): CodeListColumnMapping[] {
    return codeList.columnMappings.filter((mapping) => mapping.index != null);
}

export function getDuplicateCodeListColumnError(
    mappings: ReadonlyArray<Pick<CodeListColumnMapping, 'index'>>,
    columns: readonly string[],
): string | undefined {
    const used = new Set<number>();
    for (const {index} of mappings) {
        if (index == null) continue;
        if (used.has(index)) {
            const column = columns[index - 1] ? `${index} – ${columns[index - 1]}` : String(index);
            return `Die Spalte „${column}“ ist mehrfach zugeordnet. Wählen Sie für jede Zuordnung eine eigene Spalte.`;
        }
        used.add(index);
    }
}
