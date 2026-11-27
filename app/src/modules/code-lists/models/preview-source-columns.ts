import {type CodeList, type CodeListColumnMapping, type CodeListSourceColumns} from './code-list';

/** Reconciles a draft against fresh metadata without changing the saved cells or adding definitions. */
export function previewSourceColumns(
    list: CodeList,
    source: CodeListSourceColumns,
): Pick<CodeList, 'columns' | 'columnMappings' | 'keyColumnKey'> {
    const positions = new Map(source.columns.map((column, i) => [column, i + 1]));
    const occupied = new Set<number>();
    const columnMappings = list.columnMappings.map((mapping): CodeListColumnMapping => {
        if (mapping.index == null) return mapping;
        const sourceId = list.columns[mapping.index - 1];
        const index = positions.get(sourceId);
        if (index == null) return {...mapping, index: null, formerSourceColumn: sourceId};
        occupied.add(index);
        return mapping.index === index && mapping.formerSourceColumn == null
            ? mapping
            : {...mapping, index, formerSourceColumn: null};
    });
    // Explicit bindings have priority over a missing column that has just returned.
    for (const [i, mapping] of columnMappings.entries()) {
        if (mapping.index != null || mapping.formerSourceColumn == null) continue;
        const index = positions.get(mapping.formerSourceColumn);
        if (index != null && !occupied.has(index)) {
            occupied.add(index);
            columnMappings[i] = {...mapping, index, formerSourceColumn: null};
        }
    }
    return {columns: source.columns, columnMappings, keyColumnKey: list.keyColumnKey};
}
